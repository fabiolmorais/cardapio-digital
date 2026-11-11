import { devices, expect, test } from "@playwright/test";
import { createAccount, loginThroughForm } from "./support/account";

const APP_URL = "http://localhost:3000";

test("aparelho novo mostra o código e é vinculado pelo painel", async ({ browser, page, request }) => {
  const { email } = await createAccount(request);

  // O celular novo: outro navegador, sem login nenhum.
  const phoneContext = await browser.newContext({ ...devices["Pixel 7"], locale: "pt-BR" });
  const phone = await phoneContext.newPage();
  await phone.goto(`${APP_URL}/aparelho`);
  const codeBox = phone.getByTestId("pairing-code");
  await expect(codeBox).toBeVisible();
  const code = await codeBox.getAttribute("data-code");
  expect(code).toMatch(/^\d{6}$/);
  await expect(phone.getByText(/Vale por \d+:\d{2}/)).toBeVisible();
  await expect(phone.getByText("Aguardando o gerente…")).toBeVisible();

  // O dono, no painel, digita o código.
  await loginThroughForm(page, email);
  await page.goto("/admin/equipe");
  await page.getByRole("radio", { name: /^Código/ }).check();
  await page.getByLabel("Código do aparelho").fill(code!);
  await page.getByLabel("Nome do aparelho").fill("Celular do salão");
  await page.getByRole("button", { name: "Vincular", exact: true }).click();
  await expect(page.getByRole("status").filter({ hasText: "Celular do salão vinculado" })).toBeVisible();

  // O celular percebe sozinho, recebe o token num cookie que o JavaScript não lê e passa para o PIN.
  await expect(phone.getByRole("heading", { name: "Quem está entrando?" })).toBeVisible({ timeout: 10_000 });
  await expect(phone.getByText("Aparelho vinculado · Celular do salão")).toBeVisible();
  const cookie = (await phoneContext.cookies()).find((c) => c.name === "device_token");
  expect(cookie?.httpOnly).toBe(true);
  expect(cookie?.sameSite).toBe("Strict");

  // No painel, o aparelho aparece como visto.
  const item = page.getByRole("listitem").filter({ hasText: "Celular do salão" });
  await expect(item).toContainText("Visto agora", { timeout: 10_000 });

  await page.getByRole("button", { name: "Desvincular Celular do salão" }).click();
  const confirm = page.getByRole("alertdialog");
  await confirm.getByRole("button", { name: "Desvincular" }).click();
  await expect(confirm).toBeHidden();
  await expect(page.getByText("Nenhum aparelho vinculado ainda.")).toBeVisible();

  await phoneContext.close();
});

test("código errado mostra o erro no painel", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await page.goto("/admin/equipe");

  await page.getByRole("radio", { name: /^Código/ }).check();
  await page.getByLabel("Código do aparelho").fill("000000");
  await page.getByLabel("Nome do aparelho").fill("Tablet da cozinha");
  await page.getByRole("radio", { name: /Cozinha/ }).check();
  await page.getByRole("button", { name: "Vincular", exact: true }).click();

  await expect(page.getByRole("alert").filter({ hasText: "Código não encontrado ou vencido" })).toBeVisible();
});

test("o gerente mostra um QR Code e o aparelho novo entra vinculado só de escanear", async ({
  browser,
  page,
  request,
}) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await page.goto("/admin/equipe");

  // QR Code é o jeito padrão: só o nome e para que serve.
  await expect(page.getByRole("radio", { name: /^QR Code/ })).toBeChecked();
  await expect(page.getByLabel("Código do aparelho")).toHaveCount(0);
  await page.getByLabel("Nome do aparelho").fill("Tablet da cozinha");
  await page.getByRole("radio", { name: /Cozinha/ }).check();
  await page.getByRole("button", { name: "Mostrar QR Code" }).click();

  const dialog = page.getByRole("dialog", { name: "Vincular Tablet da cozinha" });
  const qr = dialog.getByTestId("device-invite-qr");
  await expect(qr).toBeVisible();
  await expect(dialog.getByText(/Vale por \d+:\d{2}, uma vez só/)).toBeVisible();
  const url = await qr.getAttribute("data-url");
  expect(url).toMatch(/\/aparelho#convite=[0-9a-f-]{36}\.[\w-]+$/);

  // O tablet "escaneia": abre o endereço do QR e já cai na tela do PIN, sem código.
  const tabletContext = await browser.newContext({ ...devices["iPad (gen 7)"], locale: "pt-BR" });
  const tablet = await tabletContext.newPage();
  await tablet.goto(url!);
  await expect(tablet.getByRole("heading", { name: "Quem está entrando?" })).toBeVisible({ timeout: 10_000 });
  await expect(tablet.getByText("Aparelho vinculado · Tablet da cozinha")).toBeVisible();
  expect(new URL(tablet.url()).hash).toBe(""); // o segredo sai do endereço

  // O painel percebe sozinho.
  await expect(dialog.getByText("Tablet da cozinha vinculado")).toBeVisible({ timeout: 10_000 });
  await dialog.getByRole("button", { name: "Concluir" }).click();
  await expect(page.getByRole("listitem").filter({ hasText: "Tablet da cozinha" })).toContainText("Cozinha · Visto");

  // O mesmo QR não vale de novo: outro aparelho vê o aviso e o código.
  const otherContext = await browser.newContext({ ...devices["Pixel 7"], locale: "pt-BR" });
  const other = await otherContext.newPage();
  await other.goto(url!);
  await expect(other.getByRole("alert").filter({ hasText: "Este QR Code venceu ou já foi usado" })).toBeVisible();
  await expect(other.getByTestId("pairing-code")).toBeVisible();

  await tabletContext.close();
  await otherContext.close();
});

test("cancelar o QR sem escanear tira o aparelho da lista", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await page.goto("/admin/equipe");

  await page.getByLabel("Nome do aparelho").fill("Celular do caixa");
  await page.getByRole("button", { name: "Mostrar QR Code" }).click();
  const dialog = page.getByRole("dialog", { name: "Vincular Celular do caixa" });
  await expect(dialog.getByTestId("device-invite-qr")).toBeVisible();
  await expect(page.getByRole("listitem").filter({ hasText: "Celular do caixa" })).toContainText(
    "Aguardando o aparelho",
  );

  await dialog.getByRole("button", { name: "Cancelar" }).click();
  await expect(page.getByText("Nenhum aparelho vinculado ainda.")).toBeVisible();
});

test("no iPhone 7, os 6 números do código cabem no card", async ({ browser }) => {
  const context = await browser.newContext({ ...devices["iPhone 7"], locale: "pt-BR" });
  const phone = await context.newPage();
  await phone.goto(`${APP_URL}/aparelho`);
  const codeBox = phone.getByTestId("pairing-code");
  await expect(codeBox).toBeVisible();

  const box = (await codeBox.boundingBox())!;
  const card = (await codeBox.locator("xpath=..").boundingBox())!;
  expect(box.x).toBeGreaterThanOrEqual(card.x);
  expect(box.x + box.width).toBeLessThanOrEqual(card.x + card.width);
  expect(await phone.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(375);

  await context.close();
});
