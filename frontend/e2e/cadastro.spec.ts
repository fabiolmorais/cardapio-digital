import { expect, test, type Page } from "@playwright/test";

const unique = () => `${Date.now()}-${Math.round(Math.random() * 1e6)}`;
const SUBMIT = "Criar conta e começar o teste";

async function fill(page: Page, email: string, document = "11222333000181", confirm = "sushi2026bom") {
  await page.getByLabel("Nome do restaurante").fill(`Restaurante Teste ${unique()}`);
  await page.getByLabel("Seu nome").fill("Marina Tanaka");
  await page.getByLabel("WhatsApp").fill("81999999999");
  await page.getByLabel("CPF ou CNPJ").fill(document);
  await page.getByLabel("E-mail").fill(email);
  await page.getByLabel("Senha", { exact: true }).fill("sushi2026bom");
  await page.getByLabel("Confirme a senha").fill(confirm);
}

test("cadastro cria a conta e mostra a confirmação", async ({ page }) => {
  await page.goto("/cadastro");
  await fill(page, `dono-${unique()}@teste.com`);
  await page.getByRole("checkbox").click();
  await page.getByRole("button", { name: SUBMIT }).click();

  await expect(page.getByRole("heading", { name: "Conta criada!" })).toBeVisible();
});

test("cadastro exige aceitar os termos", async ({ page }) => {
  await page.goto("/cadastro");
  await fill(page, `dono-${unique()}@teste.com`);
  await page.getByRole("button", { name: SUBMIT }).click();

  await expect(page.locator("#signup-error")).toContainText("aceite os termos");
});

test("cadastro mostra o erro do servidor para CPF inválido", async ({ page }) => {
  await page.goto("/cadastro");
  await fill(page, `dono-${unique()}@teste.com`, "11111111111");
  await page.getByRole("checkbox").click();
  await page.getByRole("button", { name: SUBMIT }).click();

  await expect(page.locator("#signup-error")).toContainText("CPF ou CNPJ inválido");
});

test("cadastro recusa senhas diferentes antes de enviar", async ({ page }) => {
  await page.goto("/cadastro");
  await fill(page, `dono-${unique()}@teste.com`, "11222333000181", "sushi2026ruim");
  await page.getByRole("checkbox").click();
  await page.getByRole("button", { name: SUBMIT }).click();

  await expect(page.locator("#signup-error")).toContainText("As senhas não são iguais");
  await expect(page.getByLabel("Confirme a senha")).toHaveAttribute("aria-invalid", "true");
});

test("aviso de senhas aparece enquanto a pessoa digita", async ({ page }) => {
  await page.goto("/cadastro");
  const password = page.getByLabel("Senha", { exact: true });
  const confirm = page.getByLabel("Confirme a senha");
  const status = page.locator("#password-match");

  await password.fill("sushi2026bom");
  await expect(status).toHaveText("");

  await confirm.pressSequentially("sushi2026");
  await expect(status).toHaveText("As senhas ainda não são iguais");
  await expect(confirm).toHaveAttribute("aria-invalid", "true");

  await confirm.pressSequentially("bom");
  await expect(status).toHaveText("As senhas são iguais");
  await expect(confirm).not.toHaveAttribute("aria-invalid");

  await password.fill("sushi2026outra");
  await expect(status).toHaveText("As senhas ainda não são iguais");
});

test("telefone e documento são formatados enquanto a pessoa digita", async ({ page }) => {
  await page.goto("/cadastro");
  const phone = page.getByLabel("WhatsApp");
  const document = page.getByLabel("CPF ou CNPJ");

  await phone.pressSequentially("8199999999912345");
  await expect(phone).toHaveValue("(81) 99999-9999");
  await phone.fill("");
  await phone.pressSequentially("8133334444");
  await expect(phone).toHaveValue("(81) 3333-4444");

  await document.pressSequentially("11111111122");
  await expect(document).toHaveValue("111.111.111-22");
  await document.fill("");
  await document.pressSequentially("1122233300018199");
  await expect(document).toHaveValue("11.222.333/0001-81");
  await document.fill("");
  await document.pressSequentially("12abc34501de35");
  await expect(document).toHaveValue("12.ABC.345/01DE-35");
});
