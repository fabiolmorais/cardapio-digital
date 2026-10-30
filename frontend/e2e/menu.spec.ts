import { expect, test, type APIRequestContext, type Page } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

/** Abre o cardápio pelo atalho do painel. */
async function openMenu(page: Page) {
  await page.getByRole("link", { name: /^Cardápio/ }).click();
  await expect(page.getByRole("heading", { name: "Cardápio", level: 1 })).toBeVisible();
}

async function newCategory(page: Page, name: string) {
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("Nome da categoria").fill(name);
  await dialog.getByRole("button", { name: "Criar categoria" }).click();
  await expect(dialog).toBeHidden();
}

async function newProduct(page: Page, name: string, price: string, promo = "") {
  await page.getByRole("link", { name: "Novo produto" }).click();
  await expect(page.getByRole("heading", { name: "Novo produto" })).toBeVisible();
  await field(page, "Nome").fill(name);
  await field(page, "Preço").fill(price);
  if (promo) await field(page, "Preço promocional (opcional)").fill(promo);
  await page.getByRole("button", { name: "Criar produto" }).click();
  await expect(page.getByRole("heading", { name: "Cardápio", level: 1 })).toBeVisible();
}

async function apiCategory(request: APIRequestContext, token: string, name: string) {
  const response = await request.post(`${API_URL}/api/v1/menu-categories`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { name },
  });
  expect(response.status()).toBe(201);
  return (await response.json()).id as string;
}

async function apiProduct(
  request: APIRequestContext,
  token: string,
  categoryId: string,
  name: string,
  priceCents: number,
  promoPriceCents?: number,
) {
  const response = await request.post(`${API_URL}/api/v1/products`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { categoryId, name, priceCents, promoPriceCents, featured: false, available: true },
  });
  expect(response.status()).toBe(201);
}

/**
 * Campo do formulário visível. O Next guarda escondidas na página as telas já visitadas
 * (outro "Novo produto", por exemplo), e o mesmo rótulo existe nelas também.
 */
function field(page: Page, label: string) {
  return page.getByLabel(label, { exact: true }).filter({ visible: true });
}

function categoryButtons(page: Page) {
  return page.getByRole("navigation", { name: "Categorias" }).getByRole("button", { pressed: true });
}

function productNames(page: Page) {
  return page.getByRole("table").getByRole("link");
}

test("dono cria categoria e produtos pelo formulário, com preço em reais e promoção", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await openMenu(page);

  await page.getByRole("button", { name: "Criar a primeira categoria" }).click();
  await newCategory(page, "Entradas");
  await expect(categoryButtons(page)).toHaveText(/Entradas/);

  // Preço com vírgula e só uma casa: vira R$ 27,90. A promoção aparece na linha.
  await newProduct(page, "Hot roll da casa", "27,9", "24,90");
  // O segundo "Novo produto" abre vazio (sem a promoção do primeiro).
  await newProduct(page, "Sunomono", "19.90");
  await expect(productNames(page)).toHaveText(["Hot roll da casa", "Sunomono"]);
  const hotRoll = page.getByRole("row", { name: /Hot roll da casa/ });
  await expect(hotRoll).toContainText("R$ 27,90");
  await expect(hotRoll).toContainText("Promoção R$ 24,90");

  // Promoção maior que o preço: o back recusa, e a frase dele aparece no formulário.
  await page.getByRole("link", { name: "Sunomono" }).click();
  await field(page, "Preço promocional (opcional)").fill("25,00");
  await page.getByRole("button", { name: "Salvar" }).click();
  await expect(
    page.getByRole("alert").filter({ hasText: "O preço promocional precisa ser menor que o preço normal" }),
  ).toBeVisible();
});

test("editar, esgotar e buscar produtos", async ({ page, request }) => {
  const { email } = await createAccount(request);
  const token = await apiLogin(request, email);
  const entradas = await apiCategory(request, token, "Entradas");
  await apiCategory(request, token, "Pratos frios");
  await apiProduct(request, token, entradas, "Hot roll da casa", 2790, 2490);
  await apiProduct(request, token, entradas, "Sunomono", 1990);
  await loginThroughForm(page, email);
  await openMenu(page);

  // Tira a promoção, marca destaque e muda de categoria.
  await page.getByRole("link", { name: "Hot roll da casa" }).click();
  await expect(field(page, "Preço promocional (opcional)")).toHaveValue("24,90");
  await field(page, "Preço promocional (opcional)").fill("");
  await page.getByRole("switch", { name: /Destaque/ }).click();
  await field(page, "Categoria").selectOption({ label: "Pratos frios" });
  await page.getByRole("button", { name: "Salvar" }).click();

  // Volta para a categoria do produto, sem a promoção e com o selo de destaque.
  await expect(categoryButtons(page)).toHaveText(/Pratos frios/);
  const moved = page.getByRole("row", { name: /Hot roll da casa/ });
  await expect(moved).toContainText("Destaque");
  await expect(moved).not.toContainText("Promoção");

  // Esgotado hoje pelo interruptor da lista.
  const toggle = page.getByRole("switch", { name: "Hot roll da casa disponível" });
  await toggle.click();
  await expect(toggle).toHaveAttribute("aria-checked", "false");

  // A busca procura em todas as categorias.
  await page.getByRole("searchbox", { name: "Buscar produto" }).fill("SUNO");
  await expect(page.getByRole("status")).toHaveText("1 produto encontrado em todas as categorias");
  await expect(productNames(page)).toHaveText(["Sunomono"]);
});

test("reordenar arrastando ou pelo teclado", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await openMenu(page);
  await page.getByRole("button", { name: "Criar a primeira categoria" }).click();
  await newCategory(page, "Entradas");
  for (const [name, price] of [["Guioza", "29,90"], ["Sunomono", "19,90"], ["Edamame", "19,90"]]) {
    await newProduct(page, name, price);
  }
  await expect(productNames(page)).toHaveText(["Guioza", "Sunomono", "Edamame"]);

  // Teclado: com a alça do Edamame selecionada, seta para cima o leva para o meio.
  await page.getByRole("button", { name: "Mover Edamame" }).press("ArrowUp");
  await expect(productNames(page)).toHaveText(["Guioza", "Edamame", "Sunomono"]);
  await expect(page.getByRole("table")).toHaveAttribute("aria-busy", "false"); // terminou de salvar

  // Arrastar: a alça do Sunomono até acima do Guioza.
  const handle = await page.getByRole("button", { name: "Mover Sunomono" }).boundingBox();
  const target = await page.getByRole("row", { name: /Guioza/ }).boundingBox();
  await page.mouse.move(handle!.x + handle!.width / 2, handle!.y + handle!.height / 2);
  await page.mouse.down();
  await page.mouse.move(handle!.x + handle!.width / 2, target!.y + 2, { steps: 8 });
  await page.mouse.up();
  await expect(productNames(page)).toHaveText(["Sunomono", "Guioza", "Edamame"]);

  // A ordem ficou gravada no back.
  await page.reload();
  await expect(productNames(page)).toHaveText(["Sunomono", "Guioza", "Edamame"]);
});

test("arrastar categorias por cima de várias, duas vezes seguidas", async ({ page, request }) => {
  const { email } = await createAccount(request);
  const token = await apiLogin(request, email);
  for (const name of ["Entradas", "Sushis", "Temakis", "Bebidas"]) await apiCategory(request, token, name);
  await loginThroughForm(page, email);
  await openMenu(page);
  const names = page.getByRole("navigation", { name: "Categorias" }).getByRole("listitem");
  await expect(names).toHaveText([/Entradas/, /Sushis/, /Temakis/, /Bebidas/]);

  /**
   * Arrasta a alça até passar do centro de outra linha (para baixo ou para cima), em muitos
   * passos, como uma mão faria.
   */
  async function dragOnto(handleName: string, targetName: string, direction: 1 | -1) {
    const handle = (await page.getByRole("button", { name: handleName }).boundingBox())!;
    const target = (await names.filter({ hasText: targetName }).boundingBox())!;
    await page.mouse.move(handle.x + handle.width / 2, handle.y + handle.height / 2);
    await page.mouse.down();
    await page.mouse.move(handle.x + handle.width / 2, target.y + target.height / 2 + 4 * direction, { steps: 20 });
    await page.mouse.up();
    await expect(page.getByRole("list").first()).toHaveAttribute("aria-busy", "false");
  }

  await dragOnto("Mover Entradas", "Bebidas", 1); // desce três posições
  await expect(names).toHaveText([/Sushis/, /Temakis/, /Bebidas/, /Entradas/]);
  await dragOnto("Mover Bebidas", "Sushis", -1); // e logo depois sobe outra
  await expect(names).toHaveText([/Bebidas/, /Sushis/, /Temakis/, /Entradas/]);

  await page.reload();
  await expect(names).toHaveText([/Bebidas/, /Sushis/, /Temakis/, /Entradas/]);
});

test("categoria com produtos não pode ser desativada", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await openMenu(page);
  await page.getByRole("button", { name: "Criar a primeira categoria" }).click();
  await newCategory(page, "Bebidas");
  await newProduct(page, "Suco de uva", "10");

  await page.getByRole("button", { name: "Desativar categoria" }).click();
  const confirm = page.getByRole("alertdialog");
  await confirm.getByRole("button", { name: "Desativar" }).click();
  await expect(confirm.getByRole("alert")).toHaveText("Esta categoria tem 1 produto. Mova ou desative o produto antes.");
  await confirm.getByRole("button", { name: "Cancelar" }).click();

  await page.getByRole("link", { name: "Suco de uva" }).click();
  await page.getByRole("button", { name: "Desativar produto" }).click();
  await page.getByRole("alertdialog").getByRole("button", { name: "Desativar" }).click();
  await expect(page.getByText("Nenhum produto nesta categoria ainda.")).toBeVisible();

  await page.getByRole("button", { name: "Desativar categoria" }).click();
  await page.getByRole("alertdialog").getByRole("button", { name: "Desativar" }).click();
  await expect(page.getByRole("heading", { name: "Comece pelas categorias" })).toBeVisible();
});
