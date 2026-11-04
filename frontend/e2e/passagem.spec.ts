import { expect, test, type APIRequestContext, type Browser } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

// O celular da mesa e o painel do dono, em dois navegadores, com login: mais tempo.
test.describe.configure({ timeout: 90_000 });

/** Uma churrascaria com a mesa 04, o Wi-Fi do salão e o Churrasco (rodízio de passagem). */
async function restaurant(request: APIRequestContext) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) => (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  const sector = await post("/api/v1/sectors", { name: "Salão principal" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  await post("/api/v1/rodizios", {
    name: "Churrasco",
    type: "PASSAGEM",
    description: "Cortes nobres à vontade.",
    rules: {},
    tiers: [{ name: "Adulto", defaultPriceCents: 15990, prices: [] }],
    productIds: [],
  });
  return { qr: table.publicToken as string, email };
}

async function ownerPanel(browser: Browser, email: string) {
  const context = await browser.newContext();
  const page = await context.newPage();
  await loginThroughForm(page, email);
  return { page, close: () => context.close() };
}

test("a mesa entra no passagem, vira o cartão, e os passadores veem a cor na hora", async ({
  page,
  request,
  browser,
}) => {
  const { qr, email } = await restaurant(request);

  // O celular pede o rodízio; o garçom ativa pelo chamado.
  await page.goto(`/m/${qr}`);
  await page.getByRole("link", { name: /Quero o rodízio/ }).click();
  await expect(page.getByText("Os passadores servem na mesa")).toBeVisible();
  await page.getByRole("button", { name: "Chamar garçom para ativar" }).click();
  await expect(page.getByText("Pedido enviado ao garçom")).toBeVisible();

  const owner = await ownerPanel(browser, email);
  await owner.page
    .getByRole("listitem", { name: "Quero o rodízio · Mesa 04" })
    .getByRole("button", { name: "Ativar rodízio" })
    .click();
  const dialog = owner.page.getByRole("dialog");
  await dialog.getByRole("button", { name: "Mais Adulto" }).click();
  await dialog.getByRole("button", { name: "Mais Adulto" }).click();
  await dialog.getByRole("button", { name: "Ativar rodízio" }).click();
  await expect(dialog).toBeHidden();

  // O celular vê o cartão verde.
  await expect(page.getByRole("heading", { name: "Rodízio ativado!" })).toBeVisible();
  await page.getByRole("link", { name: "Ver o cartão" }).click();
  const card = page.getByRole("region", { name: "Cartão do rodízio" });
  await expect(card.getByRole("status")).toContainText("Pode servir");

  // A tela dos passadores mostra a mesa, e acompanha cada virada.
  await owner.page.goto("/painel");
  await owner.page.getByRole("link", { name: /Abrir a tela dos passadores/ }).click();
  const mesa = owner.page.getByRole("listitem", { name: /^Mesa 04/ });
  await expect(mesa).toHaveAccessibleName("Mesa 04 · Pode servir");
  await expect(mesa).toContainText("2 pessoas · Churrasco");

  await card.getByRole("button", { name: "Pausar (cartão vermelho)" }).click();
  await expect(card.getByRole("status")).toContainText("Pausado");
  await expect(mesa).toHaveAccessibleName("Mesa 04 · Pausada");
  await expect(owner.page.getByText("1 mesa · 1 pausada")).toBeVisible();

  await card.getByRole("button", { name: "Voltar a ser servido" }).click();
  await expect(card.getByRole("status")).toContainText("Pode servir");
  await expect(mesa).toHaveAccessibleName("Mesa 04 · Pode servir");
  await owner.close();
});
