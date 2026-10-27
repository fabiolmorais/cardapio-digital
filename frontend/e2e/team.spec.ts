import { expect, test, type Page } from "@playwright/test";
import { createAccount, loginThroughForm } from "./support/account";

/** Abre a equipe pelo cartão do painel. */
async function openTeam(page: Page) {
  await page.getByRole("link", { name: /Equipe/ }).first().click();
  await expect(page).toHaveURL(/\/admin\/equipe$/);
  await expect(page.getByRole("heading", { name: "Equipe" })).toBeVisible();
}

function row(page: Page, name: string) {
  return page.getByRole("row").filter({ hasText: name });
}

test("dono cadastra, edita e desliga alguém da equipe", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await openTeam(page);

  // No celular, a tabela rola dentro da própria caixa: a página não pode ficar mais larga que a tela.
  const width = await page.evaluate(() => document.documentElement.scrollWidth);
  expect(width).toBeLessThanOrEqual(page.viewportSize()!.width);

  // O dono já aparece na lista, sem botões (ninguém altera o dono).
  await expect(row(page, "Marina Tanaka")).toContainText("Dono");
  await expect(row(page, "Marina Tanaka").getByRole("button")).toHaveCount(0);

  await page.getByRole("button", { name: "Adicionar pessoa" }).click();
  const add = page.getByRole("dialog");
  await add.getByLabel("Nome").fill("Carlos Souza");
  await add.getByRole("radio", { name: /Garçom/ }).check();
  await add.getByLabel("PIN").fill("2580");
  await add.getByRole("button", { name: "Adicionar" }).click();
  await expect(add).toBeHidden();
  await expect(row(page, "Carlos Souza")).toContainText("Garçom");
  await expect(row(page, "Carlos Souza")).toContainText("Ativo");

  await page.getByRole("button", { name: "Editar Carlos Souza" }).click();
  const edit = page.getByRole("dialog");
  await edit.getByRole("radio", { name: /Cozinha/ }).check();
  await edit.getByRole("button", { name: "Salvar" }).click();
  await expect(edit).toBeHidden();
  await expect(row(page, "Carlos Souza")).toContainText("Cozinha");

  await page.getByRole("button", { name: "Desligar Carlos Souza" }).click();
  const confirm = page.getByRole("alertdialog");
  await expect(confirm).toContainText("Desligar Carlos Souza?");
  await confirm.getByRole("button", { name: "Desligar" }).click();
  await expect(confirm).toBeHidden();
  await expect(row(page, "Carlos Souza")).toContainText("Desligado");
  await expect(page.getByRole("button", { name: "Desligar Carlos Souza" })).toHaveCount(0);
});

test("PIN fácil mostra o erro do servidor sem fechar o formulário", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await openTeam(page);

  await page.getByRole("button", { name: "Adicionar pessoa" }).click();
  const add = page.getByRole("dialog");
  await add.getByLabel("Nome").fill("Rita");
  await add.getByLabel("PIN").fill("1234");
  await add.getByRole("button", { name: "Adicionar" }).click();

  await expect(add.getByRole("alert")).toHaveText("Esse PIN é fácil de adivinhar. Escolha outro");
  await expect(add).toBeVisible();
});

test("F5 na equipe continua logado", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await openTeam(page);

  await page.reload();

  await expect(page.getByRole("heading", { name: "Equipe" })).toBeVisible();
  await expect(row(page, "Marina Tanaka")).toBeVisible();
});

test("equipe sem login volta para a tela de entrar", async ({ page }) => {
  await page.goto("/admin/equipe");
  await expect(page).toHaveURL(/\/entrar$/);
});
