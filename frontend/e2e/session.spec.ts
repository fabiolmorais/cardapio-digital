import { expect, test } from "@playwright/test";
import { createAccount, loginThroughForm, panelGreeting } from "./support/account";

const isRefresh = (url: string) => url.endsWith("/api/v1/auth/refresh");

test("F5 no painel continua logado", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);

  await page.reload();

  await expect(panelGreeting(page)).toBeVisible();
  await expect(page).toHaveURL(/\/painel$/);
});

test("depois de sair, o painel pede login de novo", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);

  await page.getByRole("button", { name: "Sair" }).click();
  await expect(page).toHaveURL(/\/entrar$/);

  // O cookie foi apagado e o refresh token revogado: abrir o painel de novo não entra.
  await page.goto("/painel");
  await expect(page).toHaveURL(/\/entrar$/);
});

test("duas abas recarregando ao mesmo tempo continuam logadas", async ({ page, context, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  const other = await context.newPage();

  // O back troca o cookie a cada renovação; duas abas renovando juntas mandariam o mesmo
  // cookie, e a segunda seria vista como reuso de token, derrubando a sessão. O Web Lock
  // põe uma renovação depois da outra: aqui cada uma fica presa meio segundo antes de sair,
  // e o teste confere que nunca há duas em andamento ao mesmo tempo.
  let inFlight = 0;
  let maxInFlight = 0;
  await context.route("**/api/v1/auth/refresh", async (route) => {
    inFlight += 1;
    maxInFlight = Math.max(maxInFlight, inFlight);
    await new Promise((resolve) => setTimeout(resolve, 500));
    await route.continue();
  });
  const done = (r: { url: () => string }) => {
    if (isRefresh(r.url())) inFlight -= 1;
  };
  context.on("requestfinished", done);
  context.on("requestfailed", done);

  await Promise.all([page.reload(), other.goto("/painel")]);
  await expect(panelGreeting(page)).toBeVisible();
  await expect(panelGreeting(other)).toBeVisible();

  await Promise.all([page.reload(), other.reload()]);
  await expect(panelGreeting(page)).toBeVisible();
  await expect(panelGreeting(other)).toBeVisible();

  expect(maxInFlight).toBe(1);
});

test("sair numa aba tira a outra do painel", async ({ page, context, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  const other = await context.newPage();
  await other.goto("/painel");
  await expect(panelGreeting(other)).toBeVisible();

  await page.getByRole("button", { name: "Sair" }).click();

  await expect(other).toHaveURL(/\/entrar$/);
});

test("renova o token sozinho antes de ele vencer", async ({ page, request }) => {
  await page.clock.install();
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);

  // O token de acesso vale 15 minutos; a aba renova 1 minuto antes.
  const renewed = page.waitForResponse((response) => isRefresh(response.url()));
  await page.clock.fastForward("14:30");
  expect((await renewed).status()).toBe(200);

  // Depois da renovação, o painel continua funcionando, inclusive após um F5.
  await page.reload();
  await expect(panelGreeting(page)).toBeVisible();
});
