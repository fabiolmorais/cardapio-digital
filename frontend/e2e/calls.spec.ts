import { expect, test, type APIRequestContext, type Browser } from "@playwright/test";
import { API_URL, apiLogin, createAccount, linkDeviceByCode, loginThroughForm } from "./support/account";

// Cada teste usa dois navegadores (o celular da mesa e o painel do dono), com login: mais tempo.
test.describe.configure({ timeout: 60_000 });

/** Um restaurante com a mesa 04 no "Salão principal". Devolve o QR e o e-mail do dono. */
async function restaurant(request: APIRequestContext) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) => (await request.post(`${API_URL}${path}`, { headers, data })).json();
  // Trava de presença: o navegador do teste chega pela rede local, que vira "o Wi-Fi do salão".
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
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
    // A mesa vê na hora, pelo tempo real, e só o primeiro nome de quem vem.
    await expect(page.getByRole("status")).toContainText("Garçom a caminho", { timeout: 3000 });
    await expect(page.getByRole("status")).toContainText("Marina aceitou seu chamado.");
    await expect(page.getByRole("status")).not.toContainText("Tanaka");

    await call.getByRole("button", { name: "Concluir" }).click();
    await expect(panel.page.getByText("Nenhum chamado aberto")).toBeVisible();
    await expect(page.getByRole("status")).toHaveCount(0, { timeout: 3000 });
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

test("o painel liga o som e mostra que está ao vivo", async ({ request, browser }) => {
  const { email } = await restaurant(request);
  const panel = await ownerPanel(browser, email);
  try {
    await expect(panel.page.getByRole("status").filter({ hasText: "Ao vivo" })).toBeVisible();
    await panel.page.getByRole("button", { name: "Ativar som" }).click();
    await expect(panel.page.getByText("Som ligado")).toBeVisible();
  } finally {
    await panel.close();
  }
});

test("o Modo TV mostra o chamado novo em tela cheia, na hora", async ({ page, request, browser }) => {
  const { qr, email } = await restaurant(request);
  const panel = await ownerPanel(browser, email);
  try {
    await panel.page.getByRole("link", { name: "Abrir Modo TV" }).click();
    await panel.page.getByRole("button", { name: "Ativar som e começar" }).click();
    await expect(panel.page.getByText("Nenhum chamado aberto.", { exact: true })).toBeVisible();
    await expect(panel.page.getByRole("status").filter({ hasText: "Ao vivo" })).toBeVisible();

    // A mesa chama; a TV não recarrega nada: o aviso chega pelo tempo real.
    await page.goto(`/m/${qr}`);
    await page.getByRole("button", { name: /Chamar garçom/ }).click();

    const alert = panel.page.getByRole("alert").filter({ hasText: "Novo chamado" });
    await expect(alert).toContainText("Chamar garçom");
    await expect(alert).toContainText("04");
    await expect(alert).toBeHidden({ timeout: 12_000 });
    await expect(panel.page.getByRole("heading", { name: "Chamados" })).toBeVisible();
  } finally {
    await panel.close();
  }
});

test("a TV vinculada abre o Modo TV sozinha, sem login nem PIN", async ({ page, request, browser }) => {
  const { qr, email } = await restaurant(request);
  const token = await apiLogin(request, email);
  const context = await browser.newContext({ viewport: { width: 1280, height: 720 }, locale: "pt-BR" });
  const tv = await context.newPage();
  try {
    await tv.goto("/aparelho");
    const code = await tv.getByTestId("pairing-code").getAttribute("data-code");
    await linkDeviceByCode(request, token, code!, "TV da cozinha", "TV");

    await tv.getByRole("button", { name: "Ativar som e começar" }).click({ timeout: 10_000 });
    await expect(tv.getByText("Nenhum chamado aberto.", { exact: true })).toBeVisible();
    await expect(tv.getByRole("status").filter({ hasText: "Ao vivo" })).toBeVisible();
    // Só leitura: nada de sair para o painel nem de aceitar.
    await expect(tv.getByRole("link", { name: "Sair do Modo TV" })).toHaveCount(0);

    await page.goto(`/m/${qr}`);
    await page.getByRole("button", { name: /Chamar garçom/ }).click();

    await expect(tv.getByRole("alert").filter({ hasText: "Novo chamado" })).toContainText("Chamar garçom");
    await expect(tv.getByRole("button", { name: "Aceitar" })).toHaveCount(0);
  } finally {
    await context.close();
  }
});

test("a equipe bloqueia o celular de um trote, e ele vê o aviso na hora", async ({ page, request, browser }) => {
  const { qr, email } = await restaurant(request);
  await page.goto(`/m/${qr}`);
  await page.getByRole("button", { name: /Chamar garçom/ }).click();
  await expect(page.getByRole("status")).toContainText("Chamado enviado");

  const panel = await ownerPanel(browser, email);
  try {
    const call = panel.page.getByRole("listitem", { name: "Chamar garçom · Mesa 04" });
    await call.getByRole("button", { name: "Bloquear este celular" }).click();
    await panel.page.getByRole("alertdialog").getByRole("button", { name: "Bloquear" }).click();
    await expect(panel.page.getByText("Nenhum chamado aberto")).toBeVisible();

    // O celular fica sabendo pelo tempo real: os botões somem e aparece o aviso.
    await expect(page.getByRole("alert").filter({ hasText: "Chamados bloqueados neste celular" })).toBeVisible({
      timeout: 5000,
    });
    await expect(page.getByRole("button", { name: /Chamar garçom/ })).toHaveCount(0);
  } finally {
    await panel.close();
  }
});
