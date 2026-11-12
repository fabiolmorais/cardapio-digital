import { expect, test, type APIRequestContext, type Browser, type Page } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

// O celular da mesa e o painel do dono, em dois navegadores, com login: mais tempo.
test.describe.configure({ timeout: 90_000 });

/** Um restaurante com a mesa 04, o Wi-Fi do salão, o Guioza (do rodízio), a Coca (à parte) e o Japonês. */
async function restaurant(request: APIRequestContext) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) => (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  const sector = await post("/api/v1/sectors", { name: "Salão principal" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  const category = await post("/api/v1/menu-categories", { name: "Japonês" });
  const product = (name: string, priceCents: number) =>
    post("/api/v1/products", { categoryId: category.id, name, priceCents, featured: false, available: true });
  const guioza = await product("Guioza", 2990);
  await product("Coca-Cola", 790);
  // Só o preço padrão: o teste não depende do dia e da hora em que roda.
  const japones = await post("/api/v1/rodizios", {
    name: "Rodízio Japonês",
    type: "A_LA_MINUTE",
    description: "Peças à vontade",
    rules: {},
    tiers: [
      { name: "Adulto", defaultPriceCents: 12990, prices: [] },
      { name: "Criança", defaultPriceCents: 6490, prices: [] },
    ],
    productIds: [guioza.id],
  });
  return { qr: table.publicToken as string, email, post, guioza, tableId: table.id as string, japonesId: japones.id };
}

async function ownerPanel(browser: Browser, email: string) {
  const context = await browser.newContext();
  const page = await context.newPage();
  await loginThroughForm(page, email);
  return { page, close: () => context.close() };
}

test("a mesa pede o rodízio, o garçom ativa, a mesa pede rodadas e a conta soma as pessoas", async ({
  page,
  request,
  browser,
}) => {
  const { qr, email } = await restaurant(request);

  // O celular: "Quero o rodízio", os preços de agora e o pedido ao garçom.
  await page.goto(`/m/${qr}`);
  await page.getByRole("link", { name: /Quero o rodízio/ }).click();
  await expect(page.getByRole("heading", { name: "Escolha o rodízio" })).toBeVisible();
  await expect(page.getByText("R$ 129,90 adulto")).toBeVisible();
  await page.getByRole("button", { name: "Chamar garçom para ativar" }).click();
  await expect(page.getByText("Pedido enviado ao garçom")).toBeVisible();

  // O painel: o chamado na fila, e o diálogo "Ativar rodízio".
  const owner = await ownerPanel(browser, email);
  const call = owner.page.getByRole("listitem", { name: "Quero o rodízio · Mesa 04" });
  await call.getByRole("button", { name: "Ativar rodízio" }).click();
  const dialog = owner.page.getByRole("dialog");
  for (let i = 0; i < 3; i++) await dialog.getByRole("button", { name: "Mais Adulto" }).click();
  await dialog.getByRole("button", { name: "Mais Criança" }).click();
  await expect(dialog).toContainText("R$ 454,60");
  await dialog.getByRole("button", { name: "Ativar rodízio" }).click();
  await expect(dialog).toBeHidden();
  // O chamado termina na ativação.
  await expect(call).toHaveCount(0);

  // O celular fica sabendo pelo tempo real, e vai às rodadas.
  await expect(page.getByRole("heading", { name: "Rodízio ativado!" })).toBeVisible();
  await page.getByRole("link", { name: "Pedir rodada" }).click();
  const round = page.getByRole("region", { name: "Rodízio Japonês · 4 pessoas" });
  await expect(round).toContainText("Incluído no rodízio");
  // A Coca não é do rodízio: fica só no cardápio de sempre, com preço.
  await expect(round).not.toContainText("Coca-Cola");
  await round.getByRole("button", { name: "Adicionar Guioza à rodada" }).first().click();
  await round.getByRole("button", { name: "Adicionar Guioza à rodada" }).click();
  await round.getByRole("button", { name: "Enviar rodada (2 itens)" }).click();
  await expect(round.getByRole("status")).toHaveText("Rodada enviada para a cozinha.");
  await expect(round).toContainText("1 rodada pedida");

  // O Next guarda as telas visitadas escondidas na página: só vale o que está à vista.
  await page.getByRole("link", { name: "Pedidos" }).click();
  await expect(page.getByRole("heading", { name: "Meus pedidos" })).toBeVisible();
  const visible = page.locator("main").filter({ visible: true });
  await expect(visible.getByText("Rodízio Japonês · 4 pessoas")).toBeVisible();
  await expect(visible.getByRole("article", { name: "2× Guioza" })).toContainText("Rodízio");
  await expect(visible.getByText("R$ 454,60").first()).toBeVisible();

  // A comanda no painel: o rodízio, o item a zero e o ajuste das pessoas.
  await owner.page.goto("/painel/comandas");
  await owner.page.getByRole("link", { name: "Comanda da mesa 04" }).click();
  const rodizio = owner.page.getByRole("region", { name: "Rodízio Japonês · 4 pessoas" });
  await expect(rodizio).toContainText("3× Adulto · R$ 129,90");
  await expect(owner.page.getByRole("region", { name: "Itens" })).toContainText("2× Guioza");
  await rodizio.getByRole("button", { name: "Ajustar pessoas" }).click();
  await owner.page.getByRole("dialog").getByRole("button", { name: "Menos Adulto" }).click();
  await owner.page.getByRole("dialog").getByRole("button", { name: "Salvar" }).click();
  await expect(owner.page.getByRole("region", { name: "Rodízio Japonês · 3 pessoas" })).toBeVisible();
  await expect(owner.page.getByText("R$ 324,70").first()).toBeVisible();
  await owner.close();
});

test("com mais de um rodízio, a tela inicial mostra um banner para cada um", async ({ page, request }) => {
  const { qr, post, guioza } = await restaurant(request);
  await post("/api/v1/rodizios", {
    name: "Rodízio de Petiscos",
    type: "A_LA_MINUTE",
    description: "Petiscos à vontade",
    rules: {},
    tiers: [{ name: "Adulto", defaultPriceCents: 8990, prices: [] }],
    productIds: [guioza.id],
  });

  await page.goto(`/m/${qr}`);
  const banners = page.getByRole("list", { name: "2 rodízios" });
  await expect(banners.getByRole("article")).toHaveCount(2);
  await expect(banners.getByRole("article", { name: "Rodízio Japonês" })).toContainText("R$ 129,90");
  await expect(banners.getByRole("article", { name: "Rodízio de Petiscos" })).toContainText("R$ 89,90");
  await banners.getByRole("link", { name: "Quero o rodízio · Rodízio de Petiscos" }).click();
  await expect(page).toHaveURL(new RegExp(`/m/${qr}/rodizio$`));
});

test("quem chega depois da ativação pergunta se participa, e entra quando o garçom conta", async ({
  page,
  request,
  browser,
}) => {
  const { qr, email, post, tableId, japonesId } = await restaurant(request);
  // A Ana já está na mesa quando o garçom ativa o rodízio para 2 pessoas.
  await page.goto(`/m/${qr}`);
  await expect(page.getByText("No salão")).toBeVisible(); // a sessão da Ana já existe, com a presença
  await post(`/api/v1/tables/${tableId}/rodizio`, {
    rodizioId: japonesId,
    guests: [{ tierName: "Adulto", quantity: 2 }],
  });
  await page.reload();
  await expect(page.getByRole("link", { name: /Rodízio Japonês · 2 pessoas/ })).toBeVisible();

  // A Bia chega depois: o celular dela pergunta, em vez de já entrar no rodízio.
  const later = await browser.newContext();
  const bia = await later.newPage();
  const owner = await ownerPanel(browser, email);
  try {
    await bia.goto(`/m/${qr}`);
    const invite = bia.getByRole("region", { name: "Rodízio da mesa" });
    await expect(invite).toContainText("Esta mesa está no Rodízio Japonês · 2 pessoas");
    await bia.getByRole("link", { name: "Cardápio" }).click();
    await expect(bia.getByRole("link", { name: "Rodízio", exact: true })).toHaveCount(0);
    await bia.getByRole("region", { name: "Rodízio da mesa" }).getByRole("button", { name: "Sim, vou participar" }).click();
    await expect(bia.getByRole("status").filter({ hasText: "Garçom chamado" })).toBeVisible();

    // O garçom conta a Bia: o diálogo abre no "Ajustar pessoas", com o rodízio da comanda.
    const call = owner.page.getByRole("listitem", { name: "Quero o rodízio · Mesa 04" });
    await call.getByRole("button", { name: "Ativar rodízio" }).click();
    const dialog = owner.page.getByRole("dialog");
    await expect(dialog).toContainText("Ajustar pessoas");
    await expect(dialog).toContainText("Chegou alguém que quer participar");
    await dialog.getByRole("button", { name: "Mais Adulto" }).click();
    await dialog.getByRole("button", { name: "Salvar" }).click();
    await expect(dialog).toBeHidden();
    await expect(call).toHaveCount(0);

    // O celular da Bia entra no rodízio na hora, pelo tempo real.
    await expect(bia.getByRole("link", { name: "Rodízio", exact: true })).toBeVisible({ timeout: 5000 });
    await expect(bia.getByRole("region", { name: "Rodízio da mesa" })).toHaveCount(0);
  } finally {
    await later.close();
    await owner.close();
  }
});

test("o garçom recusa o pedido de quem chegou depois, e o celular pode pedir de novo", async ({
  page,
  request,
  browser,
}) => {
  const { qr, email, post, tableId, japonesId } = await restaurant(request);
  await page.goto(`/m/${qr}`);
  await expect(page.getByText("No salão")).toBeVisible();
  await post(`/api/v1/tables/${tableId}/rodizio`, {
    rodizioId: japonesId,
    guests: [{ tierName: "Adulto", quantity: 2 }],
  });

  const later = await browser.newContext();
  const bia = await later.newPage();
  const owner = await ownerPanel(browser, email);
  try {
    await bia.goto(`/m/${qr}`);
    const invite = bia.getByRole("region", { name: "Rodízio da mesa" });
    await invite.getByRole("button", { name: "Não, vou pedir à parte" }).click();
    await bia.getByRole("button", { name: /Quero participar/ }).click();
    await expect(bia.getByRole("status").filter({ hasText: "Garçom chamado" })).toBeVisible();

    // No painel, o "Quero o rodízio" se resolve ativando ou recusando: não há "Aceitar" nem "Concluir".
    const call = owner.page.getByRole("listitem", { name: "Quero o rodízio · Mesa 04" });
    await expect(call.getByRole("button", { name: "Ativar rodízio" })).toBeVisible();
    await expect(call.getByRole("button", { name: "Aceitar" })).toHaveCount(0);
    await call.getByRole("button", { name: "Recusar" }).click();
    await expect(call).toHaveCount(0);

    // O celular volta a mostrar o atalho para pedir de novo, na hora.
    await expect(bia.getByRole("button", { name: /Quero participar/ })).toBeVisible({ timeout: 5000 });
  } finally {
    await later.close();
    await owner.close();
  }
});

/** Um banner 1200 × 600 gerado no próprio navegador (canvas), sem arquivo no repositório. */
async function bannerImage(page: Page) {
  const base64 = await page.evaluate(async () => {
    const canvas = document.createElement("canvas");
    canvas.width = 1200;
    canvas.height = 600;
    const context = canvas.getContext("2d")!;
    context.fillStyle = "#7c2d12";
    context.fillRect(0, 0, 1200, 600);
    const blob = await new Promise<Blob>((resolve) => canvas.toBlob((b) => resolve(b!), "image/jpeg", 0.9));
    const bytes = new Uint8Array(await blob.arrayBuffer());
    let text = "";
    for (const byte of bytes) text += String.fromCharCode(byte);
    return btoa(text);
  });
  return { name: "banner.jpg", mimeType: "image/jpeg", buffer: Buffer.from(base64, "base64") };
}

test("o dono envia o banner do rodízio, e a mesa vê o banner em \u201cEscolha o rodízio\u201d", async ({
  page,
  request,
  browser,
}) => {
  const { qr, email } = await restaurant(request);
  const owner = await ownerPanel(browser, email);
  try {
    await owner.page.goto("/admin/rodizios");
    const editor = owner.page.getByRole("region", { name: "Rodízio Japonês" });
    await expect(editor).toContainText("Tamanho ideal: 1200 × 600 pixels");
    await expect(editor).toContainText("Sem banner: a mesa mostra automaticamente a foto de um prato");
    await editor.getByLabel("Enviar banner").setInputFiles(await bannerImage(owner.page));
    await expect(editor.getByRole("status").filter({ hasText: "Banner atualizado." })).toBeVisible({ timeout: 15_000 });
    await expect(editor.getByRole("img", { name: "Banner do Rodízio Japonês" })).toBeVisible();
  } finally {
    await owner.close();
  }

  await page.goto(`/m/${qr}/rodizio`);
  const card = page.getByRole("listitem").filter({ hasText: "Rodízio Japonês" });
  await expect(card).toContainText("R$ 129,90 adulto");
  await expect(card.locator("img")).toHaveAttribute("src", /\/rodizios\/.+\/banner-.+\.jpg$/);
});
