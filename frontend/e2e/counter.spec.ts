import { expect, test, type APIRequestContext } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm, openMenu } from "./support/account";

test.describe.configure({ timeout: 60_000 });

/** Um bar com a mesa 04 e o Chopp. O lançamento pela equipe fica desligado: no balcão ele vale sempre. */
async function bar(request: APIRequestContext) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) =>
    (await request.post(`${API_URL}${path}`, { headers, data })).json();
  const sector = await post("/api/v1/sectors", { name: "Salão" });
  await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  const category = await post("/api/v1/menu-categories", { name: "Bar" });
  await post("/api/v1/products", {
    categoryId: category.id,
    name: "Chopp",
    priceCents: 1200,
    featured: false,
    available: true,
  });
  return { email };
}

test("o balcão: comanda com o nome do cliente, pedido da equipe, cozinha e ida para a mesa", async ({
  page,
  request,
}) => {
  const { email } = await bar(request);
  await loginThroughForm(page, email);

  // O dono cadastra o balcão em Mesas e QR Codes: sem QR Code.
  await page.goto("/admin/mesas");
  await page.getByRole("button", { name: "Novo balcão" }).click();
  const form = page.getByRole("dialog", { name: "Novo balcão" });
  await form.getByLabel("Nome").fill("Balcão do bar");
  await form.getByRole("button", { name: "Criar balcão" }).click();
  await expect(form).toBeHidden();
  await expect(
    page
      .getByText(/sem QR Code/i)
      .filter({ visible: true })
      .first(),
  ).toBeVisible();

  // O item "Balcão" aparece no menu do painel quando há um balcão.
  await page.goto("/painel");
  await openMenu(page);
  await page.getByRole("navigation").getByRole("link", { name: "Balcão", exact: true }).click();
  await expect(page).toHaveURL(/\/painel\/balcao$/);
  const counter = page.getByRole("region", { name: "Balcão do bar" });
  await expect(counter).toContainText("Nenhuma comanda aberta neste balcão.");

  await counter.getByRole("button", { name: "Nova comanda no balcão" }).click();
  const open = page.getByRole("dialog", { name: "Nova comanda no balcão" });
  await open.getByLabel("Como chamar o cliente").fill("João");
  await open.getByLabel("Número do cartão (opcional)").fill("12");
  await open.getByRole("button", { name: "Abrir comanda" }).click();
  await expect(page).toHaveURL(/\/painel\/comandas\/[0-9a-f-]+$/);
  const tabUrl = page.url();

  // O pedido sai pela comanda, mesmo com o lançamento pela equipe desligado nas mesas.
  await page.getByRole("link", { name: "Fazer pedido" }).click();
  await expect(page.getByRole("heading", { name: "Balcão do bar · João" })).toBeVisible();
  await page.getByRole("button", { name: "Adicionar Chopp" }).click();
  await page.getByRole("button", { name: /^Revisar pedido/ }).click();
  await page.getByRole("button", { name: /^Enviar para a cozinha/ }).click();
  await expect(page.getByRole("status").filter({ hasText: "enviado para a cozinha" })).toBeVisible();

  // A cozinha vê o cliente do balcão, e não uma mesa.
  await page.goto("/painel/cozinha");
  const ticket = page.getByRole("region", { name: "Na fila" }).getByRole("article", {
    name: "Balcão do bar · João · Pedido nº 1",
  });
  await expect(ticket).toContainText("Cartão 12");
  await expect(ticket).toContainText("Chopp");

  await page.goto("/painel/balcao");
  await expect(page.getByRole("link", { name: "Comanda de João, cartão 12" })).toContainText(/R\$\s12,00/);

  // O cliente sentou: a comanda vai para a mesa 04 com o nome.
  await page.goto(tabUrl);
  await page.getByRole("button", { name: "Mudar de mesa" }).click();
  const move = page.getByRole("dialog", { name: "Mudar a comanda de João" });
  await move.getByRole("button", { name: "Mesa 04" }).click();
  await expect(move).toContainText("Mudar a comanda de João para a Mesa 04?");
  await move.getByRole("button", { name: "Mudar para a Mesa 04" }).click();
  await expect(move).toBeHidden();
  await expect(page.getByRole("link", { name: "Fazer pedido" })).toHaveCount(0);

  await page.goto("/painel/balcao");
  await expect(page.getByRole("region", { name: "Balcão do bar" })).toContainText(
    "Nenhuma comanda aberta neste balcão.",
  );
});
