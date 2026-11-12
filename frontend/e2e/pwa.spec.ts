import { expect, test, type Page } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm, openMenu } from "./support/account";

test.describe.configure({ timeout: 60_000 });

test("o painel vira app (manifesto da equipe), e a página da mesa não", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await expect(page.locator('link[rel="manifest"]')).toHaveAttribute("href", "/equipe.webmanifest");

  const manifest = await (await request.get("/equipe.webmanifest")).json();
  expect(manifest.start_url).toBe("/painel");
  expect(manifest.display).toBe("standalone");
  for (const icon of manifest.icons) expect((await request.get(icon.src)).ok()).toBe(true);

  // O service worker do app fica só nas telas do painel.
  const scope = await page.evaluate(async () => {
    const registration = await navigator.serviceWorker.getRegistration("/painel");
    return registration?.scope;
  });
  expect(scope).toMatch(/\/painel$/);

  // A mesa: sem manifesto.
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const sector = await (await request.post(`${API_URL}/api/v1/sectors`, { headers, data: { name: "Salão" } })).json();
  const table = await (
    await request.post(`${API_URL}/api/v1/tables`, { headers, data: { label: "04", sectorId: sector.id } })
  ).json();
  await page.goto(`/m/${table.publicToken}`);
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  await expect(page.locator('link[rel="manifest"]')).toHaveCount(0);
});

test("o painel leva ao passo a passo da instalação", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await openMenu(page);
  await page.getByRole("navigation", { name: "Navegação" }).getByRole("link", { name: "Instalar o app" }).click();
  await expect(page.getByRole("heading", { name: "Instalar o app da equipe" })).toBeVisible();
  await expect(page.getByRole("region", { name: "No Android (Chrome)" })).toContainText("Instalar app");
  await expect(page.getByRole("region", { name: "No iPhone (Safari)" })).toContainText("Adicionar à Tela de Início");
});

/**
 * O Chromium dos testes não alcança o serviço de push do Google e sempre nega os avisos: a
 * permissão e a inscrição do navegador são simuladas (um endereço do FCM e chaves P-256 de verdade). O resto é real: a chave VAPID vem do
 * back, e a inscrição é gravada e apagada nele.
 */
async function fakePushService(page: Page) {
  await page.addInitScript(() => {
    let current: PushSubscription | null = null;
    Object.defineProperty(Notification, "permission", { get: () => "granted" });
    Notification.requestPermission = async () => "granted";
    const b64 = (bytes: ArrayBuffer) =>
      btoa(String.fromCharCode(...new Uint8Array(bytes)))
        .replace(/\+/g, "-")
        .replace(/\//g, "_")
        .replace(/=+$/, "");
    PushManager.prototype.getSubscription = async () => current;
    PushManager.prototype.subscribe = async (options?: PushSubscriptionOptionsInit) => {
      const pair = await crypto.subtle.generateKey({ name: "ECDH", namedCurve: "P-256" }, true, ["deriveBits"]);
      const p256dh = b64(await crypto.subtle.exportKey("raw", pair.publicKey));
      const auth = b64(crypto.getRandomValues(new Uint8Array(16)).buffer);
      const endpoint = `https://fcm.googleapis.com/fcm/send/e2e-${crypto.randomUUID()}`;
      const subscription = {
        endpoint,
        expirationTime: null,
        options: { applicationServerKey: options?.applicationServerKey, userVisibleOnly: true },
        getKey: () => null,
        toJSON: () => ({ endpoint, expirationTime: null, keys: { p256dh, auth } }),
        unsubscribe: async () => {
          current = null;
          return true;
        },
      };
      current = subscription as unknown as PushSubscription;
      return current;
    };
  });
}

test("o garçom ativa as notificações no painel, e desativa", async ({ page, request }) => {
  // O back precisa das chaves VAPID (no CI, geradas na hora; aqui, no .env).
  const { email } = await createAccount(request);
  await fakePushService(page);
  await loginThroughForm(page, email);

  // A tag fica no topo do Ao vivo, ao lado do "Som ligado".
  const subscribed = page.waitForResponse(
    (r) => r.url().endsWith("/api/v1/push/subscriptions") && r.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Avisos no celular desligados. Toque para ligar" }).click();
  expect((await subscribed).status()).toBe(201);
  const on = page.getByRole("button", { name: "Avisos no celular ligados. Toque para desligar" });
  await expect(on).toHaveAttribute("aria-pressed", "true");
  await expect(on).toHaveText("Avisos ligados");

  const removed = page.waitForResponse(
    (r) => r.url().includes("/api/v1/push/subscriptions?endpoint=") && r.request().method() === "DELETE",
  );
  await on.click();
  expect((await removed).status()).toBe(204);
  await expect(page.getByRole("button", { name: "Avisos no celular desligados. Toque para ligar" })).toHaveText(
    "Avisos desligados",
  );
});
