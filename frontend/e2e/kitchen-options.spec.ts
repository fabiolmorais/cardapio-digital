import { expect, test, type APIRequestContext, type Browser } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

// O celular da mesa (pela API), a estação de impressão e o painel: mais tempo.
test.describe.configure({ timeout: 90_000 });

/** Uma pizzaria com a mesa 04 e a Pizza: Broto e Grande, os sabores meio a meio e a borda. */
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
  const options = await (
    await request.put(`${API_URL}/api/v1/products/${pizza.id}/options`, {
      headers,
      data: {
        variants: [
          { name: "Broto", priceCents: 0, available: true, rodizioDefault: false },
          { name: "Grande", priceCents: 0, available: true, rodizioDefault: false },
        ],
        groups: [
          { groupId: flavors.id, limits: [] },
          { groupId: crust.id, limits: [] },
        ],
        prices: [],
      },
    })
  ).json();
  return {
    email,
    qr: table.publicToken as string,
    pizzaId: pizza.id as string,
    broto: options.variants[0].id as string,
    grande: options.variants[1].id as string,
    calabresa: flavors.items[0].id as string,
    frango: flavors.items[1].id as string,
    catupiry: crust.items[0].id as string,
  };
}

/**
 * A estação de impressão, num navegador separado, com a impressora trocada por um registro: cada
 * window.print() guarda o texto da comanda que estava na área de impressão.
 */
async function station(browser: Browser, email: string) {
  const context = await browser.newContext();
  const page = await context.newPage();
  await page.addInitScript(() => {
    const prints: string[] = [];
    Object.assign(window, { __prints: prints });
    window.print = () => {
      prints.push(document.querySelector("[data-print-ticket]")?.textContent ?? "");
    };
  });
  await loginThroughForm(page, email);
  await page.goto("/painel/impressao");
  await expect(page.getByRole("status").filter({ hasText: "Ao vivo" })).toBeVisible();
  const prints = () => page.evaluate(() => (window as unknown as { __prints: string[] }).__prints);
  return { page, prints, close: () => context.close() };
}

const HALF = "Grande · ½ Calabresa · ½ Frango com catupiry · Catupiry";

test("as escolhas do prato aparecem na cozinha, no Por item, na comanda e na comanda impressa", async ({
  page,
  request,
  browser,
}) => {
  const pz = await pizzeria(request);
  const printer = await station(browser, pz.email);
  try {
    // A mesa pede duas pizzas iguais (meio a meio, com borda) e um broto de calabresa.
    const phone = page.context().request;
    expect((await phone.post(`${API_URL}/api/v1/public/tables/${pz.qr}/session`)).ok()).toBe(true);
    await phone.post(`${API_URL}/api/v1/public/tables/${pz.qr}/presence`);
    const placed = await phone.post(`${API_URL}/api/v1/public/tables/${pz.qr}/orders`, {
      data: {
        items: [
          {
            productId: pz.pizzaId,
            quantity: 2,
            variantId: pz.grande,
            options: [
              { optionId: pz.calabresa, quantity: 1 },
              { optionId: pz.frango, quantity: 1 },
              { optionId: pz.catupiry, quantity: 1 },
            ],
          },
          {
            productId: pz.pizzaId,
            quantity: 1,
            variantId: pz.broto,
            options: [{ optionId: pz.calabresa, quantity: 1 }],
          },
        ],
      },
    });
    expect(placed.status()).toBe(201);

    // A comanda impressa: o tamanho e cada escolha numa linha.
    await expect.poll(async () => (await printer.prints()).filter((p) => p.includes("MESA 04"))).toHaveLength(1);
    const printed = (await printer.prints()).find((p) => p.includes("MESA 04"))!;
    for (const line of ["Grande", "½ Calabresa", "½ Frango com catupiry", "Catupiry", "Broto"]) {
      expect(printed).toContain(line);
    }
    await expect(printer.page.getByText(`(${HALF})`)).toBeVisible();

    // A cozinha, por pedido: as escolhas embaixo de cada item.
    await loginThroughForm(page, pz.email);
    await page.goto("/painel/cozinha");
    const ticket = page.getByRole("article", { name: "Mesa 04 · Pedido nº 1" });
    await expect(ticket.getByText(HALF)).toBeVisible();
    await expect(ticket.getByText("Broto · Calabresa", { exact: true })).toBeVisible();

    // Por item: escolhas diferentes não se somam no mesmo cartão.
    await page.getByRole("group", { name: "Visão" }).getByRole("button", { name: "Por item" }).click();
    await expect(page.getByRole("article", { name: `Pizza · ${HALF}: 2 no total` })).toBeVisible();
    await expect(page.getByRole("article", { name: "Pizza · Broto · Calabresa: 1 no total" })).toBeVisible();

    // A comanda no painel.
    await page.goto("/painel/comandas");
    await page.getByRole("link", { name: "Comanda da mesa 04" }).click();
    await expect(page.getByText(HALF)).toBeVisible();
    await expect(page.getByText("Broto · Calabresa", { exact: true })).toBeVisible();
  } finally {
    await printer.close();
  }
});
