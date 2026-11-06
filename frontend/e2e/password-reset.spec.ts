import { expect, test } from "@playwright/test";
import { createAccount } from "./support/account";

// O e-mail de verdade sai pelo Resend, que não roda aqui: o caminho com o link do e-mail está no
// teste de integração do back (PasswordResetTest). Aqui, as telas e as respostas de erro.

test("da tela de entrar, o dono pede o link e recebe a mesma resposta de sempre", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await page.goto("/entrar");
  await page.getByRole("link", { name: "Esqueci a senha" }).click();
  await expect(page.getByRole("heading", { name: "Esqueci a senha" })).toBeVisible();

  await page.getByLabel("E-mail").fill(email);
  await page.getByRole("button", { name: "Mandar o link" }).click();
  await expect(page.getByRole("status")).toContainText(
    "Se houver uma conta com este e-mail, o link chega em instantes.",
  );

  // Um e-mail sem conta: a mesma frase (ninguém descobre quem é cliente).
  await page.getByLabel("E-mail").fill("ninguem-aqui@teste.com");
  await page.getByRole("button", { name: "Mandar de novo" }).click();
  await expect(page.getByRole("status")).toContainText("Se houver uma conta com este e-mail");
});

test("a senha nova confere as duas senhas, e um link inválido leva a pedir outro", async ({ page }) => {
  await page.goto("/redefinir-senha");
  await expect(page.locator("main").getByRole("alert")).toContainText("Este link está incompleto.");

  await page.goto("/redefinir-senha?token=token-que-nao-existe");
  await page.getByLabel("Senha nova", { exact: true }).fill("sushi2027novo");
  await page.getByLabel("Repita a senha nova").fill("sushi2027outra");
  await page.getByRole("button", { name: "Salvar a senha nova" }).click();
  await expect(page.locator("main").getByRole("alert")).toHaveText("As duas senhas não são iguais.");

  await page.getByLabel("Repita a senha nova").fill("sushi2027novo");
  await page.getByRole("button", { name: "Salvar a senha nova" }).click();
  await expect(page.locator("main").getByRole("alert")).toHaveText(
    "Este link para redefinir a senha expirou ou já foi usado. Peça outro.",
  );
  await page.getByRole("link", { name: "Pedir outro link" }).click();
  await expect(page.getByRole("heading", { name: "Esqueci a senha" })).toBeVisible();
});
