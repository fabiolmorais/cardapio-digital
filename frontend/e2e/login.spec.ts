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

test("o que a pessoa digita antes de a página carregar o JavaScript não some", async ({ page, request }) => {
  const { email } = await createAccount(request);
  // Um 4G lento: o JavaScript da página chega 2 s depois do HTML.
  await page.route("**/_next/static/chunks/**", async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 2000));
    await route.continue();
  });

  await page.goto("/entrar", { waitUntil: "commit" });
  await page.getByLabel("E-mail").fill(email);
  await page.getByLabel("Senha").fill(PASSWORD);
  // Espera o React assumir a página (o formulário ganha a ligação com o React) e os efeitos rodarem.
  await page.waitForFunction(() => {
    const form = document.querySelector("form");
    return form !== null && Object.keys(form).some((key) => key.startsWith("__reactFiber"));
  });
  await page.waitForTimeout(500);
  await expect(page.getByLabel("E-mail")).toHaveValue(email);
  await expect(page.getByLabel("Senha")).toHaveValue(PASSWORD);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page).toHaveURL(/\/painel$/, { timeout: 15_000 });
});
