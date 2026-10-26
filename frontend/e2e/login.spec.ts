import { expect, test, type APIRequestContext } from "@playwright/test";

const API_URL = process.env.API_URL ?? "http://localhost:8080";
const PASSWORD = "sushi2026bom";

/** Cria uma conta nova direto pela API e devolve o e-mail e o nome do restaurante. */
async function createAccount(request: APIRequestContext) {
  const id = `${Date.now()}-${Math.round(Math.random() * 1e6)}`;
  const email = `dono-${id}@teste.com`;
  const establishmentName = `Restaurante Login ${id}`;
  const response = await request.post(`${API_URL}/api/v1/signup`, {
    data: {
      establishmentName,
      ownerName: "Marina Tanaka",
      document: "11222333000181",
      whatsapp: "81999999999",
      email,
      password: PASSWORD,
      acceptedTerms: true,
    },
  });
  expect(response.status()).toBe(201);
  return { email, establishmentName };
}

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
