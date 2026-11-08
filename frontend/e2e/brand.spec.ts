import { expect, test, type Page } from "@playwright/test";
import { createAccount, loginThroughForm, openMenu } from "./support/account";

/** Abre a marca pelo atalho do painel. */
async function openBrand(page: Page) {
  await openMenu(page);
  await page.getByRole("link", { name: /^Marca e aparência/ }).click();
  await expect(page.getByRole("heading", { name: "Marca e aparência", level: 1 })).toBeVisible();
}

/** Uma logo PNG quadrada gerada no próprio navegador (canvas), sem arquivo no repositório. */
async function logo(page: Page) {
  const base64 = await page.evaluate(async () => {
    const canvas = document.createElement("canvas");
    canvas.width = 256;
    canvas.height = 256;
    const context = canvas.getContext("2d")!;
    context.fillStyle = "#0f766e";
    context.beginPath();
    context.arc(128, 128, 120, 0, Math.PI * 2);
    context.fill();
    const blob = await new Promise<Blob>((resolve) => canvas.toBlob((b) => resolve(b!), "image/png"));
    const bytes = new Uint8Array(await blob.arrayBuffer());
    return btoa(String.fromCharCode(...bytes));
  });
  return { name: "logo.png", mimeType: "image/png", buffer: Buffer.from(base64, "base64") };
}

test("dono muda nome, cor, tema e Instagram, e o menu mostra o nome novo", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await openBrand(page);

  await page.getByLabel("Nome na página da mesa").fill("Sakura Sushi");
  await page.getByRole("radio", { name: "Petróleo" }).check();
  await page.getByRole("radio", { name: "Claro" }).check();
  await page.getByLabel("Instagram").fill("https://www.instagram.com/sakurasushi.sp/");
  // A prévia já mostra os links do fim da página da mesa, antes de salvar.
  const preview = page.getByRole("img", { name: /Prévia ao vivo/ });
  await expect(preview).toContainText("Siga no Instagram · @sakurasushi.sp");
  await expect(preview).not.toContainText("Avalie no Google");
  await page.getByLabel("Link de avaliação no Google").fill("https://g.page/r/CSakura/review");
  await expect(preview).toContainText("Avalie no Google");
  await page.getByRole("button", { name: "Salvar" }).click();
  await expect(page.getByRole("status").filter({ hasText: "Marca salva." })).toBeVisible();
  // O campo fica com o @ que o back guardou.
  await expect(page.getByLabel("Instagram")).toHaveValue("sakurasushi.sp");

  // O menu lateral (a sessão) já mostra o nome novo.
  await expect(page.getByRole("complementary", { name: "Menu" })).toContainText("Sakura Sushi");

  // Gravado no back: depois de recarregar, tudo continua, e o Instagram ficou só com o @.
  await page.reload();
  await expect(page.getByLabel("Nome na página da mesa")).toHaveValue("Sakura Sushi");
  await expect(page.getByRole("radio", { name: "Petróleo" })).toBeChecked();
  await expect(page.getByRole("radio", { name: "Claro" })).toBeChecked();
  await expect(page.getByLabel("Instagram")).toHaveValue("sakurasushi.sp");
});

test("link de avaliação que não é do Google é recusado", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await openBrand(page);

  await page.getByLabel("Link de avaliação no Google").fill("https://g.page.golpe.com/avalie");
  await page.getByRole("button", { name: "Salvar" }).click();
  await expect(page.getByRole("alert").filter({ hasText: "Use o link de avaliação do Google" })).toBeVisible();
});

test("enviar a logo e voltar para as iniciais", async ({ page, request }) => {
  const { email, establishmentName } = await createAccount(request);
  await loginThroughForm(page, email);
  await openBrand(page);

  await page.getByLabel("Enviar imagem").setInputFiles(await logo(page));
  await expect(page.getByRole("status").filter({ hasText: "Logo atualizada." })).toBeVisible();
  const image = page.getByRole("img", { name: `Logo de ${establishmentName}` });
  await expect(image).toBeVisible();
  // A imagem existe de verdade no armazenamento (a largura natural só existe se ela carregou).
  await expect.poll(() => image.evaluate((img) => (img as HTMLImageElement).naturalWidth)).toBe(512);

  await page.getByRole("button", { name: "Usar iniciais" }).click();
  await expect(page.getByRole("status").filter({ hasText: "A página voltou a mostrar as iniciais." })).toBeVisible();
  await expect(page.getByRole("img", { name: `Logo de ${establishmentName}` })).toHaveCount(0);
  await expect(page.getByLabel("Enviar imagem")).toBeAttached();
});
