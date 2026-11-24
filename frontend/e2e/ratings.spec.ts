import { expect, test, type APIRequestContext, type Page } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

// O celular da mesa e o painel do dono, em dois navegadores, com login: mais tempo.
test.describe.configure({ timeout: 90_000 });

const REVIEW = "https://g.page/r/CSakuraExemplo/review";

/** Um restaurante com a marca (e o link do Google), a mesa 04 e o Guioza. */
async function restaurant(request: APIRequestContext) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) =>
    (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/establishment/brand`, {
    headers,
    data: { name: "Sakura Sushi", color: "PETROL", theme: "DARK", googleReviewUrl: REVIEW },
  });
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  const sector = await post("/api/v1/sectors", { name: "Salão principal" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  const entradas = await post("/api/v1/menu-categories", { name: "Entradas" });
  const guioza = await post("/api/v1/products", {
    categoryId: entradas.id,
    name: "Guioza",
    priceCents: 2990,
    featured: false,
    available: true,
  });
  return { qr: table.publicToken as string, email, headers, guioza: guioza.id as string };
}

/** O celular da página abre a visita (com a presença pela rede) e pede um Guioza; devolve o pedido. */
async function phoneOrders(page: Page, qr: string, guioza: string) {
  const tableUrl = `${API_URL}/api/v1/public/tables/${qr}`;
  await page.request.post(`${tableUrl}/session`);
  await page.request.post(`${tableUrl}/presence`);
  return (
    await page.request.post(`${tableUrl}/orders`, { data: { items: [{ productId: guioza, quantity: 1 }] } })
  ).json();
}

async function requestBill(page: Page, qr: string) {
  await page.goto(`/m/${qr}/conta`);
  await page.getByRole("radio", { name: /Pix/ }).check();
  await page.getByRole("button", { name: "Pedir a conta" }).click();
  await expect(page.getByRole("status").first()).toContainText("Conta solicitada");
}

test("nota 5: a mesa vê o convite ao Google, e o dono vê a avaliação do dia", async ({ page, request, browser }) => {
  const { qr, email, guioza } = await restaurant(request);
  await phoneOrders(page, qr, guioza);
  // O Google não é chamado de verdade no teste.
  await page.context().route("https://g.page/**", (route) => route.fulfill({ body: "Google" }));
  await requestBill(page, qr);

  const card = page.getByRole("region", { name: "Como foi o atendimento?" });
  await expect(card.getByRole("button", { name: "Enviar avaliação" })).toBeDisabled();
  await card.getByRole("button", { name: "5 estrelas" }).click();
  await expect(card).toContainText("Excelente");
  await card.getByRole("button", { name: "Comida" }).click();
  await expect(card.getByRole("button", { name: "Comida" })).toHaveAttribute("aria-pressed", "true");
  await card.getByLabel("Quer contar mais? (opcional)").fill("Guioza perfeito!");
  await card.getByRole("button", { name: "Enviar avaliação" }).click();

  await expect(page.getByText("Obrigado! Que bom que gostou")).toBeVisible();
  const google = page.getByRole("link", { name: /Avaliar no Google/ });
  await expect(google).toHaveAttribute("href", REVIEW);
  const [googleTab] = await Promise.all([page.context().waitForEvent("page"), google.click()]);
  await googleTab.close();

  const context = await browser.newContext();
  const panel = await context.newPage();
  try {
    await loginThroughForm(panel, email);
    await panel.goto("/painel/avaliacoes");
    await expect(panel.getByRole("heading", { name: "Avaliações de hoje" })).toBeVisible();
    await expect(panel.getByText("5,0")).toBeVisible();
    const list = panel.getByRole("region", { name: "Avaliações", exact: true });
    await expect(list).toContainText("Comida");
    await expect(list).toContainText("“Guioza perfeito!”");
    await expect(list.getByRole("img", { name: "Nota 5 de 5" })).toBeVisible();
    // O toque no Google é contado sem segurar a página.
    await expect(list).toContainText("Foi ao Google", { timeout: 10_000 });
  } finally {
    await context.close();
  }
});

test('nota 1: o alerta chega ao painel na hora e sai com "Já resolvi"', async ({ page, request, browser }) => {
  const { qr, email, guioza } = await restaurant(request);
  const context = await browser.newContext();
  const panel = await context.newPage();
  try {
    await loginThroughForm(panel, email);
    await phoneOrders(page, qr, guioza);
    await requestBill(page, qr);

    const card = page.getByRole("region", { name: "Como foi o atendimento?" });
    await card.getByRole("button", { name: "1 estrela" }).click();
    await expect(card).toContainText("O que pode melhorar?");
    await card.getByRole("button", { name: "Rapidez" }).click();
    await card.getByLabel("Quer contar mais? (opcional)").fill("A conta demorou");
    await card.getByRole("button", { name: "Enviar avaliação" }).click();
    await expect(page.getByText("Obrigado por contar")).toBeVisible();
    await expect(page.getByRole("link", { name: /Avaliar no Google/ })).toHaveCount(0);

    const alerts = panel.getByRole("region", { name: "Notas baixas para resolver" });
    await expect(alerts).toContainText("Nota 1 na Mesa 04", { timeout: 10_000 });
    await expect(alerts).toContainText("“A conta demorou” · Marcou: Rapidez");
    await alerts.getByRole("button", { name: "Já resolvi" }).click();
    await expect(alerts).toHaveCount(0);
  } finally {
    await context.close();
  }
});

test("depois de a comanda fechar, a conta fechada ainda deixa avaliar", async ({ page, request }) => {
  const { qr, headers, guioza } = await restaurant(request);
  const order = await phoneOrders(page, qr, guioza);
  await page.goto(`/m/${qr}/conta`);
  await expect(page.getByRole("button", { name: "Pedir a conta" })).toBeVisible();

  for (const item of order.items) await request.post(`${API_URL}/api/v1/order-items/${item.id}/deliver`, { headers });
  const open = await (await request.get(`${API_URL}/api/v1/tabs/open`, { headers })).json();
  const closed = await request.post(`${API_URL}/api/v1/tabs/${open.content[0].id}/close`, {
    headers,
    data: { paymentMethod: "PIX" },
  });
  expect(closed.ok(), await closed.text()).toBe(true);

  await expect(page.getByText("Conta fechada")).toBeVisible({ timeout: 10_000 });
  const card = page.getByRole("region", { name: "Como foi o atendimento?" });
  await card.getByRole("button", { name: "3 estrelas" }).click();
  await card.getByRole("button", { name: "Enviar avaliação" }).click();
  await expect(page.getByText("Obrigado pela avaliação")).toBeVisible();
});

test("o dono liga a nota do garçom em Presença e pedidos", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await page.goto("/admin/presenca");

  const toggle = page.getByLabel("Garçom vê a própria nota");
  await expect(toggle).not.toBeChecked();
  await toggle.check();
  await page.getByRole("button", { name: /^Salvar/ }).click();
  await expect(page.getByRole("status").filter({ hasText: /salv/i })).toBeVisible();

  await page.reload();
  await expect(page.getByLabel("Garçom vê a própria nota")).toBeChecked();
});
