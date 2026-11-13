import { expect, test, type APIRequestContext, type Browser, type Page } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

// O celular da mesa e o painel do dono, em dois navegadores, com login: mais tempo.
test.describe.configure({ timeout: 90_000 });

/** Um restaurante com a mesa 04, o Wi-Fi do salão (a rede local do teste) e dois produtos. */
async function restaurant(request: APIRequestContext) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) => (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  const sector = await post("/api/v1/sectors", { name: "Salão principal" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  const entradas = await post("/api/v1/menu-categories", { name: "Entradas" });
  const product = (name: string, priceCents: number) =>
    post("/api/v1/products", { categoryId: entradas.id, name, priceCents, featured: false, available: true });
  await product("Guioza", 2990);
  await product("Sunomono", 1990);
  return { qr: table.publicToken as string, email, headers };
}

/** O celular da mesa pede pelo cardápio e vai para "Meus pedidos". */
async function order(page: Page, qr: string, names: string[]) {
  await page.goto(`/m/${qr}/cardapio`);
  for (const name of names) {
    await page.getByRole("button", { name: `Adicionar ${name}` }).click();
    await page.getByRole("dialog", { name }).getByRole("button", { name: /^Adicionar/ }).click();
    await expect(page.getByRole("dialog")).toBeHidden();
  }
  await page.getByRole("link", { name: /Ver pedido/ }).click();
  await page.getByRole("button", { name: "Enviar pedido para a cozinha" }).click();
  await page.getByRole("link", { name: "Acompanhar pedido" }).click();
}

/** O garçom entrega, pela API, tudo o que está a caminho. */
async function deliverAll(request: APIRequestContext, headers: Record<string, string>) {
  const queue = await (await request.get(`${API_URL}/api/v1/order-items`, { headers })).json();
  for (const item of queue.content) {
    expect((await request.post(`${API_URL}/api/v1/order-items/${item.id}/deliver`, { headers })).status()).toBe(204);
  }
}

async function ownerPanel(browser: Browser, email: string) {
  const context = await browser.newContext();
  const page = await context.newPage();
  await loginThroughForm(page, email);
  return { page, close: () => context.close() };
}

test("a mesa vê o total, pede a conta, e o garçom fecha a comanda", async ({ page, request, browser }) => {
  const { qr, email, headers } = await restaurant(request);
  await order(page, qr, ["Guioza", "Sunomono"]);

  // "A caminho" mostra o que ainda não chegou; depois da entrega, fica vazio, e o histórico guarda tudo.
  await expect(page.getByRole("button", { name: "A caminho (2)" })).toHaveAttribute("aria-pressed", "true");
  await expect(page.getByRole("article", { name: "1× Guioza" })).toContainText("Na fila");
  await deliverAll(request, headers);
  await expect(page.getByText("Nada a caminho")).toBeVisible({ timeout: 3000 });
  await page.getByRole("button", { name: "Histórico" }).click();
  await expect(page.getByRole("region", { name: /Pedido nº 1/ })).toContainText("Entregue");

  await page.goto(`/m/${qr}/conta`);
  await expect(page.getByText("Total da comanda")).toBeVisible();
  await expect(page.getByText("R$ 49,80")).toBeVisible();
  await page.getByRole("radio", { name: /Cartão/ }).check();
  await page.getByRole("button", { name: "Pedir a conta" }).click();
  await expect(page.getByRole("status")).toContainText("Conta solicitada");

  const panel = await ownerPanel(browser, email);
  try {
    await panel.page.goto("/painel/comandas");
    const tabs = panel.page.getByRole("region", { name: /Mesas com comanda aberta/ });
    await expect(tabs).toContainText("R$ 49,80");
    await expect(tabs).toContainText("Tudo entregue");
    await tabs.getByRole("link", { name: "Comanda da mesa 04" }).click();

    await expect(panel.page.getByText("Conta pedida · Cartão")).toBeVisible();
    await expect(panel.page.getByRole("radio", { name: "Cartão" })).toBeChecked();
    await panel.page.getByRole("button", { name: "Recebi no Cartão · fechar comanda" }).click();
    await expect(panel.page.getByRole("status")).toContainText("Comanda fechada");
    await expect(panel.page.getByRole("status")).toContainText("R$ 49,80 no Cartão");

    // O celular da mesa vê na hora, pelo tempo real.
    await expect(page.getByRole("status")).toContainText("Conta fechada", { timeout: 3000 });

    await panel.page.goto("/painel/comandas");
    await expect(panel.page.getByText("Nenhuma comanda aberta.")).toBeVisible();
    await panel.page.goto("/painel");
    // O chamado da conta terminou junto.
    await expect(panel.page.getByText(/Nenhum chamado aberto/)).toBeVisible();
  } finally {
    await panel.close();
  }
});

test("com item a caminho, o garçom vê o que falta e não fecha", async ({ page, request, browser }) => {
  const { qr, email } = await restaurant(request);
  await order(page, qr, ["Guioza"]);

  const panel = await ownerPanel(browser, email);
  try {
    await panel.page.goto("/painel/comandas");
    const tabs = panel.page.getByRole("region", { name: /Mesas com comanda aberta/ });
    await expect(tabs).toContainText("1 item a caminho");
    await tabs.getByRole("link", { name: "Comanda da mesa 04" }).click();
    await expect(panel.page.getByText("Ainda a caminho: 1× Guioza")).toBeVisible();
    await expect(panel.page.getByRole("button", { name: /fechar comanda/ })).toBeDisabled();
  } finally {
    await panel.close();
  }
});

test("a mesa paga com item a caminho, e o garçom fecha depois sem cobrar de novo", async ({
  page,
  request,
  browser,
}) => {
  const { qr, email, headers } = await restaurant(request);
  await order(page, qr, ["Guioza", "Sunomono"]);
  // Só o guioza chega; o sunomono continua na cozinha.
  const queue = await (await request.get(`${API_URL}/api/v1/order-items`, { headers })).json();
  const guioza = queue.content.find((i: { productName: string }) => i.productName === "Guioza");
  expect((await request.post(`${API_URL}/api/v1/order-items/${guioza.id}/deliver`, { headers })).status()).toBe(204);

  await page.goto(`/m/${qr}/conta`);
  await page.getByRole("radio", { name: /Pix/ }).check();
  await page.getByRole("button", { name: "Pedir a conta" }).click();
  await expect(page.getByRole("status")).toContainText("Conta solicitada");

  const panel = await ownerPanel(browser, email);
  try {
    const call = panel.page.getByRole("listitem", { name: "Pedir a conta · Mesa 04" });
    await call.getByRole("button", { name: "Aceitar" }).click();
    await call.getByRole("button", { name: "Receber pagamento" }).click();
    const dialog = panel.page.getByRole("dialog", { name: "Conta da mesa 04" });
    await expect(dialog).toContainText("Falta receber");
    await expect(dialog).toContainText("1 item ainda está a caminho");
    await expect(dialog.getByRole("radio", { name: "Pix" })).toBeChecked();
    await dialog.getByRole("button", { name: "Recebi R$ 49,80 no Pix" }).click();
    await expect(dialog).toBeHidden();
    await expect(panel.page.getByText(/Nenhum chamado aberto/)).toBeVisible();

    // A mesa vê que a conta está paga, mas a comanda continua aberta (o sunomono ainda vem).
    await expect(page.getByRole("status")).toContainText("Conta paga", { timeout: 3000 });
    await expect(page.getByText("Já pago R$ 49,80")).toBeVisible();
    // A tela da mesa também mostra que a conta foi paga, no cartão "Pedir a conta".
    await page.getByRole("link", { name: "Voltar para a mesa" }).click();
    await expect(page.getByRole("link", { name: /Pedir a conta/ })).toContainText("Conta paga ✓");
    await page.getByRole("link", { name: /Pedir a conta/ }).click();
    await expect(page.getByRole("status")).toContainText("Conta paga");

    // Na tela Mesas, a comanda também mostra o que foi pago: a tag e o pagamento com a forma e a hora.
    await panel.page.goto("/painel/mesas");
    await panel.page.getByRole("button", { name: /Mesa 04: abrir a comanda/ }).click();
    const drawer = panel.page.getByRole("complementary", { name: "Mesa 04" });
    await expect(drawer).toContainText("Tudo pago");
    await expect(drawer.getByRole("list", { name: "Já pago" })).toContainText(/Pago · Pix · \d{2}:\d{2}/);
    await expect(drawer).toContainText("Falta receber");

    await deliverAll(request, headers);
    await panel.page.goto("/painel/comandas");
    const tabs = panel.page.getByRole("region", { name: /Mesas com comanda aberta/ });
    await tabs.getByRole("link", { name: "Comanda da mesa 04" }).click();
    await expect(panel.page.getByText(/Pago · Pix · \d{2}:\d{2}/)).toBeVisible();
    await expect(panel.page.getByText("Tudo pago")).toBeVisible();
    // Sem nada a receber, não pergunta a forma de pagamento: o botão só fecha.
    await expect(panel.page.getByRole("radio", { name: "Cartão" })).toHaveCount(0);
    await panel.page.getByRole("button", { name: "Fechar comanda", exact: true }).click();
    await expect(panel.page.getByRole("status")).toContainText("R$ 49,80 no Pix");
    await expect(page.getByRole("status")).toContainText("Conta fechada", { timeout: 3000 });
  } finally {
    await panel.close();
  }
});

test("o mapa do salão mostra cada mesa e abre a comanda ao lado", async ({ page, request, browser }) => {
  const { qr, email } = await restaurant(request);
  await order(page, qr, ["Guioza"]);
  await page.goto(`/m/${qr}/conta`);
  await page.getByRole("radio", { name: /Cartão/ }).check();
  await page.getByRole("button", { name: "Pedir a conta" }).click();

  const panel = await ownerPanel(browser, email);
  try {
    // Desenho 7.2: o salão por setor; a mesa 04 pediu a conta.
    await panel.page.goto("/painel/mesas");
    await expect(panel.page.getByText("1 de 1 ocupadas · 1 pedindo a conta")).toBeVisible();
    const salon = panel.page.getByRole("region", { name: "Salão principal" });
    await salon.getByRole("button", { name: /Mesa 04: abrir a comanda · Conta · Cartão/ }).click();

    const drawer = panel.page.getByRole("complementary", { name: "Mesa 04" });
    await expect(drawer).toContainText("Comanda aberta");
    await expect(drawer).toContainText("Conta pedida · Cartão");
    await expect(drawer).toContainText("1× Guioza");
    await expect(drawer).toContainText("Na fila");
    await expect(drawer).toContainText("R$ 29,90");
    await drawer.getByRole("link", { name: "Ver comanda e fechar" }).click();
    await expect(panel.page).toHaveURL(/\/painel\/comandas\/[0-9a-f-]+$/);
    await expect(panel.page.getByRole("heading", { name: "Comanda aberta" })).toBeVisible();
    await expect(panel.page.getByRole("main").getByText("Conta pedida · Cartão")).toBeVisible();
  } finally {
    await panel.close();
  }
});
