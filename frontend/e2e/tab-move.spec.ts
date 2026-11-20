import { expect, test, type APIRequestContext } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

test.describe.configure({ timeout: 60_000 });

/** Um bar com as mesas 04, 05 e 06, o Chopp e o Guioza, e o garçom liberado para lançar pedidos. */
async function bar(request: APIRequestContext) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) =>
    (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  await request.put(`${API_URL}/api/v1/order-settings`, {
    headers,
    data: { orderingEnabled: true, approvalRequired: false, staffOrderingEnabled: true },
  });
  const sector = await post("/api/v1/sectors", { name: "Salão" });
  const table = (label: string) => post("/api/v1/tables", { label, sectorId: sector.id });
  const [t04, t05] = [await table("04"), await table("05")];
  await table("06");
  const category = await post("/api/v1/menu-categories", { name: "Bar" });
  const product = (name: string, priceCents: number) =>
    post("/api/v1/products", { categoryId: category.id, name, priceCents, featured: false, available: true });
  const chopp = await product("Chopp", 1200);
  const guioza = await product("Guioza", 2990);
  /** O garçom lança um item na mesa (pela API); devolve o id da comanda. */
  const order = async (tableId: string, productId: string) =>
    (
      await post(`/api/v1/tables/${tableId}/orders`, {
        clientRequestId: crypto.randomUUID(),
        round: false,
        items: [{ productId, quantity: 1 }],
      })
    ).tabId as string;
  return { email, t04, t05, chopp, guioza, order };
}

test("o garçom muda a comanda para uma mesa livre, e o celular vai junto", async ({ page, request, browser }) => {
  const { email, t04, t05, chopp, order } = await bar(request);
  await order(t04.id, chopp.id);

  const context = await browser.newContext({ locale: "pt-BR" });
  const phone = await context.newPage();
  try {
    await phone.goto(`/m/${t04.publicToken}`);
    await expect(phone.getByText("No salão")).toBeVisible();
    await phone.goto(`/m/${t04.publicToken}/pedidos`);
    await expect(phone.getByText("Chopp").first()).toBeVisible();

    await loginThroughForm(page, email);
    await page.goto("/painel/mesas");
    await page.getByRole("button", { name: /Mesa 04/ }).click();
    await page.getByRole("button", { name: "Mudar de mesa" }).click();
    const dialog = page.getByRole("dialog", { name: "Mudar a comanda da Mesa 04" });
    await dialog.getByRole("button", { name: "Mesa 05" }).click();
    await expect(dialog).toContainText("Mudar a comanda da Mesa 04 para a Mesa 05?");
    await dialog.getByRole("button", { name: "Mudar para a Mesa 05" }).click();
    await expect(dialog).toBeHidden();
    // A gaveta acompanha a comanda, agora na mesa 05.
    await expect(page.getByRole("complementary", { name: "Mesa 05" })).toContainText("Chopp");

    // O celular vai sozinho para a mesa nova, na mesma tela, e continua vendo o pedido.
    await expect(phone).toHaveURL(new RegExp(`/m/${t05.publicToken}/pedidos$`), { timeout: 10_000 });
    await expect(phone.getByText("Chopp").first()).toBeVisible();
  } finally {
    await context.close();
  }
});

test("para uma mesa com comanda, as duas se juntam na mesa de destino", async ({ page, request }) => {
  const { email, t04, t05, chopp, guioza, order } = await bar(request);
  const from = await order(t04.id, chopp.id);
  const into = await order(t05.id, guioza.id);

  await loginThroughForm(page, email);
  await page.goto(`/painel/comandas/${from}`);
  await page.getByRole("button", { name: "Mudar de mesa" }).click();
  const dialog = page.getByRole("dialog", { name: "Mudar a comanda da Mesa 04" });
  await dialog.getByRole("button", { name: /Mesa 05/ }).click();
  await expect(dialog).toContainText("Juntar a comanda da Mesa 04 (R$ 12,00) com a da Mesa 05 (R$ 29,90)?");
  await expect(dialog).toContainText("R$ 41,90");
  await dialog.getByRole("button", { name: "Juntar as comandas" }).click();

  // A tela vai para a comanda que continuou, com os dois pedidos e o registro da junção.
  await expect(page).toHaveURL(new RegExp(`/painel/comandas/${into}$`));
  await expect(page.getByText("1× Chopp", { exact: true })).toBeVisible();
  await expect(page.getByText("1× Guioza", { exact: true })).toBeVisible();
  await expect(page.getByLabel("Mudanças de mesa")).toContainText("Juntou a Mesa 04 com a Mesa 05");

  // A comanda de origem fica registrada como juntada, com o atalho para a outra.
  await page.goto(`/painel/comandas/${from}`);
  await expect(page.getByRole("heading", { name: "Comanda juntada" })).toBeVisible();
  await page.getByRole("link", { name: "Ver a comanda junta" }).click();
  await expect(page).toHaveURL(new RegExp(`/painel/comandas/${into}$`));
});
