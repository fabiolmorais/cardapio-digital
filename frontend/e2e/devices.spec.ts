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

  await page.getByLabel("Código do aparelho").fill("000000");
  await page.getByLabel("Nome do aparelho").fill("Tablet da cozinha");
  await page.getByRole("radio", { name: /Cozinha/ }).check();
  await page.getByRole("button", { name: "Vincular", exact: true }).click();

  await expect(page.getByRole("alert").filter({ hasText: "Código não encontrado ou vencido" })).toBeVisible();
});
