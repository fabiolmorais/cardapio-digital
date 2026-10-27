import { devices, expect, test, type Browser, type APIRequestContext, type Page } from "@playwright/test";
import { addTeamMember, API_URL, apiLogin, createAccount, linkDeviceByCode } from "./support/account";

const APP_URL = "http://localhost:3000";
const PIN = "258014";

/** Restaurante com o Carlos (garçom) e um celular já vinculado, mostrando a tela do PIN. */
async function waiterPhone(browser: Browser, request: APIRequestContext) {
  const { email } = await createAccount(request);
  const token = await apiLogin(request, email);
  await addTeamMember(request, token, "Carlos Souza", "STAFF", PIN);
  await addTeamMember(request, token, "Rita Lima", "KITCHEN", "739105");

  const context = await browser.newContext({ ...devices["Pixel 7"], locale: "pt-BR" });
  const phone = await context.newPage();
  await phone.goto(`${APP_URL}/aparelho`);
  const code = await phone.getByTestId("pairing-code").getAttribute("data-code");
  const deviceId = await linkDeviceByCode(request, token, code!, "Celular do salão");
  await expect(phone.getByRole("heading", { name: "Quem está entrando?" })).toBeVisible({ timeout: 10_000 });
  return { phone, context, token, deviceId };
}

async function typePin(phone: Page, pin: string) {
  for (const digit of pin) {
    await phone.getByRole("button", { name: digit, exact: true }).click();
  }
}

test("garçom entra com PIN, cai no painel e, ao sair, volta para o PIN", async ({ browser, request }) => {
  const { phone, context } = await waiterPhone(browser, request);

  // O dono não aparece: ele entra com e-mail e senha.
  await expect(phone.getByRole("group", { name: "Equipe" }).getByRole("button")).toHaveCount(2);
  await phone.getByRole("button", { name: "Carlos Souza, Garçom" }).click();
  await typePin(phone, PIN); // entra sozinho ao digitar o 6º dígito

  await expect(phone).toHaveURL(/\/painel$/);
  await expect(phone.getByRole("heading", { name: "Olá, Carlos Souza" })).toBeVisible();
  await expect(phone.getByText("Seu papel: Garçom")).toBeVisible();
  await expect(phone.getByRole("link", { name: /Equipe/ })).toHaveCount(0); // garçom não cuida da equipe

  await phone.getByRole("button", { name: "Sair" }).click();
  await expect(phone).toHaveURL(/\/aparelho$/);
  await expect(phone.getByRole("heading", { name: "Quem está entrando?" })).toBeVisible();
  await context.close();
});

test("PIN errado diz quantas tentativas restam e o quinto bloqueia", async ({ browser, request }) => {
  const { phone, context } = await waiterPhone(browser, request);
  await phone.getByRole("button", { name: "Carlos Souza, Garçom" }).click();

  for (const remaining of [4, 3, 2]) {
    await typePin(phone, "999999");
      await expect(phone.locator("#pin-error")).toHaveText(`PIN incorreto. Restam ${remaining} tentativas.`);
  }
  await typePin(phone, "999999");
  await expect(phone.locator("#pin-error")).toHaveText("PIN incorreto. Resta 1 tentativa.");

  await typePin(phone, "999999");
  await expect(phone.locator("#pin-error")).toContainText("Muitas tentativas erradas. Tente de novo em 15 minutos");
  await expect(phone.getByRole("button", { name: "1", exact: true })).toBeDisabled();
  await context.close();
});

test("aparelho desvinculado volta para a tela do código", async ({ browser, request }) => {
  const { phone, context, token, deviceId } = await waiterPhone(browser, request);
  const unlink = await request.delete(`${API_URL}/api/v1/devices/${deviceId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(unlink.status()).toBe(204);

  await phone.getByRole("button", { name: "Carlos Souza, Garçom" }).click();
  await typePin(phone, PIN);

  await expect(phone.getByTestId("pairing-code")).toBeVisible({ timeout: 10_000 });
  await context.close();
});
