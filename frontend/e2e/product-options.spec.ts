import { expect, test, type APIRequestContext } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

test.describe.configure({ timeout: 60_000 });

/** Um restaurante com a categoria Pizzas e o produto Pizza, criado pela API. */
async function pizzeria(request: APIRequestContext) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) =>
    (await request.post(`${API_URL}${path}`, { headers, data })).json();
  const category = await post("/api/v1/menu-categories", { name: "Pizzas" });
  const pizza = await post("/api/v1/products", {
    categoryId: category.id,
    name: "Pizza",
    priceCents: 3990,
    featured: false,
    available: true,
  });
  return { email, headers, pizzaId: pizza.id as string };
}

test("o dono cria um grupo pelo modelo da pizza e esgota uma opção", async ({ page, request }) => {
  const { email } = await pizzeria(request);
  await loginThroughForm(page, email);

  await page.goto("/admin/cardapio");
  await page.getByRole("link", { name: "Grupos de opções" }).click();
  await expect(page.getByRole("heading", { name: "Grupos de opções", level: 1 })).toBeVisible();
  await expect(page.getByText("Nenhum grupo ainda")).toBeVisible();

  await page.getByRole("button", { name: "Novo grupo" }).click();
  const dialog = page.getByRole("dialog", { name: "Novo grupo de opções" });
  await dialog.getByLabel("Começar de um modelo").click();
  await page.getByRole("option", { name: "Pizza: sabores meio a meio" }).click();
  await expect(dialog.getByLabel("Pergunta que o cliente vê")).toHaveValue("Escolha os sabores");
  await expect(dialog.getByRole("switch", { name: /Divide o item em partes/ })).toHaveAttribute("aria-checked", "true");
  await dialog.getByLabel("Preço da opção 1").fill("39,9");
  await dialog.getByLabel("Preço da opção 3").fill("44,90");
  await dialog.getByRole("button", { name: "Criar grupo" }).click();
  await expect(dialog).toBeHidden();

  const group = page.getByRole("listitem", { name: "Escolha os sabores" });
  await expect(group).toContainText("Obrigatório · escolha de 1 a 2 · Divide o item em partes · Cobra o maior preço");
  await expect(group).toContainText("Em nenhum produto");
  await expect(group).toContainText("R$ 39,90");

  await group.getByRole("switch", { name: "Calabresa disponível" }).click();
  await expect(group.getByRole("switch", { name: "Calabresa disponível" })).toHaveAttribute("aria-checked", "false");
  await page.reload();
  await expect(
    page.getByRole("listitem", { name: "Escolha os sabores" }).getByRole("switch", { name: "Calabresa disponível" }),
  ).toHaveAttribute("aria-checked", "false");
});

test("o produto ganha tamanhos, o grupo, o limite e o preço por tamanho, num Salvar só", async ({ page, request }) => {
  const { email, headers, pizzaId } = await pizzeria(request);
  const group = await (
    await request.post(`${API_URL}/api/v1/option-groups`, {
      headers,
      data: {
        title: "Escolha os sabores",
        minChoices: 1,
        maxChoices: 2,
        maxPerOption: 1,
        splitsItem: true,
        pricing: "HIGHEST",
        freeQuantity: 0,
        items: [
          { name: "Calabresa", priceCents: 3990, available: true },
          { name: "Frango", priceCents: 4490, available: true },
        ],
      },
    })
  ).json();
  await loginThroughForm(page, email);
  await page.goto(`/admin/cardapio/produtos/${pizzaId}`);

  await page.getByRole("button", { name: "Configurar tamanhos e opções" }).click();
  await page.getByRole("button", { name: "Tamanho", exact: true }).click();
  await page.getByLabel("Nome do tamanho 1").fill("Broto");
  await page.getByLabel("Preço do tamanho 1").fill("0");
  await page.getByRole("button", { name: "Tamanho", exact: true }).click();
  await page.getByLabel("Nome do tamanho 2").fill("Grande");
  await page.getByLabel("Preço do tamanho 2").fill("10");
  // Com tamanhos, o preço do produto trava no menor deles.
  await expect(page.getByLabel("Preço", { exact: true })).toBeDisabled();

  await page.getByLabel("Escolha um grupo").click();
  await page.getByRole("option", { name: "Escolha os sabores" }).click();
  await page.getByRole("button", { name: "Adicionar grupo" }).click();
  await page.getByLabel("Máximo no tamanho Broto").fill("1");
  await page.getByLabel("Preço de Calabresa no tamanho Grande").fill("54,90");
  await page.getByRole("button", { name: "Salvar" }).click();
  await expect(page).toHaveURL(/\/admin\/cardapio\?categoria=/);

  const saved = await (await request.get(`${API_URL}/api/v1/products/${pizzaId}/options`, { headers })).json();
  expect(saved.variants.map((v: { name: string }) => v.name)).toEqual(["Broto", "Grande"]);
  expect(saved.variants[0].rodizioDefault).toBe(true);
  expect(saved.groups[0].group.id).toBe(group.id);
  expect(saved.groups[0].limits).toEqual([{ variantId: saved.variants[0].id, minChoices: 0, maxChoices: 1 }]);
  expect(saved.prices).toEqual([{ optionId: group.items[0].id, variantId: saved.variants[1].id, priceCents: 5490 }]);
  const product = await (await request.get(`${API_URL}/api/v1/products/${pizzaId}`, { headers })).json();
  expect(product.priceCents).toBe(0);

  // De volta ao produto: tudo como foi salvo, e tirar o Grande o desativa.
  await page.goto(`/admin/cardapio/produtos/${pizzaId}`);
  await expect(page.getByLabel("Nome do tamanho 2")).toHaveValue("Grande");
  await expect(page.getByLabel("Preço de Calabresa no tamanho Grande")).toHaveValue("54,90");
  await page.getByRole("button", { name: "Tirar o tamanho Grande" }).click();
  await page.getByRole("button", { name: "Salvar" }).click();
  await expect(page).toHaveURL(/\/admin\/cardapio\?categoria=/);
  const after = await (await request.get(`${API_URL}/api/v1/products/${pizzaId}/options`, { headers })).json();
  expect(after.variants.map((v: { name: string }) => v.name)).toEqual(["Broto"]);
  expect(after.prices).toEqual([]);
});

test("um produto sem opções não mostra nada diferente", async ({ page, request }) => {
  const { email, pizzaId } = await pizzeria(request);
  await loginThroughForm(page, email);
  await page.goto(`/admin/cardapio/produtos/${pizzaId}`);

  await expect(page.getByRole("button", { name: "Configurar tamanhos e opções" })).toBeVisible();
  await expect(page.getByLabel("Preço", { exact: true })).toBeEnabled();
  await expect(page.getByLabel("Nome do tamanho 1")).toHaveCount(0);
});
