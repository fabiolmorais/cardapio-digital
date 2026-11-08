import { expect, test } from "@playwright/test";
import { createAccount, PASSWORD, openMenu } from "./support/account";

test("login leva ao painel com os dados de quem entrou", async ({ page, request }) => {
  const { email, establishmentName } = await createAccount(request);

  await page.goto("/entrar");
  await page.getByLabel("E-mail").fill(email);
  await page.getByLabel("Senha", { exact: true }).fill(PASSWORD);
  await page.getByRole("button", { name: "Entrar" }).click();

  await expect(page).toHaveURL(/\/painel$/);
  await expect(page.getByRole("heading", { name: "Olá, Marina Tanaka" })).toBeVisible();
  await expect(page.getByText(establishmentName)).toBeVisible();
  await openMenu(page);
  await expect(page.getByText("Seu papel: Dono")).toBeVisible();

  await page.getByRole("button", { name: "Sair" }).click();
  await expect(page).toHaveURL(/\/entrar$/);
  // A tela de entrar volta limpa: botão liberado e sem a senha de quem saiu.
  await expect(page.getByRole("button", { name: "Entrar" })).toBeEnabled();
  await expect(page.getByLabel("Senha", { exact: true })).toHaveValue("");
});

test("senha errada mostra o erro sem sair da tela", async ({ page, request }) => {
  const { email } = await createAccount(request);

  await page.goto("/entrar");
  await page.getByLabel("E-mail").fill(email);
  await page.getByLabel("Senha", { exact: true }).fill("senhaErrada123");
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
  await page.getByLabel("Senha", { exact: true }).fill(PASSWORD);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page).toHaveURL(/\/painel$/);

  await page.goBack();
  await expect(page).toHaveURL(/\/entrar$/);
  await expect(page.getByRole("button", { name: "Entrar" })).toBeEnabled();
  await expect(page.getByLabel("Senha", { exact: true })).toHaveValue("");
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
  await page.getByLabel("Senha", { exact: true }).fill(PASSWORD);
  // Espera o React assumir a página (o formulário ganha a ligação com o React) e os efeitos rodarem.
  await page.waitForFunction(() => {
    const form = document.querySelector("form");
    return form !== null && Object.keys(form).some((key) => key.startsWith("__reactFiber"));
  });
  await page.waitForTimeout(500);
  await expect(page.getByLabel("E-mail")).toHaveValue(email);
  await expect(page.getByLabel("Senha", { exact: true })).toHaveValue(PASSWORD);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page).toHaveURL(/\/painel$/, { timeout: 15_000 });
});

test("o olho mostra e esconde a senha digitada", async ({ page }) => {
  await page.goto("/entrar");
  const password = page.getByLabel("Senha", { exact: true });
  await password.fill("sushi2026bom");
  await expect(password).toHaveAttribute("type", "password");

  const eye = page.getByRole("button", { name: "Mostrar senha" });
  await eye.click();
  await expect(password).toHaveAttribute("type", "text");
  await expect(eye).toHaveAttribute("aria-pressed", "true");
  await expect(password).toHaveValue("sushi2026bom");

  await eye.click();
  await expect(password).toHaveAttribute("type", "password");
  await expect(eye).toHaveAttribute("aria-pressed", "false");
});
