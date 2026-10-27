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

/** Entra pela API e devolve o token de acesso do dono. */
export async function apiLogin(request: APIRequestContext, email: string) {
  const response = await request.post(`${API_URL}/api/v1/auth/login`, { data: { email, password: PASSWORD } });
  expect(response.status()).toBe(200);
  return (await response.json()).accessToken as string;
}

/** Cadastra alguém na equipe pela API e devolve o id. */
export async function addTeamMember(request: APIRequestContext, token: string, name: string, role: string, pin: string) {
  const response = await request.post(`${API_URL}/api/v1/team-members`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { name, role, pin },
  });
  expect(response.status()).toBe(201);
  return (await response.json()).id as string;
}

/** O dono vincula, pela API, o código que aparece na tela do aparelho. */
export async function linkDeviceByCode(request: APIRequestContext, token: string, code: string, name: string) {
  const response = await request.post(`${API_URL}/api/v1/devices`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { code, name, type: "WAITER" },
  });
  expect(response.status()).toBe(201);
  return (await response.json()).id as string;
}
