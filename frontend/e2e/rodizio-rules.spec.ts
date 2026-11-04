import { expect, test, type APIRequestContext } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

test.describe.configure({ timeout: 90_000 });

/**
 * Um restaurante com o Guioza, o Sashimi (só do rodízio) e a Coca, e o Japonês com as regras
 * ligadas: 1 de cada item por pessoa, até 2 itens a caminho por pessoa, 15 minutos de rodízio
 * (assim o aviso do fim já aparece) e o aviso de desperdício. O rodízio já está ativo, para 1 pessoa.
 */
async function restaurant(request: APIRequestContext) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) => (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  const sector = await post("/api/v1/sectors", { name: "Salão principal" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  const category = await post("/api/v1/menu-categories", { name: "Japonês" });
  const product = (name: string, priceCents: number, aLaCarte = true) =>
    post("/api/v1/products", { categoryId: category.id, name, priceCents, featured: false, available: true, aLaCarte });
  const guioza = await product("Guioza", 2990);
  const sashimi = await product("Sashimi", 4990, false);
  await product("Coca-Cola", 790);
  const rodizio = await post("/api/v1/rodizios", {
    name: "Rodízio Japonês",
    type: "A_LA_MINUTE",
    rules: { maxPerItemPerRound: 1, maxPendingPerGuest: 2, timeLimitMinutes: 15, wasteNotice: "Sobras são cobradas à parte." },
    tiers: [{ name: "Adulto", defaultPriceCents: 12990, prices: [] }],
    productIds: [guioza.id, sashimi.id],
  });
  const activated = await request.post(`${API_URL}/api/v1/tables/${table.id}/rodizio`, {
    headers,
    data: { rodizioId: rodizio.id, guests: [{ tierName: "Adulto", quantity: 1 }] },
  });
  expect(activated.status()).toBe(201);
  return { qr: table.publicToken as string, email, sashimi: sashimi.id as string };
}

test("a aba Rodízio mostra as regras, e o + e o envio param nelas", async ({ page, request }) => {
  const { qr } = await restaurant(request);
  await page.goto(`/m/${qr}/cardapio`);

  const round = page.getByRole("region", { name: "Rodízio Japonês · 1 pessoa" });
  await expect(round).toContainText("Sobras são cobradas à parte.");
  await expect(round).toContainText("Até 1 de cada item por rodada");
  await expect(round.getByRole("status").first()).toContainText(/Faltam 1[45] minutos para o fim do rodízio/);
  // O Sashimi é só do rodízio: está na aba Rodízio, mas não no cardápio de sempre.
  await expect(round).toContainText("Sashimi");
  const carte = page.getByRole("region", { name: "Japonês", exact: true });
  await expect(carte).toContainText("Coca-Cola");
  await expect(carte).not.toContainText("Sashimi");

  // 1 de cada por pessoa: depois do primeiro Sashimi, o + para.
  await round.getByRole("button", { name: "Adicionar Sashimi à rodada" }).first().click();
  await expect(round.getByRole("button", { name: "Adicionar Sashimi à rodada" })).toBeDisabled();
  await round.getByRole("button", { name: "Adicionar Guioza à rodada" }).click();
  await expect(round).toContainText("A rodada chegou ao limite");
  await round.getByRole("button", { name: "Enviar rodada (2 itens)" }).click();
  await expect(round).toContainText("Rodada enviada para a cozinha.");

  // 2 itens a caminho para 1 pessoa: a próxima rodada espera a cozinha.
  await expect(round).toContainText("Espere chegar parte do que está a caminho");
  await expect(round.getByRole("button", { name: "Adicionar Guioza à rodada" })).toBeDisabled();
});

test("o dono marca o produto como só do rodízio, e o carrinho não o leva", async ({ page, request }) => {
  const { email, qr, sashimi } = await restaurant(request);

  // O carrinho recusa pela API, com a frase que a tela mostraria.
  const phone = await page.context().request.post(`${API_URL}/api/v1/public/tables/${qr}/session`);
  expect(phone.ok()).toBe(true);
  await page.context().request.post(`${API_URL}/api/v1/public/tables/${qr}/presence`);
  const cart = await page.context().request.post(`${API_URL}/api/v1/public/tables/${qr}/orders`, {
    data: { items: [{ productId: sashimi, quantity: 1 }] },
  });
  expect(cart.status()).toBe(409);
  expect((await cart.json()).detail).toBe("Sashimi é só do rodízio. Peça pela aba Rodízio.");

  // Na gestão: o selo na lista e o interruptor no produto.
  await loginThroughForm(page, email);
  await page.goto("/admin/cardapio");
  await expect(page.getByRole("link", { name: /Sashimi/ })).toContainText("Só no rodízio");
  await page.getByRole("link", { name: /Sashimi/ }).click();
  const toggle = page.getByRole("switch", { name: /Aparece no cardápio à la carte/ });
  await expect(toggle).toHaveAttribute("aria-checked", "false");
  await toggle.click();
  await page.getByRole("button", { name: "Salvar" }).click();
  await expect(page).toHaveURL(/\/admin\/cardapio\?categoria=/);
  await expect(page.getByRole("link", { name: /Sashimi/ })).not.toContainText("Só no rodízio");
});
