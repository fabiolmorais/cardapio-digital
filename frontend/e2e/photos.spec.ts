import { expect, test, type APIRequestContext, type Page } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

/** Um produto pronto, criado pela API, para a tela de edição. */
async function productWithoutPhotos(request: APIRequestContext) {
  const { email } = await createAccount(request);
  const token = await apiLogin(request, email);
  const headers = { Authorization: `Bearer ${token}` };
  const category = await request.post(`${API_URL}/api/v1/menu-categories`, { headers, data: { name: "Entradas" } });
  const product = await request.post(`${API_URL}/api/v1/products`, {
    headers,
    data: {
      categoryId: (await category.json()).id,
      name: "Guioza",
      priceCents: 2990,
      featured: false,
      available: true,
    },
  });
  expect(product.status()).toBe(201);
  return { email, productId: (await product.json()).id as string };
}

/**
 * Uma foto JPEG gerada no próprio navegador (canvas): nenhum arquivo de imagem no repositório.
 * A cor ajuda a ver qual foto é qual.
 */
async function photo(page: Page, name: string, color: string) {
  const base64 = await page.evaluate(async (fill) => {
    const canvas = document.createElement("canvas");
    canvas.width = 640;
    canvas.height = 480;
    const context = canvas.getContext("2d")!;
    context.fillStyle = fill;
    context.fillRect(0, 0, 640, 480);
    const blob = await new Promise<Blob>((resolve) => canvas.toBlob((b) => resolve(b!), "image/jpeg", 0.9));
    const bytes = new Uint8Array(await blob.arrayBuffer());
    return btoa(String.fromCharCode(...bytes));
  }, color);
  return { name, mimeType: "image/jpeg", buffer: Buffer.from(base64, "base64") };
}

/** Quanto esperar pelo envio de fotos (ver o comentário no teste). */
const UPLOAD_TIMEOUT = 30_000;

function gallery(page: Page) {
  return page.getByRole("region", { name: "Fotos" }).filter({ visible: true });
}

test("dono envia, ordena e remove fotos do produto", async ({ page, request }) => {
  test.setTimeout(60_000);
  const { email, productId } = await productWithoutPhotos(request);
  await loginThroughForm(page, email);
  await page.goto(`/admin/cardapio/produtos/${productId}`);
  await expect(gallery(page)).toContainText("Nenhuma foto ainda");

  // Duas fotos de uma vez: vão uma por vez, com progresso, e a primeira vira a capa.
  const fotos = [await photo(page, "vermelha.jpg", "#c0392b"), await photo(page, "azul.jpg", "#2c3e50")];
  await gallery(page).getByLabel("Enviar foto ou vídeo").setInputFiles(fotos);
  // Prazo maior só aqui: em desenvolvimento as fotos vão ao R2 de verdade, e o back ainda baixa
  // cada uma, gera os três tamanhos e envia de volta (perto de 5 s por foto). No CI, com o S3Mock,
  // leva menos de 1 s.
  await expect(gallery(page).getByRole("status").filter({ hasText: "2 fotos adicionadas." })).toBeVisible({
    timeout: UPLOAD_TIMEOUT,
  });
  const thumbs = gallery(page).getByRole("img");
  await expect(thumbs).toHaveCount(2);
  await expect(thumbs.nth(0)).toHaveAccessibleName("Guioza, foto 1");
  await expect(thumbs.nth(1)).toHaveAccessibleName("Guioza, foto 2");
  await expect(gallery(page).getByText("Capa", { exact: true })).toHaveCount(1);
  const firstSrc = await thumbs.first().getAttribute("src");

  // A miniatura existe de verdade no armazenamento.
  expect((await request.get(firstSrc!)).status()).toBe(200);

  // Pelo teclado: a segunda foto vai para a frente e vira a capa.
  // exact: "Mover a foto 2" também está dentro de "Remover a foto 2".
  await gallery(page).getByRole("button", { name: "Mover a foto 2", exact: true }).press("ArrowLeft");
  await expect(thumbs.nth(1)).toHaveAttribute("src", firstSrc!);
  // Gravou no back: depois de recarregar, a ordem continua.
  await page.reload();
  await expect(thumbs.nth(1)).toHaveAttribute("src", firstSrc!);

  // Remover, com confirmação.
  await gallery(page).getByRole("button", { name: "Remover a foto 2", exact: true }).click();
  await page.getByRole("alertdialog").getByRole("button", { name: "Remover" }).click();
  await expect(thumbs).toHaveCount(1);
  await expect(thumbs.first()).not.toHaveAttribute("src", firstSrc!);
});

test("arquivo que não é JPG ou PNG é recusado antes de enviar", async ({ page, request }) => {
  const { email, productId } = await productWithoutPhotos(request);
  await loginThroughForm(page, email);
  await page.goto(`/admin/cardapio/produtos/${productId}`);

  await gallery(page)
    .getByLabel("Enviar foto ou vídeo")
    .setInputFiles({ name: "cardapio.pdf", mimeType: "application/pdf", buffer: Buffer.from("%PDF-1.4") });
  await expect(gallery(page).getByRole("alert")).toHaveText("cardapio.pdf: envie uma foto JPG ou PNG.");
  await expect(gallery(page)).toContainText("Nenhuma foto ainda");
});

test("produto novo pede para salvar antes das fotos", async ({ page, request }) => {
  const { email } = await productWithoutPhotos(request);
  await loginThroughForm(page, email);
  await page.goto("/admin/cardapio/produtos/novo");
  await expect(page.getByText("Salve o produto para adicionar fotos.")).toBeVisible();
});
