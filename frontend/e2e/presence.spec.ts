import { expect, test, type APIRequestContext, type Browser } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";
import { choose } from "./support/select";

// Dois navegadores por teste (o celular e o painel), com login: mais tempo.
test.describe.configure({ timeout: 60_000 });

/** Praça da Liberdade, São Paulo: o "restaurante" dos testes. */
const SAKURA = { latitude: -23.5555, longitude: -46.6355 };

/**
 * Um restaurante com a mesa 04 e a regra de presença pedida. Sem rede cadastrada, o celular do
 * teste (que chega pela rede local) não prova a presença pelo Wi-Fi.
 */
async function restaurant(request: APIRequestContext, rule: object | null) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  if (rule) await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: rule });
  const post = async (path: string, data: object) => (await request.post(`${API_URL}${path}`, { headers, data })).json();
  const sector = await post("/api/v1/sectors", { name: "Salão principal" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  return { qr: table.publicToken as string, email };
}

async function ownerPanel(browser: Browser, email: string) {
  const context = await browser.newContext();
  const page = await context.newPage();
  await loginThroughForm(page, email);
  return { page, close: () => context.close() };
}

test("sem confirmar a presença, a mesa pede liberação e o garçom libera na hora", async ({ page, request, browser }) => {
  const { qr, email } = await restaurant(request, null);
  await page.goto(`/m/${qr}`);

  await expect(page.getByRole("heading", { name: "Não conseguimos confirmar que você está no salão" })).toBeVisible();
  await expect(page.getByRole("button", { name: /Chamar garçom/ })).toHaveCount(0);
  // Sem local nem Wi-Fi cadastrados, só a liberação pelo garçom.
  await expect(page.getByRole("button", { name: /Usar minha localização/ })).toHaveCount(0);
  await page.getByRole("button", { name: "Pedir liberação ao garçom" }).click();
  await expect(page.getByRole("status")).toContainText("Pedido de liberação enviado");

  const panel = await ownerPanel(browser, email);
  try {
    const call = panel.page.getByRole("listitem", { name: "Liberar mesa · Mesa 04" });
    await expect(call).toContainText("Presença não confirmada");
    await call.getByRole("button", { name: "Liberar mesa" }).click();
    await expect(panel.page.getByText("Nenhum chamado aberto")).toBeVisible();

    // A mesa fica sabendo pelo tempo real: os botões aparecem, e o chamado mostra "Liberada pelo garçom".
    await expect(page.getByRole("button", { name: /Chamar garçom/ })).toBeVisible({ timeout: 5000 });
    await page.getByRole("button", { name: /Chamar garçom/ }).click();
    await expect(panel.page.getByRole("listitem", { name: "Chamar garçom · Mesa 04" })).toContainText(
      "Liberada pelo garçom",
    );
  } finally {
    await panel.close();
  }
});

test("o garçom recusa, e a mesa volta a poder pedir", async ({ page, request, browser }) => {
  const { qr, email } = await restaurant(request, null);
  await page.goto(`/m/${qr}`);
  await page.getByRole("button", { name: "Pedir liberação ao garçom" }).click();
  await expect(page.getByRole("status")).toContainText("Pedido de liberação enviado");

  const panel = await ownerPanel(browser, email);
  try {
    await panel.page
      .getByRole("listitem", { name: "Liberar mesa · Mesa 04" })
      .getByRole("button", { name: "Recusar" })
      .click();
    await expect(page.getByRole("button", { name: "Pedir liberação ao garçom" })).toBeVisible({ timeout: 5000 });
  } finally {
    await panel.close();
  }
});

test("fora do Wi-Fi, a localização dentro do raio libera os botões", async ({ request, browser }) => {
  const { qr } = await restaurant(request, {
    networks: ["198.51.100.7"], // uma rede que não é a do teste
    wifiName: "Sakura Clientes",
    location: { ...SAKURA, radiusMeters: 100 },
  });
  const context = await browser.newContext({
    locale: "pt-BR",
    permissions: ["geolocation"],
    geolocation: { latitude: -23.5556, longitude: -46.6355, accuracy: 20 },
  });
  const phone = await context.newPage();
  try {
    await phone.goto(`/m/${qr}`);
    await expect(phone.getByRole("button", { name: /Conectar ao Wi-Fi “Sakura Clientes”/ })).toBeVisible();

    await phone.getByRole("button", { name: /Usar minha localização/ }).click();

    await expect(phone.getByRole("button", { name: /Chamar garçom/ })).toBeVisible();
  } finally {
    await context.close();
  }
});

test("longe do restaurante, a localização não basta", async ({ request, browser }) => {
  const { qr } = await restaurant(request, { networks: [], location: { ...SAKURA, radiusMeters: 100 } });
  const context = await browser.newContext({
    locale: "pt-BR",
    permissions: ["geolocation"],
    geolocation: { latitude: -23.57, longitude: -46.6355, accuracy: 20 }, // ~1,6 km
  });
  const phone = await context.newPage();
  try {
    await phone.goto(`/m/${qr}`);
    await phone.getByRole("button", { name: /Usar minha localização/ }).click();

    await expect(phone.getByRole("alert").filter({ hasText: "Sua localização ficou fora do restaurante" })).toBeVisible();
    await expect(phone.getByRole("button", { name: /Chamar garçom/ })).toHaveCount(0);
  } finally {
    await context.close();
  }
});

test("o dono cadastra a rede de onde está, o nome do Wi-Fi e o local", async ({ page, request }) => {
  const { email } = await restaurant(request, null);
  await loginThroughForm(page, email);
  await page.goto("/admin/presenca");

  await page.getByRole("button", { name: "Usar o IP desta rede" }).click();
  await page.getByLabel("Nome do Wi-Fi para os clientes (opcional)").fill("Sakura Clientes");
  await page.getByLabel("IP ou faixa (CIDR)").fill("2804:14c:6a::/48");
  await page.getByRole("button", { name: "Adicionar" }).click();
  await page.getByLabel("Conferir pela localização").check();
  await choose(page, page.getByLabel("A sessão da mesa expira após"), "1 hora sem uso");
  await expect(page.getByRole("group", { name: /Mapa com o restaurante e o raio de 100 metros/ })).toBeVisible();
  await page.getByRole("button", { name: "Salvar" }).click();

  await expect(page.getByRole("status")).toHaveText("Configuração salva.");
  // O back devolve as faixas com o tamanho; a IPv6 vira o começo da rede, na forma curta.
  await expect(page.getByText("2804:14c:6a::/48")).toBeVisible();
  const token = await apiLogin(request, email);
  const saved = await (
    await request.get(`${API_URL}/api/v1/presence-rule`, { headers: { Authorization: `Bearer ${token}` } })
  ).json();
  expect(saved.networks).toHaveLength(2);
  expect(saved.wifiName).toBe("Sakura Clientes");
  expect(saved.location.radiusMeters).toBe(100);
  expect(saved.sessionIdleMinutes).toBe(60);
});
