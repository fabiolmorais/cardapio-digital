import { expect, type APIRequestContext, type Page } from "@playwright/test";

export const API_URL = process.env.API_URL ?? "http://localhost:8080";
export const PASSWORD = "sushi2026bom";
export const OWNER_NAME = "Marina Tanaka";

/** Cria uma conta nova direto pela API e devolve o e-mail e o nome do restaurante. */
export async function createAccount(request: APIRequestContext) {
  const id = `${Date.now()}-${Math.round(Math.random() * 1e6)}`;
  const email = `dono-${id}@teste.com`;
  const establishmentName = `Restaurante Login ${id}`;
  const response = await request.post(`${API_URL}/api/v1/signup`, {
    data: {
      establishmentName,
      ownerName: OWNER_NAME,
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

/** Entra pela tela /entrar e espera o painel abrir. */
export async function loginThroughForm(page: Page, email: string) {
  await page.goto("/entrar");
  await page.getByLabel("E-mail").fill(email);
  await page.getByLabel("Senha").fill(PASSWORD);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page).toHaveURL(/\/painel$/);
  await expect(panelGreeting(page)).toBeVisible();
}

export function panelGreeting(page: Page) {
  return page.getByRole("heading", { name: `Olá, ${OWNER_NAME}` });
}
