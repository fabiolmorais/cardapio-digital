import { expect, test, type Page } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

test.describe.configure({ timeout: 60_000 });

function gallery(page: Page) {
  return page.getByRole("region", { name: "Fotos e vídeos" }).filter({ visible: true });
}

/**
 * Um "vídeo" de 7 MB: o serviço de vídeo falso não confere o conteúdo, e o tamanho faz o envio
 * ir em dois pedaços (de 5 MB), como um vídeo de verdade no 4G.
 */
const VIDEO = { name: "guioza.mp4", mimeType: "video/mp4", buffer: Buffer.alloc(7 * 1024 * 1024, 7) };

test("o dono envia um vídeo em pedaços, ele converte e entra na galeria", async ({ page, request }) => {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const category = await (
    await request.post(`${API_URL}/api/v1/menu-categories`, { headers, data: { name: "Entradas" } })
  ).json();
  const product = await (
    await request.post(`${API_URL}/api/v1/products`, {
      headers,
      data: { categoryId: category.id, name: "Guioza", priceCents: 2990, featured: false, available: true },
    })
  ).json();
  await loginThroughForm(page, email);
  await page.goto(`/admin/cardapio/produtos/${product.id}`);

  // Tipo errado: o aviso sai antes de enviar.
  await gallery(page)
    .getByLabel("Enviar vídeo")
    .setInputFiles({ name: "cardapio.avi", mimeType: "video/x-msvideo", buffer: Buffer.from("x") });
  await expect(gallery(page).getByRole("alert")).toHaveText("cardapio.avi: envie um vídeo MP4, MOV ou WebM.");

  await gallery(page).getByLabel("Enviar vídeo").setInputFiles(VIDEO);
  await expect(gallery(page).getByText("Vídeo enviado.")).toBeVisible({ timeout: 30_000 });
  // O serviço falso "converte" na hora: 1080 × 1920, 15 segundos.
  const video = gallery(page).getByLabel("Vídeo 1", { exact: true });
  await expect(video).toBeVisible();
  await expect(gallery(page)).toContainText("0:15");
  await expect(gallery(page).getByText("Capa", { exact: true })).toHaveCount(0);

  await gallery(page).getByRole("button", { name: "Remover o vídeo 1" }).click();
  await expect(page.getByRole("alertdialog")).toContainText("apagado do serviço de vídeo");
  await page.getByRole("alertdialog").getByRole("button", { name: "Remover" }).click();
  await expect(gallery(page)).toContainText("Nenhuma foto ainda");
});
