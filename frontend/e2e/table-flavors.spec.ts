import { expect, test, type APIRequestContext, type Page } from "@playwright/test";
import { API_URL, apiLogin, createAccount } from "./support/account";

test.describe.configure({ timeout: 60_000 });

/** Uma foto JPEG gerada no navegador (canvas): nenhum arquivo de imagem no repositório. */
async function jpeg(page: Page) {
  const base64 = await page.evaluate(async () => {
    const canvas = document.createElement("canvas");
    canvas.width = 480;
    canvas.height = 480;
    const context = canvas.getContext("2d")!;
    context.fillStyle = "#b8361a";
    context.fillRect(0, 0, 480, 480);
    const blob = await new Promise<Blob>((resolve) => canvas.toBlob((b) => resolve(b!), "image/jpeg", 0.9));
    const bytes = new Uint8Array(await blob.arrayBuffer());
    return btoa(String.fromCharCode(...bytes));
  });
  return Buffer.from(base64, "base64");
}

/**
 * A pizzaria com um produto por tamanho (complemento da ADR 0010): a Pequena e a Grande, com a
 * mesma biblioteca de sabores; a Calabresa tem descrição e foto, e cada sabor tem o preço da Pequena.
 */
async function pizzeria(page: Page, request: APIRequestContext) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) =>
    (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  const sector = await post("/api/v1/sectors", { name: "Salão" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  const category = await post("/api/v1/menu-categories", { name: "Escolha suas pizzas" });
  const flavors = await post("/api/v1/option-groups", {
    title: "Escolha os sabores",
    minChoices: 1,
    maxChoices: 2,
    maxPerOption: 1,
    splitsItem: true,
    pricing: "HIGHEST",
    freeQuantity: 0,
    items: [
      {
        name: "Calabresa",
        description: "Mussarela, calabresa, cebola e orégano",
        priceCents: 3900,
        available: true,
      },
      { name: "Frango com catupiry", priceCents: 4500, available: true },
      { name: "Mussarela", priceCents: 3900, available: true },
    ],
  });
  const [calabresa, frango, mussarela] = flavors.items.map((i: { id: string }) => i.id);
  const product = async (name: string, prices: [string, number][]) => {
    const created = await post("/api/v1/products", {
      categoryId: category.id,
      name,
      priceCents: 0,
      featured: false,
      available: true,
    });
    const saved = await request.put(`${API_URL}/api/v1/products/${created.id}/options`, {
      headers,
      data: {
        variants: [],
        groups: [{ groupId: flavors.id, limits: [] }],
        prices: prices.map(([optionId, priceCents]) => ({ optionId, priceCents })),
      },
    });
    expect(saved.status()).toBe(200);
  };
  await product("Pequena", [
    [calabresa, 2900],
    [frango, 3400],
    [mussarela, 2900],
  ]);
  await product("Grande", []);

  // A foto da Calabresa, pelo caminho do navegador: URL assinada, envio direto e o aviso ao back.
  const photo = await jpeg(page);
  const upload = await post(`/api/v1/option-items/${calabresa}/media/uploads`, {
    contentType: "image/jpeg",
    sizeBytes: photo.length,
  });
  const sent = await request.put(upload.url, { headers: upload.headers, data: photo });
  expect(sent.ok()).toBe(true);
  const added = await request.post(`${API_URL}/api/v1/option-items/${calabresa}/media`, {
    headers,
    data: { uploadId: upload.uploadId },
  });
  expect(added.status()).toBe(201);
  return { headers, qr: table.publicToken as string };
}

async function openPequena(page: Page, qr: string) {
  await page.goto(`/m/${qr}/cardapio`);
  await page.getByRole("button", { name: "Adicionar Pequena" }).click();
  return page.getByRole("dialog", { name: "Pequena" });
}

test("a escolha vira um cardápio: a foto e a descrição do sabor, a busca e o preço neste tamanho", async ({
  page,
  request,
}) => {
  const { headers, qr } = await pizzeria(page, request);
  await page.goto(`/m/${qr}/cardapio`);
  await expect(page.getByText("R$ 29,00").first()).toBeVisible();

  const pizza = await openPequena(page, qr);
  await expect(pizza.getByRole("button", { name: "Ver fotos e vídeos de Calabresa" })).toBeVisible();
  await expect(pizza.getByText("Mussarela, calabresa, cebola e orégano")).toBeVisible();
  // O preço do sabor é o da Pequena.
  await expect(pizza.getByRole("button", { name: /Frango com catupiry/ })).toContainText("R$ 34,00");

  // A busca, sem acento nem maiúscula.
  await pizza.getByPlaceholder("Pesquise pelo nome").fill("FRANGO");
  await expect(pizza.getByRole("button", { name: /Calabresa/ })).toHaveCount(0);
  await pizza.getByRole("button", { name: /Frango com catupiry/ }).click();
  await pizza.getByPlaceholder("Pesquise pelo nome").fill("xyz");
  await expect(pizza.getByText("Nada com esse nome.")).toBeVisible();
  await pizza.getByPlaceholder("Pesquise pelo nome").fill("");

  await pizza.getByRole("button", { name: /^Calabresa/ }).click();
  await expect(pizza.getByRole("button", { name: /^Adicionar/ })).toContainText("R$ 34,00");
  await pizza.getByRole("button", { name: /^Adicionar/ }).click();
  await expect(pizza).toBeHidden();

  await page.getByRole("link", { name: /Ver pedido/ }).click();
  await expect(page.getByText("½ Calabresa · ½ Frango com catupiry")).toBeVisible();
  await page.getByRole("button", { name: "Enviar pedido para a cozinha" }).click();
  await expect(page.getByText("Pedido enviado!")).toBeVisible();
  const open = await (await request.get(`${API_URL}/api/v1/tabs/open`, { headers })).json();
  expect(open.content[0].totalCents).toBe(3400);
});

test("tocar na foto do sabor abre o Reels dos sabores, e o Escolher marca na escolha", async ({ page, request }) => {
  const { qr } = await pizzeria(page, request);
  const pizza = await openPequena(page, qr);

  await pizza.getByRole("button", { name: "Ver fotos e vídeos de Calabresa" }).click();
  const reels = page.getByRole("region", { name: "Fotos e vídeos: Escolha os sabores" });
  await expect(reels).toBeVisible();
  await expect(reels.getByRole("heading", { name: "Calabresa" })).toBeVisible();
  await expect(reels.getByText("R$ 29,00")).toBeVisible();
  await reels.getByRole("button", { name: "Escolher Calabresa" }).click();
  await expect(reels.getByRole("button", { name: "Tirar Calabresa" })).toHaveAttribute("aria-pressed", "true");

  // O Esc fecha só o Reels: a escolha continua aberta, com a Calabresa marcada.
  await page.keyboard.press("Escape");
  await expect(reels).toBeHidden();
  await expect(pizza).toBeVisible();
  await expect(pizza.getByRole("button", { name: /^Calabresa/ })).toHaveAttribute("aria-pressed", "true");
  await expect(pizza.getByRole("button", { name: /^Adicionar/ })).toContainText("R$ 29,00");
});
