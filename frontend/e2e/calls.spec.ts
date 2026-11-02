import { expect, test, type APIRequestContext, type Browser } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

// Cada teste usa dois navegadores (o celular da mesa e o painel do dono), com login: mais tempo.
test.describe.configure({ timeout: 60_000 });

/** Um restaurante com a mesa 04 no "Salão principal". Devolve o QR e o e-mail do dono. */
async function restaurant(request: APIRequestContext) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) => (await request.post(`${API_URL}${path}`, { headers, data })).json();
  const sector = await post("/api/v1/sectors", { name: "Salão principal" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  return { qr: table.publicToken as string, email };
}

/** O painel do dono, num navegador separado do celular da mesa (cada um com seus cookies). */
async function ownerPanel(browser: Browser, email: string) {
  const context = await browser.newContext();
  const page = await context.newPage();
  await loginThroughForm(page, email);
  return { page, close: () => context.close() };
}

test("a mesa chama o garçom, o painel aceita e conclui, e a mesa vê cada passo", async ({ page, request, browser }) => {
  const { qr, email } = await restaurant(request);
  await page.goto(`/m/${qr}`);

  await page.getByRole("button", { name: /Chamar garçom/ }).click();
  await expect(page.getByRole("status")).toContainText("Chamado enviado");
  await expect(page.getByRole("button", { name: /Chamar garçom/ })).toHaveAttribute("aria-pressed", "true");
  // Tocar de novo não duplica o chamado.
  await page.getByRole("button", { name: /Chamar garçom/ }).click();

  const panel = await ownerPanel(browser, email);
  try {
    const call = panel.page.getByRole("listitem", { name: "Chamar garçom · Mesa 04" });
    await expect(call).toContainText("Salão principal");
    await expect(panel.page.getByText("1 aberto")).toBeVisible();

    await call.getByRole("button", { name: "Aceitar" }).click();
    await expect(call).toContainText("Em atendimento · Marina Tanaka");
    // A mesa vê só o primeiro nome de quem vem (atualiza sozinha em alguns segundos).
    await expect(page.getByRole("status")).toContainText("Garçom a caminho", { timeout: 15_000 });
    await expect(page.getByRole("status")).toContainText("Marina aceitou seu chamado.");
    await expect(page.getByRole("status")).not.toContainText("Tanaka");

    await call.getByRole("button", { name: "Concluir" }).click();
    await expect(panel.page.getByText("Nenhum chamado aberto")).toBeVisible();
    await expect(page.getByRole("status")).toHaveCount(0, { timeout: 15_000 });
  } finally {
    await panel.close();
  }
});

test("a mesa cancela o chamado e ele sai do painel", async ({ page, request, browser }) => {
  const { qr, email } = await restaurant(request);
  await page.goto(`/m/${qr}`);
  await page.getByRole("button", { name: /Chamar garçom/ }).click();
  await page.getByRole("button", { name: "Cancelar chamado" }).click();
  await expect(page.getByRole("status")).toHaveCount(0);
  await expect(page.getByRole("button", { name: /Chamar garçom/ })).toHaveAttribute("aria-pressed", "false");

  const panel = await ownerPanel(browser, email);
  try {
    await expect(panel.page.getByText("Nenhum chamado aberto")).toBeVisible();
  } finally {
    await panel.close();
  }
});

test("a mesa pede a conta, troca a forma de pagamento e o painel mostra a escolha", async ({ page, request, browser }) => {
  const { qr, email } = await restaurant(request);
  await page.goto(`/m/${qr}`);
  await page.getByRole("link", { name: /Pedir a conta/ }).click();
  await expect(page).toHaveURL(`/m/${qr}/conta`);

  await page.getByRole("radio", { name: /Cartão/ }).check();
  await page.getByRole("button", { name: "Pedir a conta" }).click();
  await expect(page.getByRole("heading", { name: "Conta solicitada" })).toBeVisible();
  await expect(page.getByText("vai até a mesa com a maquininha")).toBeVisible();

  await page.getByRole("button", { name: "Trocar forma de pagamento" }).click();
  await expect(page.getByRole("radio", { name: /Cartão/ })).toBeChecked();
  await page.getByRole("radio", { name: /Dinheiro/ }).check();
  await page.getByRole("button", { name: "Trocar para esta forma" }).click();
  await expect(page.getByText("Avise se precisar de troco.")).toBeVisible();

  const panel = await ownerPanel(browser, email);
  try {
    const call = panel.page.getByRole("listitem", { name: "Pedir a conta · Mesa 04" });
    await expect(call).toContainText("Dinheiro · Salão principal");
    // Um pedido de conta só: trocar a forma de pagamento não criou outro.
    await expect(panel.page.getByText("1 aberto")).toBeVisible();
  } finally {
    await panel.close();
  }

  // De volta à mesa (pela aba de baixo: em desenvolvimento, o botão do Next cobre a seta do topo),
  // o botão mostra que a conta já foi pedida.
  await expect(page.getByRole("link", { name: "Voltar para a mesa" })).toHaveAttribute("href", `/m/${qr}`);
  await page.getByRole("link", { name: "Mesa", exact: true }).click();
  await expect(page.getByRole("link", { name: /Pedir a conta/ })).toContainText("Conta solicitada");
});
