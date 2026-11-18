import { expect, test, type APIRequestContext, type Page } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm, OWNER_NAME } from "./support/account";

test.describe.configure({ timeout: 60_000 });

/** Um bar com a mesa 04, o Chopp, o Guioza e a Pizza com tamanhos. */
async function bar(request: APIRequestContext) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) =>
    (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  const sector = await post("/api/v1/sectors", { name: "Salão" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  const category = await post("/api/v1/menu-categories", { name: "Bar" });
  const product = (name: string, priceCents: number) =>
    post("/api/v1/products", { categoryId: category.id, name, priceCents, featured: false, available: true });
  const chopp = await product("Chopp", 1200);
  await product("Guioza", 2990);
  const pizza = await product("Pizza", 0);
  const saved = await request.put(`${API_URL}/api/v1/products/${pizza.id}/options`, {
    headers,
    data: {
      variants: [
        { name: "Broto", priceCents: 3000, available: true, rodizioDefault: false },
        { name: "Grande", priceCents: 5000, available: true, rodizioDefault: false },
      ],
      groups: [],
      prices: [],
    },
  });
  expect(saved.status()).toBe(200);
  return { email, headers, qr: table.publicToken as string, tableId: table.id as string, chopp };
}

/** O dono liga a opção pela tela de presença e pedidos. */
async function allowStaffOrders(page: Page) {
  await page.goto("/admin/presenca");
  await page.getByLabel("Garçom pode lançar pedidos").check();
  await page.getByRole("button", { name: /^Salvar/ }).click();
  await expect(page.getByRole("status").filter({ hasText: /salv/i })).toBeVisible();
}

async function openTableOrder(page: Page) {
  await page.goto("/painel/mesas");
  await page.getByRole("button", { name: /Mesa 04/ }).click();
  await page.getByRole("link", { name: "Fazer pedido" }).click();
  await expect(page.getByRole("heading", { name: "Pedido da Mesa 04" })).toBeVisible();
}

async function send(page: Page) {
  await page.getByRole("button", { name: /^Revisar pedido/ }).click();
  await page.getByRole("button", { name: /^Enviar para a cozinha/ }).click();
}

test("sem a opção ligada, a mesa não mostra Fazer pedido", async ({ page, request }) => {
  const { email, tableId } = await bar(request);
  await loginThroughForm(page, email);
  await page.goto("/painel/mesas");
  await page.getByRole("button", { name: /Mesa 04/ }).click();
  await expect(page.getByRole("complementary", { name: /04/ })).toBeVisible();
  await expect(page.getByRole("link", { name: "Fazer pedido" })).toHaveCount(0);

  await page.goto(`/painel/mesas/${tableId}/pedido`);
  await expect(page.getByRole("alert").filter({ hasText: "está desligado" })).toBeVisible();
});

test("o garçom lança o pedido com tamanho e observação, e a mesa vê", async ({ page, request, browser }) => {
  const { email, headers, qr } = await bar(request);
  await loginThroughForm(page, email);
  await allowStaffOrders(page);
  await openTableOrder(page);

  await page.getByRole("searchbox", { name: "Buscar no cardápio" }).fill("chopp");
  await page.getByRole("button", { name: "Adicionar Chopp" }).click();
  await page.getByRole("button", { name: "Mais Chopp" }).click();
  await page.getByRole("searchbox", { name: "Buscar no cardápio" }).fill("");

  await page.getByRole("button", { name: "Adicionar Pizza" }).click();
  const pizza = page.getByRole("dialog", { name: "Pizza" });
  await expect(pizza.getByRole("button", { name: "Falta: o tamanho" })).toBeDisabled();
  await pizza.getByRole("button", { name: /Grande/ }).click();
  await pizza.getByRole("button", { name: /^Adicionar · R\$ 50,00/ }).click();

  await page.getByRole("button", { name: /^Revisar pedido · 3 itens · R\$ 74,00/ }).click();
  await page.getByRole("textbox", { name: "Observação de Chopp" }).fill("sem colarinho");
  await page.getByRole("button", { name: /^Enviar para a cozinha/ }).click();
  await expect(page.getByRole("status").filter({ hasText: "enviado para a cozinha" })).toBeVisible();
  await expect(page.getByRole("region", { name: "Já pedido nesta mesa" })).toContainText("2× Chopp");
  await expect(page.getByText("2 a caminho")).toBeVisible();

  const open = await (await request.get(`${API_URL}/api/v1/tabs/open`, { headers })).json();
  expect(open.content[0].totalCents).toBe(7400);
  await page.goto(`/painel/comandas/${open.content[0].id}`);
  await expect(page.getByText(`Lançado por ${OWNER_NAME}`)).toBeVisible();

  // O cliente que escaneia depois vê o pedido, marcado como do garçom, e não cancela.
  const context = await browser.newContext({ locale: "pt-BR" });
  const phone = await context.newPage();
  try {
    await phone.goto(`/m/${qr}`);
    await expect(phone.getByText("No salão")).toBeVisible();
    await phone.goto(`/m/${qr}/pedidos`);
    await phone.getByRole("button", { name: "Histórico" }).click();
    await expect(phone.getByText("Garçom").first()).toBeVisible();
    await expect(phone.getByRole("button", { name: /Cancelar/ })).toHaveCount(0);
  } finally {
    await context.close();
  }
});

test("o segundo pedido do mesmo item para no aviso: tirar ou confirmar", async ({ page, request }) => {
  const { email, headers, tableId, chopp } = await bar(request);
  await loginThroughForm(page, email);
  await allowStaffOrders(page);
  await openTableOrder(page);
  await page.getByRole("button", { name: "Adicionar Guioza" }).click();
  await send(page);
  await expect(page.getByRole("status").filter({ hasText: "enviado para a cozinha" })).toBeVisible();

  // Outro garçom lança o Chopp enquanto esta tela está aberta (aqui, pela API).
  const other = await request.post(`${API_URL}/api/v1/tables/${tableId}/orders`, {
    headers,
    data: { clientRequestId: crypto.randomUUID(), round: false, items: [{ productId: chopp.id, quantity: 1 }] },
  });
  expect(other.status()).toBe(201);

  await page.getByRole("button", { name: "Adicionar Chopp" }).click();
  await page.getByRole("button", { name: "Adicionar Guioza" }).click();
  await send(page);
  const warning = page.getByRole("dialog", { name: "A Mesa 04 já pediu isto" });
  await expect(warning).toContainText("1× Chopp");
  await expect(warning).toContainText("1× Guioza");
  await expect(warning.getByRole("button", { name: "Continuar" })).toBeDisabled();

  await warning.getByRole("button", { name: "Tirar Guioza do pedido" }).click();
  await warning.getByRole("button", { name: "Chopp é outro pedido, enviar mesmo assim" }).click();
  await warning.getByRole("button", { name: "Continuar" }).click();
  await expect(page.getByRole("status").filter({ hasText: "enviado para a cozinha" })).toBeVisible();

  const open = await (await request.get(`${API_URL}/api/v1/tabs/open`, { headers })).json();
  expect(open.content[0].totalCents).toBe(2990 + 1200 + 1200);
});
