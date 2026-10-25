import { expect, test } from "@playwright/test";

const unique = () => `${Date.now()}-${Math.round(Math.random() * 1e6)}`;

async function fill(page: import("@playwright/test").Page, email: string, document = "11.222.333/0001-81") {
  await page.getByLabel("Nome do restaurante").fill(`Restaurante Teste ${unique()}`);
  await page.getByLabel("Seu nome").fill("Marina Tanaka");
  await page.getByLabel("WhatsApp").fill("(11) 98765-4321");
  await page.getByLabel("CPF ou CNPJ").fill(document);
  await page.getByLabel("E-mail").fill(email);
  await page.getByLabel("Senha").fill("sushi2026bom");
}

test("cadastro cria a conta e mostra a confirmação", async ({ page }) => {
  await page.goto("/cadastro");
  await fill(page, `dono-${unique()}@teste.com`);
  await page.getByRole("checkbox").click();
  await page.getByRole("button", { name: "Criar conta e começar o teste" }).click();

  await expect(page.getByRole("heading", { name: "Conta criada!" })).toBeVisible();
});

test("cadastro exige aceitar os termos", async ({ page }) => {
  await page.goto("/cadastro");
  await fill(page, `dono-${unique()}@teste.com`);
  await page.getByRole("button", { name: "Criar conta e começar o teste" }).click();

  await expect(page.locator("#signup-error")).toContainText("aceite os termos");
});

test("cadastro mostra o erro do servidor para CPF inválido", async ({ page }) => {
  await page.goto("/cadastro");
  await fill(page, `dono-${unique()}@teste.com`, "111.111.111-11");
  await page.getByRole("checkbox").click();
  await page.getByRole("button", { name: "Criar conta e começar o teste" }).click();

  await expect(page.locator("#signup-error")).toContainText("CPF ou CNPJ inválido");
});
