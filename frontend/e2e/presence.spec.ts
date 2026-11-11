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
  const post = async (path: string, data: object) =>
    (await request.post(`${API_URL}${path}`, { headers, data })).json();
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

test("a chegada em tela cheia deixa ver só o cardápio, e a trava fica no meio da página", async ({ page, request }) => {
  const { qr } = await restaurant(request, null);
  await page.goto(`/m/${qr}`);

  const chegada = page.getByRole("dialog", { name: "Não conseguimos confirmar que você está no salão" });
  await expect(chegada).toBeVisible();
  await chegada.getByRole("button", { name: "Ver só o cardápio" }).click();
  await expect(chegada).toBeHidden();
  // A página da mesa, com a trava no meio (para pedir a liberação depois), e não volta a tela cheia.
  await expect(page.getByRole("heading", { name: "Não conseguimos confirmar que você está no salão" })).toBeVisible();
  await page.reload();
  await expect(page.getByRole("dialog")).toHaveCount(0);
});

test("sem confirmar a presença, a mesa mostra o código e o garçom libera digitando ele", async ({
  page,
  request,
  browser,
}) => {
  const { qr, email } = await restaurant(request, null);
  await page.goto(`/m/${qr}`);

  await expect(page.getByRole("heading", { name: "Não conseguimos confirmar que você está no salão" })).toBeVisible();
  await expect(page.getByRole("button", { name: /Chamar garçom/ })).toHaveCount(0);
  // Sem local nem Wi-Fi cadastrados, só a liberação pelo garçom.
  await expect(page.getByRole("button", { name: /Usar minha localização/ })).toHaveCount(0);
  await page.getByRole("button", { name: "Pedir liberação ao garçom" }).click();
  await expect(page.getByRole("status")).toContainText("Pedido de liberação enviado");
  const code = await page.getByTestId("release-code").getAttribute("data-code");
  expect(code).toMatch(/^\d{4}$/);

  const panel = await ownerPanel(browser, email);
  try {
    const call = panel.page.getByRole("listitem", { name: "Liberar mesa · Mesa 04" });
    await expect(call).toContainText("Presença não confirmada");
    await expect(call).not.toContainText(code!); // o painel não mostra o código: o garçom vê no celular
    await call.getByRole("button", { name: "Liberar mesa" }).click();

    // Código errado: o pedido continua aberto.
    const dialog = panel.page.getByRole("dialog", { name: "Liberar o celular da mesa 04" });
    await dialog.getByLabel("Código do celular").fill(code === "0000" ? "1111" : "0000");
    await dialog.getByRole("button", { name: "Liberar", exact: true }).click();
    await expect(dialog.getByRole("alert")).toContainText("O código não confere");

    await dialog.getByLabel("Código do celular").fill(code!);
    await dialog.getByRole("button", { name: "Liberar", exact: true }).click();
    await expect(dialog).toBeHidden();
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

test("quem abre o QR de longe não vê o código de quem está na mesa, e não é liberado junto", async ({
  request,
  browser,
}) => {
  const { qr, email } = await restaurant(request, null);
  const inTheRoom = await browser.newContext({ locale: "pt-BR" });
  const fromHome = await browser.newContext({ locale: "pt-BR" });
  const panel = await ownerPanel(browser, email);
  try {
    const home = await fromHome.newPage();
    await home.goto(`/m/${qr}`);
    await home.getByRole("button", { name: "Pedir liberação ao garçom" }).click();
    const homeCode = await home.getByTestId("release-code").getAttribute("data-code");

    const room = await inTheRoom.newPage();
    await room.goto(`/m/${qr}`);
    await room.getByRole("button", { name: "Pedir liberação ao garçom" }).click();
    const roomCode = room.getByTestId("release-code");
    await expect(roomCode).toBeVisible();
    const code = await roomCode.getAttribute("data-code");

    // Dois pedidos na fila, um de cada celular.
    const calls = panel.page.getByRole("listitem", { name: "Liberar mesa · Mesa 04" });
    await expect(calls).toHaveCount(2);

    // O garçom vai à mesa e digita o código do celular que está lá. Tenta no pedido mais novo.
    await calls.last().getByRole("button", { name: "Liberar mesa" }).click();
    const dialog = panel.page.getByRole("dialog", { name: "Liberar o celular da mesa 04" });
    await dialog.getByLabel("Código do celular").fill(code!);
    await dialog.getByRole("button", { name: "Liberar", exact: true }).click();
    await expect(dialog).toBeHidden();

    await expect(room.getByRole("button", { name: /Chamar garçom/ })).toBeVisible({ timeout: 5000 });
    // O de casa continua esperando, com o pedido dele ainda aberto (o garçom pode recusar).
    await home.reload();
    await expect(home.getByTestId("release-code")).toHaveAttribute("data-code", homeCode!);
    await expect(home.getByRole("button", { name: /Chamar garçom/ })).toHaveCount(0);
    await expect(calls).toHaveCount(1);
  } finally {
    await panel.close();
    await inTheRoom.close();
    await fromHome.close();
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
  // Sem a permissão ainda: o navegador pergunta quando o cliente toca no botão.
  const context = await browser.newContext({
    locale: "pt-BR",
    geolocation: { latitude: -23.5556, longitude: -46.6355, accuracy: 20 },
  });
  const phone = await context.newPage();
  try {
    await phone.goto(`/m/${qr}`);
    await expect(phone.getByRole("button", { name: /Conectar ao Wi-Fi “Sakura Clientes”/ })).toBeVisible();

    await context.grantPermissions(["geolocation"]); // o cliente aceita o pedido do navegador
    await phone.getByRole("button", { name: /Usar minha localização/ }).click();

    await expect(phone.getByRole("button", { name: /Chamar garçom/ })).toBeVisible();
  } finally {
    await context.close();
  }
});

test("com a localização já permitida, a mesa confirma sozinha, sem tocar em nada", async ({ request, browser }) => {
  const { qr } = await restaurant(request, { networks: [], location: { ...SAKURA, radiusMeters: 25 } });
  const context = await browser.newContext({
    locale: "pt-BR",
    permissions: ["geolocation"],
    geolocation: { latitude: -23.5556, longitude: -46.6355, accuracy: 20 }, // ~11 m
  });
  const phone = await context.newPage();
  try {
    await phone.goto(`/m/${qr}`);

    await expect(phone.getByRole("button", { name: /Chamar garçom/ })).toBeVisible();
    await expect(phone.getByRole("dialog")).toHaveCount(0);
  } finally {
    await context.close();
  }
});

test("se o servidor não responde ao abrir, a mesa tenta de novo sozinha", async ({ page, request }) => {
  const { qr } = await restaurant(request, null);
  // As duas primeiras conferências falham (o servidor acordando, por exemplo); a terceira passa.
  let failures = 0;
  await page.route("**/api/v1/public/tables/*/presence", (route) =>
    failures++ < 2 ? route.fulfill({ status: 502, body: "" }) : route.fallback(),
  );
  await page.goto(`/m/${qr}`);

  await expect(page.getByRole("dialog", { name: "Não conseguimos confirmar que você está no salão" })).toBeVisible({
    timeout: 15_000,
  });
  expect(failures).toBeGreaterThanOrEqual(3);
});

test("sem falar com o restaurante, a chegada oferece tentar de novo e ver só o cardápio", async ({ page, request }) => {
  test.setTimeout(90_000); // as tentativas somam uns 30 s antes de desistir
  const { qr } = await restaurant(request, null);
  let down = true;
  await page.route("**/api/v1/public/tables/*/session", (route) =>
    down ? route.fulfill({ status: 503, body: "" }) : route.fallback(),
  );
  await page.goto(`/m/${qr}`);

  const unreachable = page.getByRole("dialog", { name: "Não conseguimos falar com o restaurante" });
  await expect(unreachable).toBeVisible({ timeout: 45_000 });

  down = false;
  await unreachable.getByRole("button", { name: "Tentar de novo" }).click();
  await expect(page.getByRole("dialog", { name: "Não conseguimos confirmar que você está no salão" })).toBeVisible();
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

    await expect(
      phone.getByRole("alert").filter({ hasText: "Sua localização ficou fora do restaurante" }),
    ).toBeVisible();
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
