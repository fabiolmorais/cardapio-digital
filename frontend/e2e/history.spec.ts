import { expect, test, type APIRequestContext, type Browser } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

test.describe.configure({ timeout: 60_000 });

/**
 * Um restaurante com as mesas 04 e 05, cada uma com uma visita inteira hoje: um celular pede, o
 * dono entrega e fecha a comanda (tudo pela API).
 */
async function restaurantWithClosedTabs(request: APIRequestContext, browser: Browser) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) => (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  const sector = await post("/api/v1/sectors", { name: "Salão principal" });
  const entradas = await post("/api/v1/menu-categories", { name: "Entradas" });
  const guioza = await post("/api/v1/products", {
    categoryId: entradas.id,
    name: "Guioza",
    priceCents: 2990,
    featured: false,
    available: true,
  });

  for (const [label, quantity, paymentMethod] of [
    ["04", 2, "CARD"],
    ["05", 1, "PIX"],
  ] as const) {
    const table = await post("/api/v1/tables", { label, sectorId: sector.id });
    // O celular da mesa: um contexto à parte, com o cookie da sessão dele.
    const phone = await browser.newContext();
    const tableUrl = `${API_URL}/api/v1/public/tables/${table.publicToken}`;
    await phone.request.post(`${tableUrl}/session`);
    await phone.request.post(`${tableUrl}/presence`);
    const order = await (
      await phone.request.post(`${tableUrl}/orders`, { data: { items: [{ productId: guioza.id, quantity }] } })
    ).json();
    await phone.close();
    for (const item of order.items) await request.post(`${API_URL}/api/v1/order-items/${item.id}/deliver`, { headers });
    const open = await (await request.get(`${API_URL}/api/v1/tabs/open`, { headers })).json();
    const tab = open.content.find((t: { tableLabel: string }) => t.tableLabel === label);
    expect((await request.post(`${API_URL}/api/v1/tabs/${tab.id}/close`, { headers, data: { paymentMethod } })).ok()).toBe(
      true,
    );
  }
  return { email };
}

test("o dono vê as comandas fechadas de hoje, filtra por mesa e abre uma", async ({ page, request, browser }) => {
  const { email } = await restaurantWithClosedTabs(request, browser);
  await loginThroughForm(page, email);
  // As seções do painel carregam aos poucos e empurram o resto para baixo: espera elas assentarem.
  await expect(page.getByText("Nenhuma comanda aberta.")).toBeVisible();
  await page.getByRole("link", { name: /Comandas/ }).click();
  await expect(page).toHaveURL(/\/admin\/comandas$/);

  await expect(page.getByText("2 comandas fechadas")).toBeVisible();
  await expect(page.getByRole("link", { name: /Comanda da mesa 04/ })).toContainText("R$ 59,80");

  await page.getByLabel("Mesa", { exact: true }).selectOption({ label: "Mesa 05" });
  await expect(page.getByText("1 comanda fechada")).toBeVisible();
  await page.getByRole("link", { name: /Comanda da mesa 05/ }).click();

  await expect(page.getByRole("heading", { name: "Mesa 05" })).toBeVisible();
  await expect(page.getByText("1× Guioza")).toBeVisible();
  await expect(page.getByText("Total cobrado")).toBeVisible();
  await expect(page.getByRole("definition").filter({ hasText: "Pix" })).toBeVisible();

  await page.getByRole("link", { name: "Voltar ao histórico" }).click();
  await page.getByRole("button", { name: "Ontem" }).click();
  await expect(page.getByText("Nenhuma comanda fechada no período.")).toBeVisible();
});
