import { expect, test, type APIRequestContext, type Browser, type Page } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm, openMenu } from "./support/account";

// O celular da mesa e o painel do dono, em dois navegadores, com login: mais tempo.
test.describe.configure({ timeout: 90_000 });

/** Um restaurante com a mesa 04, o Wi-Fi do salão (a rede local do teste) e dois produtos. */
async function restaurant(request: APIRequestContext, orders?: object) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) => (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  if (orders) await request.put(`${API_URL}/api/v1/order-settings`, { headers, data: orders });
  const sector = await post("/api/v1/sectors", { name: "Salão principal" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  const entradas = await post("/api/v1/menu-categories", { name: "Entradas" });
  const product = (name: string, priceCents: number) =>
    post("/api/v1/products", { categoryId: entradas.id, name, priceCents, featured: false, available: true });
  await product("Guioza", 2990);
  await product("Sunomono", 1990);
  return { qr: table.publicToken as string, email };
}

/** O celular da mesa pede pelo cardápio: um de cada produto. */
async function order(page: Page, qr: string, names: string[]) {
  await page.goto(`/m/${qr}/cardapio`);
  for (const name of names) {
    await page.getByRole("button", { name: `Adicionar ${name}` }).click();
    await page.getByRole("dialog", { name }).getByRole("button", { name: /^Adicionar/ }).click();
    await expect(page.getByRole("dialog")).toBeHidden();
  }
  await page.getByRole("link", { name: /Ver pedido/ }).click();
  await page.getByRole("button", { name: "Enviar pedido para a cozinha" }).click();
  await expect(page.getByRole("status")).toContainText(/Pedido (enviado|aguardando)/);
  await page.getByRole("link", { name: "Acompanhar pedido" }).click();
}

/** O painel do dono, num navegador separado do celular da mesa. */
async function ownerPanel(browser: Browser, email: string) {
  const context = await browser.newContext();
  const page = await context.newPage();
  await loginThroughForm(page, email);
  return { page, close: () => context.close() };
}

test("a cozinha começa e marca pronto, o garçom entrega, e a mesa vê cada passo", async ({ page, request, browser }) => {
  const { qr, email } = await restaurant(request);
  await order(page, qr, ["Guioza", "Sunomono"]);
  const mine = page.getByRole("region", { name: /Pedido nº 1/ });
  await expect(mine).toContainText("Na fila");

  const panel = await ownerPanel(browser, email);
  try {
    const kitchen = panel.page;
    await openMenu(kitchen);
    await kitchen.getByRole("navigation", { name: "Navegação" }).getByRole("link", { name: "Cozinha" }).click();
    await expect(kitchen).toHaveURL(/\/painel\/cozinha$/);
    const queue = kitchen.getByRole("region", { name: "Na fila" });
    const ticket = queue.getByRole("article", { name: "Mesa 04 · Pedido nº 1" });
    await expect(ticket).toContainText("Guioza");
    await expect(ticket).toContainText("Sunomono");

    await ticket.getByRole("button", { name: "Começar" }).click();
    const preparing = kitchen.getByRole("region", { name: "Em preparo" }).getByRole("article");
    await expect(preparing).toContainText("Guioza");
    // A mesa vê na hora, pelo tempo real.
    await expect(mine.getByText("Em preparo")).toHaveCount(2, { timeout: 3000 });

    await kitchen.getByRole("button", { name: "Marcar Guioza como pronto" }).click();
    await expect(kitchen.getByRole("region", { name: /Pronto/ }).getByRole("article")).toContainText("Guioza");
    await kitchen.getByRole("button", { name: "Tudo pronto" }).click();
    await expect(kitchen.getByRole("region", { name: "Em preparo" }).getByRole("article")).toHaveCount(0);
    await expect(mine.getByText("Pronto")).toHaveCount(2, { timeout: 3000 });

    // O garçom (aqui, o dono) leva os pratos: "Entregas" no painel.
    // (O botão do Next em desenvolvimento cobre o "Voltar ao painel" no canto: o teste vai direto.)
    await kitchen.goto("/painel");
    const deliveries = kitchen.getByRole("region", { name: /Entregas/ });
    await deliveries.getByRole("listitem", { name: "Mesa 04 · 1× Guioza" }).getByRole("button", { name: "Entregue" }).click();
    await expect(deliveries.getByRole("listitem")).toHaveCount(1);
    // O entregue sai de "A caminho" (o Sunomono continua, pronto) e fica no histórico.
    await expect(mine).not.toContainText("Guioza", { timeout: 3000 });
    await expect(mine).toContainText("Sunomono");
    await page.getByRole("button", { name: "Histórico" }).click();
    await expect(mine.getByText("Entregue")).toBeVisible();
  } finally {
    await panel.close();
  }
});

test("a cozinha cancela um item com motivo, e o cliente vê o motivo", async ({ page, request, browser }) => {
  const { qr, email } = await restaurant(request);
  await order(page, qr, ["Guioza", "Sunomono"]);

  const panel = await ownerPanel(browser, email);
  try {
    await panel.page.goto("/painel/cozinha");
    const ticket = panel.page.getByRole("article", { name: "Mesa 04 · Pedido nº 1" });
    await ticket.getByRole("button", { name: "Cancelar um item" }).click();
    const dialog = panel.page.getByRole("dialog", { name: "Cancelar item" });
    await dialog.getByLabel("1× Sunomono").check();
    await dialog.getByRole("button", { name: "Outro motivo" }).click();
    await dialog.getByLabel("Escreva o motivo").fill("Acabou o pepino japonês");
    await expect(dialog).toContainText("Sunomono: Acabou o pepino japonês");
    await dialog.getByRole("button", { name: "Cancelar item" }).click();
    await expect(dialog).toBeHidden();
    await expect(ticket).not.toContainText("Sunomono");

    const mine = page.getByRole("region", { name: /Pedido nº 1/ });
    await expect(mine).toContainText("Acabou o pepino japonês", { timeout: 3000 });
    await expect(mine).toContainText("Cancelado");
  } finally {
    await panel.close();
  }
});

test("o garçom confirma um pedido e recusa outro", async ({ page, request, browser }) => {
  const { qr, email } = await restaurant(request, { orderingEnabled: true, approvalRequired: true });
  await order(page, qr, ["Guioza"]);
  await order(page, qr, ["Sunomono"]);

  const panel = await ownerPanel(browser, email);
  try {
    const approvals = panel.page.getByRole("region", { name: /Pedidos para confirmar/ });
    await expect(approvals.getByRole("listitem", { name: "Mesa 04 · Pedido nº 1" })).toContainText("R$ 29,90");
    await approvals.getByRole("listitem", { name: "Mesa 04 · Pedido nº 1" }).getByRole("button", { name: "Confirmar" }).click();
    await approvals.getByRole("listitem", { name: "Mesa 04 · Pedido nº 2" }).getByRole("button", { name: "Recusar" }).click();
    await panel.page.getByRole("alertdialog").getByRole("button", { name: "Recusar pedido" }).click();
    await expect(approvals).toBeHidden();

    await expect(page.getByRole("region", { name: /Pedido nº 2/ })).toContainText("Recusado pelo garçom", {
      timeout: 3000,
    });
    await expect(page.getByRole("region", { name: /Pedido nº 1/ })).toContainText("Na fila");
    await panel.page.goto("/painel/cozinha");
    await expect(panel.page.getByRole("article", { name: "Mesa 04 · Pedido nº 1" })).toBeVisible();
    await expect(panel.page.getByRole("article", { name: "Mesa 04 · Pedido nº 2" })).toHaveCount(0);
  } finally {
    await panel.close();
  }
});
