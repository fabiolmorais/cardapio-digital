import { expect, test, type APIRequestContext, type Page } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

// Dois celulares por teste (ou o celular e a gestão, com login): mais tempo.
test.describe.configure({ timeout: 60_000 });

/**
 * Um restaurante com a mesa 04, o Wi-Fi do salão (a rede local do teste) e três produtos.
 * `orders` muda as configurações de pedido (os padrões: ligado, R$ 300 e 10 itens).
 */
async function restaurant(request: APIRequestContext, orders?: object) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) => (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  if (orders) {
    const response = await request.put(`${API_URL}/api/v1/order-settings`, { headers, data: orders });
    expect(response.ok()).toBeTruthy();
  }
  const sector = await post("/api/v1/sectors", { name: "Salão principal" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  const entradas = await post("/api/v1/menu-categories", { name: "Entradas" });
  const product = (name: string, priceCents: number, extra: object = {}) =>
    post("/api/v1/products", { categoryId: entradas.id, name, priceCents, featured: false, available: true, ...extra });
  await product("Guioza", 2990);
  await product("Sunomono", 1990, { promoPriceCents: 1590 });
  await product("Chá verde gelado", 1200);
  return { qr: table.publicToken as string, email, headers };
}

/** No cardápio, abre o prato, põe a observação e a quantidade e adiciona. */
async function add(page: Page, name: string, quantity = 1, note = "") {
  await page.getByRole("button", { name: `Adicionar ${name}` }).click();
  const sheet = page.getByRole("dialog", { name });
  await expect(sheet).toBeVisible();
  if (note) await sheet.getByLabel("Alguma observação?").fill(note);
  for (let i = 1; i < quantity; i++) await sheet.getByRole("button", { name: "Aumentar" }).click();
  await sheet.getByRole("button", { name: /^Adicionar/ }).click();
  await expect(sheet).toBeHidden();
}

test("o celular monta o carrinho, envia, acompanha e cancela um item na fila", async ({ page, request, browser }) => {
  const { qr } = await restaurant(request);
  await page.goto(`/m/${qr}/cardapio`);

  await add(page, "Guioza", 2, "sem cebolinha");
  await add(page, "Sunomono");
  // O carrinho fica no celular: a barra mostra o total, e o F5 não perde nada.
  await page.reload();
  const bar = page.getByRole("link", { name: /Ver pedido/ });
  await expect(bar).toContainText("3 itens");
  await expect(bar).toContainText("R$ 75,70"); // 2 × 29,90 + 15,90 (o preço promocional)
  await bar.click();

  await expect(page).toHaveURL(new RegExp(`/m/${qr}/carrinho$`));
  await expect(page.getByRole("heading", { name: "Seu pedido" })).toBeVisible();
  await expect(page.getByRole("button", { name: /Observação: sem cebolinha/ })).toBeVisible();
  await page.getByRole("button", { name: "Aumentar Sunomono" }).click();
  await expect(page.getByText("R$ 91,60").first()).toBeVisible();
  await page.getByRole("button", { name: "Enviar pedido para a cozinha" }).click();

  const sent = page.getByRole("status");
  await expect(sent).toContainText("Pedido enviado!");
  await expect(sent).toContainText("Pedido nº 1");
  // Desenho 4.2: cada item com a tag do passo.
  await expect(page.getByRole("listitem").filter({ hasText: "2× Guioza" })).toContainText("Na fila");
  // O carrinho esvaziou: a barra some do cardápio.
  await page.getByRole("link", { name: "Acompanhar pedido" }).click();

  // Desenho 4.3: um card por item a caminho, com os minutos, a observação e a tag.
  await expect(page).toHaveURL(new RegExp(`/m/${qr}/pedidos$`));
  await expect(page.getByRole("button", { name: "A caminho (2)" })).toHaveAttribute("aria-pressed", "true");
  const guioza = page.getByRole("article", { name: "2× Guioza" });
  await expect(guioza).toContainText("sem cebolinha");
  await expect(guioza).toContainText("Na fila");
  await expect(guioza).toContainText("agora");
  await expect(page.getByText("R$ 91,60").first()).toBeVisible();

  // Outro celular da mesa vê os itens, mas não cancela o que não pediu; e vê o cancelamento na hora.
  const other = await browser.newContext();
  const phone = await other.newPage();
  try {
    await phone.goto(`/m/${qr}/pedidos`);
    await expect(phone.getByRole("article", { name: "2× Sunomono" })).toBeVisible();
    await expect(phone.getByRole("button", { name: /Cancelar/ })).toHaveCount(0);

    await page.getByRole("button", { name: "Cancelar Sunomono" }).click();
    await expect(page.getByRole("article", { name: "2× Sunomono" })).toHaveCount(0);
    await expect(page.getByText("R$ 59,80").first()).toBeVisible();
    // O cancelamento chega ao outro celular pelo tempo real, sem recarregar.
    await expect(phone.getByText("R$ 59,80").first()).toBeVisible({ timeout: 3000 });
    // O aviso diz, em destaque, quem cancelou: o próprio dono do pedido.
    await expect(page.getByText("2× Sunomono cancelado por você.")).toBeVisible();
    await expect(phone.getByText("2× Sunomono cancelado por quem fez o pedido.")).toBeVisible();
  } finally {
    await other.close();
  }

  // Desenho 4.4: o histórico agrupa por pedido, com todos os itens, inclusive o cancelado.
  await page.getByRole("button", { name: "Histórico" }).click();
  const order = page.getByRole("region", { name: /Pedido nº 1/ });
  await expect(order).toContainText("Este celular");
  await expect(order.getByRole("listitem").filter({ hasText: "2× Sunomono" })).toContainText("Cancelado");
  await expect(order.getByRole("listitem").filter({ hasText: "2× Guioza" })).toContainText("Na fila");

  await page.getByRole("link", { name: "Cardápio", exact: true }).click();
  await expect(page.getByRole("link", { name: /Ver pedido/ })).toHaveCount(0);
});

test("o pedido acima do limite fica esperando o garçom", async ({ page, request }) => {
  const { qr } = await restaurant(request, { orderingEnabled: true, approvalRequired: false, maxItems: 2 });
  await page.goto(`/m/${qr}/cardapio`);

  await add(page, "Chá verde gelado", 3);
  await page.getByRole("link", { name: /Ver pedido/ }).click();
  await page.getByRole("button", { name: "Enviar pedido para a cozinha" }).click();

  await expect(page.getByRole("status")).toContainText("Pedido aguardando o garçom");
  await page.getByRole("link", { name: "Acompanhar pedido" }).click();
  await expect(page.getByRole("article", { name: "3× Chá verde gelado" })).toContainText("Aguardando o garçom");
});

test("depois de enviar, \"Pedir mais alguma coisa\" leva ao cardápio e o carrinho mostra o pedido novo", async ({
  page,
  request,
}) => {
  const { qr } = await restaurant(request);
  await page.goto(`/m/${qr}/cardapio`);
  await add(page, "Guioza");
  await page.getByRole("link", { name: /Ver pedido/ }).click();
  await page.getByRole("button", { name: "Enviar pedido para a cozinha" }).click();
  await expect(page.getByRole("status")).toContainText("Pedido nº 1");

  await page.getByRole("link", { name: "Pedir mais alguma coisa" }).click();
  await expect(page).toHaveURL(new RegExp(`/m/${qr}/cardapio$`));
  await add(page, "Sunomono");
  await page.getByRole("link", { name: /Ver pedido/ }).click();

  // O resumo do pedido novo, e não o "Pedido enviado!" do anterior.
  await expect(page.getByRole("heading", { name: "Seu pedido" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Sunomono" })).toBeVisible();
  await expect(page.getByText("Pedido enviado!")).toHaveCount(0);
  await page.getByRole("button", { name: "Enviar pedido para a cozinha" }).click();
  await expect(page.getByRole("status")).toContainText("Pedido nº 2");
});

test("com o pedido desligado, o cardápio não tem o botão de adicionar", async ({ page, request }) => {
  const { qr } = await restaurant(request, { orderingEnabled: false, approvalRequired: false });
  await page.goto(`/m/${qr}/cardapio`);

  await expect(page.getByRole("heading", { name: "Guioza" })).toBeVisible();
  await expect(page.getByRole("button", { name: /^Adicionar/ })).toHaveCount(0);
});

test("o dono liga a confirmação do garçom e muda os limites", async ({ page, request }) => {
  const { email, headers } = await restaurant(request);
  await loginThroughForm(page, email);
  await page.goto("/admin/presenca");

  await expect(page.getByRole("switch", { name: /Pedido pelo celular/ })).toBeChecked();
  await expect(page.getByLabel("Valor por pedido")).toHaveValue("300,00");
  await expect(page.getByLabel("Itens por pedido")).toHaveValue("10");

  await page.getByRole("switch", { name: /Garçom confirma todo pedido/ }).check();
  await page.getByLabel("Valor por pedido").fill("150");
  await page.getByLabel("Itens por pedido").fill("");
  await page.getByRole("button", { name: "Salvar" }).click();
  await expect(page.getByRole("status")).toHaveText("Configuração salva.");

  const saved = await (await request.get(`${API_URL}/api/v1/order-settings`, { headers })).json();
  expect(saved.orderingEnabled).toBe(true);
  expect(saved.approvalRequired).toBe(true);
  expect(saved.maxAmountCents).toBe(15000);
  expect(saved.maxItems ?? null).toBeNull();
});

test("a mesa vê o total e quanto cada celular pediu", async ({ page, request, browser }) => {
  const { qr } = await restaurant(request);
  const other = await browser.newContext();
  const phone = await other.newPage();
  try {
    for (const [p, name] of [
      [page, "Guioza"],
      [phone, "Chá verde gelado"],
    ] as const) {
      await p.goto(`/m/${qr}/cardapio`);
      await add(p, name);
      await p.getByRole("link", { name: /Ver pedido/ }).click();
      await p.getByRole("button", { name: "Enviar pedido para a cozinha" }).click();
      await p.getByRole("link", { name: "Acompanhar pedido" }).click();
    }

    // "A caminho": o total da mesa e, embaixo, o que este celular pediu.
    await page.reload();
    await expect(page.getByText("Total da mesa")).toBeVisible();
    await expect(page.getByText("R$ 41,90").first()).toBeVisible();
    await expect(page.getByText("Este celular · R$ 29,90")).toBeVisible();

    // "Histórico": o total e quanto cada celular pediu, sem dizer quem é.
    await page.getByRole("button", { name: "Histórico" }).click();
    const byPhone = page.getByRole("list", { name: "Por celular" });
    await expect(byPhone.getByRole("listitem").filter({ hasText: "Este celular" })).toContainText("R$ 29,90");
    await expect(byPhone.getByRole("listitem").filter({ hasText: "Celular 2" })).toContainText("R$ 12,00");
    await expect(page.getByRole("region", { name: /Pedido nº 2/ })).toContainText("Celular 2");

    // O outro celular se vê como "Este celular", e o primeiro vira o "Celular 2".
    await phone.getByRole("button", { name: "Histórico" }).click();
    const theirs = phone.getByRole("list", { name: "Por celular" });
    await expect(theirs.getByRole("listitem").filter({ hasText: "Este celular" })).toContainText("R$ 12,00");
    await expect(theirs.getByRole("listitem").filter({ hasText: "Celular 2" })).toContainText("R$ 29,90");
  } finally {
    await other.close();
  }
});
