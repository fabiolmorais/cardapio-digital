import { devices, expect, test, type APIRequestContext, type Browser, type Page } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

// O celular da mesa e o painel do dono, em dois navegadores, com login: mais tempo.
test.describe.configure({ timeout: 90_000 });

/** Um restaurante com a mesa 04, o Guioza e a taxa de serviço de 10% ligada (RF39). */
async function restaurant(request: APIRequestContext) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) =>
    (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  const saved = await request.put(`${API_URL}/api/v1/order-settings`, {
    headers,
    data: {
      orderingEnabled: true,
      approvalRequired: false,
      serviceCharge: { enabled: true, percent: 10, destination: "TEAM" },
    },
  });
  expect(saved.status()).toBe(200);
  const sector = await post("/api/v1/sectors", { name: "Salão principal" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  const entradas = await post("/api/v1/menu-categories", { name: "Entradas" });
  await post("/api/v1/products", {
    categoryId: entradas.id,
    name: "Guioza",
    priceCents: 2990,
    featured: false,
    available: true,
  });
  return { qr: table.publicToken as string, email };
}

async function orderGuioza(page: Page, qr: string) {
  await page.goto(`/m/${qr}/cardapio`);
  await page.getByRole("button", { name: "Adicionar Guioza" }).click();
  await page
    .getByRole("dialog", { name: "Guioza" })
    .getByRole("button", { name: /^Adicionar/ })
    .click();
  await expect(page.getByRole("dialog")).toBeHidden();
  await page.getByRole("link", { name: /Ver pedido/ }).click();
  await page.getByRole("button", { name: "Enviar pedido para a cozinha" }).click();
  await page.getByRole("link", { name: "Acompanhar pedido" }).click();
}

test("a mesa vê os 10% já marcados, tira, e o garçom confirma na maquininha", async ({ page, request, browser }) => {
  const { qr, email } = await restaurant(request);
  await orderGuioza(page, qr);

  // Na tela da conta, o serviço vem marcado e o total já o inclui.
  await page.goto(`/m/${qr}/conta`);
  const service = page.getByRole("switch", { name: "Pagar os 10% de serviço" });
  await expect(service).toBeChecked();
  await expect(page.getByText("Serviço 10% (opcional)")).toBeVisible();
  await expect(page.getByText("Opcional. Vai para a equipe do salão.")).toBeVisible();
  await expect(page.getByText("Total com serviço")).toBeVisible();
  await expect(page.getByText("R$ 32,89")).toBeVisible();

  // O cliente tira os 10% e pede a conta.
  await service.uncheck({ force: true });
  await expect(page.getByText("Total sem serviço")).toBeVisible();
  await expect(page.getByText("R$ 29,90", { exact: true }).first()).toBeVisible();
  await page.getByRole("radio", { name: /Pix/ }).check();
  await page.getByRole("button", { name: "Pedir a conta" }).click();
  await expect(page.getByRole("status")).toContainText("Conta solicitada");

  const context = await browser.newContext();
  const panel = await context.newPage();
  try {
    await loginThroughForm(panel, email);
    const call = panel.getByRole("listitem", { name: "Pedir a conta · Mesa 04" });
    await call.getByRole("button", { name: "Aceitar" }).click();
    await call.getByRole("button", { name: "Receber pagamento" }).click();
    const dialog = panel.getByRole("dialog", { name: "Conta da mesa 04" });
    // O garçom vê que o cliente tirou o serviço...
    const charge = dialog.getByRole("switch", { name: "Cobrar os 10% de serviço" });
    await expect(charge).not.toBeChecked();
    await expect(dialog.getByRole("button", { name: "Recebi R$ 29,90 no Pix" })).toBeVisible();
    // ...mas, na maquininha, o cliente mudou de ideia.
    // Toca na linha, como o garçom: o input escondido (sr-only), no celular, pode ficar fora da tela.
    await dialog.locator("label", { has: panel.getByRole("switch", { name: "Cobrar os 10% de serviço" }) }).click();
    await expect(charge).toBeChecked();
    await dialog.getByRole("button", { name: "Recebi R$ 32,89 no Pix" }).click();
    await expect(dialog).toBeHidden();

    // A mesa vê a conta paga, com o serviço.
    await expect(page.getByText("Já pago R$ 32,89")).toBeVisible({ timeout: 5000 });
  } finally {
    await context.close();
  }
});

test("o dono liga a taxa, muda o percentual e escolhe o garçom que atendeu", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await page.goto("/admin/presenca");

  await page.getByLabel("Cobrar taxa de serviço").check();
  await page.getByLabel("Percentual").fill("12");
  await page.getByRole("radio", { name: /Garçom que atendeu/ }).check();
  await page.getByRole("button", { name: /^Salvar/ }).click();
  await expect(page.getByRole("status").filter({ hasText: /salv/i })).toBeVisible();

  await page.reload();
  await expect(page.getByLabel("Cobrar taxa de serviço")).toBeChecked();
  await expect(page.getByLabel("Percentual")).toHaveValue("12");
  await expect(page.getByRole("radio", { name: /Garçom que atendeu/ })).toBeChecked();
});

/**
 * Um restaurante com uma comanda de hoje já entregue e fechada pela API (o dono entrega, então é
 * ele o garçom que atendeu). `serviceDestination` liga a taxa de 10%.
 */
async function closedTabToday(
  request: APIRequestContext,
  browser: Browser,
  { priceCents, serviceDestination }: { priceCents: number; serviceDestination?: "TEAM" | "WAITER" },
) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) =>
    (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  if (serviceDestination) {
    const saved = await request.put(`${API_URL}/api/v1/order-settings`, {
      headers,
      data: {
        orderingEnabled: true,
        approvalRequired: false,
        serviceCharge: { enabled: true, percent: 10, destination: serviceDestination },
      },
    });
    expect(saved.status()).toBe(200);
  }
  const sector = await post("/api/v1/sectors", { name: "Salão" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  const carnes = await post("/api/v1/menu-categories", { name: "Carnes" });
  const product = await post("/api/v1/products", {
    categoryId: carnes.id,
    name: "Wagyu",
    priceCents,
    featured: false,
    available: true,
  });
  const phone = await browser.newContext();
  const tableUrl = `${API_URL}/api/v1/public/tables/${table.publicToken}`;
  await phone.request.post(`${tableUrl}/session`);
  await phone.request.post(`${tableUrl}/presence`);
  const order = await (
    await phone.request.post(`${tableUrl}/orders`, { data: { items: [{ productId: product.id, quantity: 1 }] } })
  ).json();
  await phone.close();
  // Acima de R$ 300, o pedido espera o garçom confirmar.
  if (order.status !== "CONFIRMED") await request.post(`${API_URL}/api/v1/orders/${order.id}/approve`, { headers });
  for (const item of order.items) await request.post(`${API_URL}/api/v1/order-items/${item.id}/deliver`, { headers });
  const open = await (await request.get(`${API_URL}/api/v1/tabs/open`, { headers })).json();
  const closed = await request.post(`${API_URL}/api/v1/tabs/${open.content[0].id}/close`, {
    headers,
    data: { paymentMethod: "PIX", ...(serviceDestination ? { serviceAccepted: true } : {}) },
  });
  expect(closed.ok(), await closed.text()).toBe(true);
  return { email };
}

test("no celular do dono, um faturado grande cabe no card do dia", async ({ request, browser }) => {
  const { email } = await closedTabToday(request, browser, { priceCents: 1_200_000 });

  const context = await browser.newContext({ ...devices["Pixel 7"] });
  const panel = await context.newPage();
  try {
    await loginThroughForm(panel, email);
    const value = panel.getByText("R$ 12.000,00", { exact: true });
    await expect(value).toBeVisible();
    // O valor não vaza do card, e a página não ganha rolagem para o lado.
    const card = (await value.locator("..").boundingBox())!;
    const box = (await value.boundingBox())!;
    expect(box.x + box.width).toBeLessThanOrEqual(card.x + card.width);
    expect(await value.evaluate((el) => el.scrollWidth <= el.clientWidth)).toBe(true);
    expect(await panel.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
      panel.viewportSize()!.width,
    );
  } finally {
    await context.close();
  }
});

test("com o serviço para o garçom, o card mostra o total e o modal divide por garçom", async ({
  page,
  request,
  browser,
}) => {
  const { email } = await closedTabToday(request, browser, { priceCents: 2990, serviceDestination: "WAITER" });
  await loginThroughForm(page, email);

  const summary = page.getByRole("region", { name: "Resumo" });
  await expect(summary.getByText("R$ 29,90", { exact: true })).toBeVisible();
  await summary.getByRole("button", { name: /\+ R\$ 2,99 de serviço\s*Ver por garçom/ }).click();

  const dialog = page.getByRole("dialog", { name: "Serviço de hoje por garçom" });
  const rows = dialog.getByRole("listitem");
  // O dono entregou o prato: é ele o garçom que atendeu. Depois, o total.
  await expect(rows).toHaveCount(2);
  await expect(rows.first()).toContainText("R$ 2,99");
  await expect(rows.last()).toContainText("Total");
  await expect(rows.last()).toContainText("R$ 2,99");
  await expect(dialog.getByText("Caixinha da equipe")).toHaveCount(0);
  await dialog.getByRole("button", { name: "Fechar" }).last().click();
  await expect(dialog).toBeHidden();
});
