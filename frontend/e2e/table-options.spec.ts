import { expect, test, type APIRequestContext, type Page } from "@playwright/test";
import { API_URL, apiLogin, createAccount } from "./support/account";

test.describe.configure({ timeout: 60_000 });

/**
 * Uma pizzaria com a mesa 04 no Wi-Fi do salão e a Pizza: Broto e Grande (preço do sabor), os sabores
 * meio a meio pelo maior preço (mais caros na grande; broto com 1 sabor) e a borda paga.
 */
async function pizzeria(request: APIRequestContext) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) =>
    (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  const sector = await post("/api/v1/sectors", { name: "Salão" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  const category = await post("/api/v1/menu-categories", { name: "Pizzas" });
  const pizza = await post("/api/v1/products", {
    categoryId: category.id,
    name: "Pizza",
    priceCents: 0,
    featured: false,
    available: true,
  });
  const flavors = await post("/api/v1/option-groups", {
    title: "Escolha os sabores",
    minChoices: 1,
    maxChoices: 2,
    maxPerOption: 1,
    splitsItem: true,
    pricing: "HIGHEST",
    freeQuantity: 0,
    items: [
      { name: "Calabresa", priceCents: 3990, available: true },
      { name: "Frango com catupiry", priceCents: 4490, available: true },
    ],
  });
  const crust = await post("/api/v1/option-groups", {
    title: "Borda",
    minChoices: 0,
    maxChoices: 1,
    maxPerOption: 1,
    splitsItem: false,
    pricing: "SUM",
    freeQuantity: 0,
    items: [{ name: "Catupiry", priceCents: 800, available: true }],
  });
  await request.put(`${API_URL}/api/v1/products/${pizza.id}/options`, {
    headers,
    data: {
      variants: [
        { name: "Broto", priceCents: 0, available: true, rodizioDefault: false },
        { name: "Grande", priceCents: 0, available: true, rodizioDefault: false },
      ],
      groups: [
        { groupId: flavors.id, limits: [{ variant: 0, minChoices: 1, maxChoices: 1 }] },
        { groupId: crust.id, limits: [] },
      ],
      prices: [
        { optionId: flavors.items[0].id, variant: 1, priceCents: 5490 },
        { optionId: flavors.items[1].id, variant: 1, priceCents: 5990 },
      ],
    },
  });
  return { headers, post, qr: table.publicToken as string, tableId: table.id as string, pizzaId: pizza.id as string };
}

async function openPizza(page: Page, qr: string) {
  await page.goto(`/m/${qr}/cardapio`);
  await page.getByRole("button", { name: "Adicionar Pizza" }).click();
  return page.getByRole("dialog", { name: "Pizza" });
}

test("a pizza meio a meio: o tamanho, os sabores e a borda, com o preço certo até Meus pedidos", async ({
  page,
  request,
}) => {
  const { qr, headers } = await pizzeria(request);
  await page.goto(`/m/${qr}/cardapio`);
  // No cardápio, o preço depende da escolha: o menor possível.
  await expect(page.getByText("a partir de").first()).toBeVisible();
  await expect(page.getByText("R$ 39,90").first()).toBeVisible();

  const pizza = await openPizza(page, qr);
  const add = pizza.getByRole("button", { name: /Escolha o tamanho/ });
  await expect(add).toBeDisabled();
  await pizza.getByRole("button", { name: /Grande/ }).click();
  await expect(pizza.getByRole("button", { name: /Falta: Escolha os sabores/ })).toBeDisabled();

  await pizza.getByRole("button", { name: /Calabresa/ }).click();
  await pizza.getByRole("button", { name: /Frango com catupiry/ }).click();
  // Grande: o sabor mais caro (59,90).
  await expect(pizza.getByRole("button", { name: /^Adicionar/ })).toContainText("R$ 59,90");
  await pizza.getByRole("button", { name: /Catupiry \+ R\$ 8,00/ }).click();
  await expect(pizza.getByRole("button", { name: /^Adicionar/ })).toContainText("R$ 67,90");
  await pizza.getByRole("button", { name: /^Adicionar/ }).click();
  await expect(pizza).toBeHidden();

  await page.getByRole("link", { name: /Ver pedido/ }).click();
  await expect(page.getByText("Grande · ½ Calabresa · ½ Frango com catupiry · Catupiry")).toBeVisible();
  await expect(page.getByText("R$ 67,90").first()).toBeVisible();
  await page.getByRole("button", { name: "Enviar pedido para a cozinha" }).click();
  await expect(page.getByText("Pedido enviado!")).toBeVisible();
  await expect(page.getByText("Grande · ½ Calabresa · ½ Frango com catupiry · Catupiry")).toBeVisible();

  await page.getByRole("link", { name: "Acompanhar pedido" }).click();
  await expect(page.getByText("Grande · ½ Calabresa · ½ Frango com catupiry · Catupiry").first()).toBeVisible();
  // E o back cobrou o mesmo.
  const open = await (await request.get(`${API_URL}/api/v1/tabs/open`, { headers })).json();
  expect(open.content[0].totalCents).toBe(6790);
});

test("o broto aceita um sabor só, e trocar de tamanho tira o que passou", async ({ page, request }) => {
  const { qr } = await pizzeria(request);
  const pizza = await openPizza(page, qr);

  await pizza.getByRole("button", { name: /Grande/ }).click();
  await pizza.getByRole("button", { name: /Calabresa/ }).click();
  await pizza.getByRole("button", { name: /Frango com catupiry/ }).click();
  await pizza.getByRole("button", { name: /Broto/ }).click();
  // Dois sabores não cabem no broto: as escolhas do grupo saem.
  await expect(pizza.getByRole("button", { name: /Calabresa/ })).toHaveAttribute("aria-pressed", "false");
  await pizza.getByRole("button", { name: /Calabresa/ }).click();
  // No broto, um sabor só: tocar no outro troca.
  await pizza.getByRole("button", { name: /Frango com catupiry/ }).click();
  await expect(pizza.getByRole("button", { name: /Calabresa/ })).toHaveAttribute("aria-pressed", "false");
  await expect(pizza.getByRole("button", { name: /^Adicionar/ })).toContainText("R$ 44,90");
});

test("no rodízio, a pizza abre para escolher no tamanho do rodízio, sem preço e sem a borda paga", async ({
  page,
  request,
}) => {
  const { headers, post, qr, tableId, pizzaId } = await pizzeria(request);
  const rodizio = await post("/api/v1/rodizios", {
    name: "Rodízio de pizza",
    type: "A_LA_MINUTE",
    rules: {},
    tiers: [{ name: "Adulto", defaultPriceCents: 6990, prices: [] }],
    productIds: [pizzaId],
  });
  await page.goto(`/m/${qr}`);
  await expect(page.getByText("No salão")).toBeVisible();
  const activated = await request.post(`${API_URL}/api/v1/tables/${tableId}/rodizio`, {
    headers,
    data: { rodizioId: rodizio.id, guests: [{ tierName: "Adulto", quantity: 2 }] },
  });
  expect(activated.status()).toBe(201);

  await page.goto(`/m/${qr}/cardapio#rodizio`);
  const round = page.locator("#rodizio");
  await round.getByRole("button", { name: /Pizza/ }).first().click();
  const pizza = page.getByRole("dialog", { name: "Pizza" });
  await expect(pizza.getByRole("button", { name: /Broto/ })).toHaveCount(0);
  await expect(pizza.getByRole("button", { name: /Catupiry/ })).toHaveCount(0);
  // O tamanho do rodízio é o Broto (o primeiro): um sabor só, e tocar no outro troca.
  await pizza.getByRole("button", { name: /Calabresa/ }).click();
  await pizza.getByRole("button", { name: /Frango com catupiry/ }).click();
  await expect(pizza.getByRole("button", { name: /Calabresa/ })).toHaveAttribute("aria-pressed", "false");
  await pizza.getByRole("button", { name: "Adicionar à rodada" }).click();
  await expect(pizza).toBeHidden();

  await expect(round.getByText("1× Broto · Frango com catupiry")).toBeVisible();
  await round.getByRole("button", { name: /Enviar rodada/ }).click();
  await expect(round.getByText("Rodada enviada para a cozinha.")).toBeVisible();
  const open = await (await request.get(`${API_URL}/api/v1/tabs/open`, { headers })).json();
  const tab = await (await request.get(`${API_URL}/api/v1/tabs/${open.content[0].id}`, { headers })).json();
  const item = tab.orders[0].items[0];
  expect(item.unitPriceCents).toBe(0);
  expect(item.choices.variantName).toBe("Broto");
  expect(item.choices.options.map((o: { name: string }) => o.name)).toEqual(["Frango com catupiry"]);
});
