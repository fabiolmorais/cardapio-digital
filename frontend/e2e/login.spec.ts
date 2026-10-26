import { expect, test } from "@playwright/test";
import { createAccount, PASSWORD } from "./support/account";

test("login leva ao painel com os dados de quem entrou", async ({ page, request }) => {
  const { email, establishmentName } = await createAccount(request);

  await page.goto("/entrar");
  await page.getByLabel("E-mail").fill(email);
  await page.getByLabel("Senha").fill(PASSWORD);
  await page.getByRole("button", { name: "Entrar" }).click();

  await expect(page).toHaveURL(/\/painel$/);
  await expect(page.getByRole("heading", { name: "Olá, Marina Tanaka" })).toBeVisible();
  await expect(page.getByText(establishmentName)).toBeVisible();
  await expect(page.getByText("Seu papel: Dono")).toBeVisible();

  await page.getByRole("button", { name: "Sair" }).click();
  await expect(page).toHaveURL(/\/entrar$/);
  // A tela de entrar volta limpa: botão liberado e sem a senha de quem saiu.
  await expect(page.getByRole("button", { name: "Entrar" })).toBeEnabled();
  await expect(page.getByLabel("Senha")).toHaveValue("");
});

test("senha errada mostra o erro sem sair da tela", async ({ page, request }) => {
  const { email } = await createAccount(request);

  await page.goto("/entrar");
  await page.getByLabel("E-mail").fill(email);
  await page.getByLabel("Senha").fill("senhaErrada123");
  await page.getByRole("button", { name: "Entrar" }).click();

  await expect(page.locator("#login-error")).toHaveText("E-mail ou senha incorretos");
  await expect(page).toHaveURL(/\/entrar$/);
});

test("painel sem login volta para a tela de entrar", async ({ page }) => {
  await page.goto("/painel");
  await expect(page).toHaveURL(/\/entrar$/);
});

test("voltar para a tela de entrar pelo navegador mostra o formulário limpo", async ({ page, request }) => {
  const { email } = await createAccount(request);

  await page.goto("/entrar");
  await page.getByLabel("E-mail").fill(email);
  await page.getByLabel("Senha").fill(PASSWORD);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page).toHaveURL(/\/painel$/);

  await page.goBack();
  await expect(page).toHaveURL(/\/entrar$/);
  await expect(page.getByRole("button", { name: "Entrar" })).toBeEnabled();
  await expect(page.getByLabel("Senha")).toHaveValue("");
});
