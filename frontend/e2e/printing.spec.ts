import { expect, test, type APIRequestContext, type Browser, type Page } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm, openMenu } from "./support/account";

// O celular da mesa e duas estações de impressão, em navegadores separados: mais tempo.
test.describe.configure({ timeout: 90_000 });

/** Um restaurante com a mesa 04, o Wi-Fi do salão (a rede local do teste) e dois produtos. */
async function restaurant(request: APIRequestContext) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) => (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  const sector = await post("/api/v1/sectors", { name: "Salão principal" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  const entradas = await post("/api/v1/menu-categories", { name: "Entradas" });
  const product = (name: string, priceCents: number) =>
    post("/api/v1/products", { categoryId: entradas.id, name, priceCents, featured: false, available: true });
  await product("Guioza", 2990);
  await product("Sunomono", 1990);
  return { qr: table.publicToken as string, email, headers };
}

/** O celular da mesa pede pelo cardápio: um de cada produto. */
async function order(page: Page, qr: string, names: string[]) {
  await page.goto(`/m/${qr}/cardapio`);
  for (const name of names) {
    await page.getByRole("button", { name: `Adicionar ${name}` }).click();
    await page.getByRole("dialog", { name }).getByRole("button", { name: /^Adicionar/ }).click();
    await expect(page.getByRole("dialog")).toBeHidden();
  }
  await page.getByRole("link", { name: /Ver pedido/ }).click();
  await page.getByRole("button", { name: "Enviar pedido para a cozinha" }).click();
  await expect(page.getByRole("status")).toContainText("Pedido enviado");
  await page.getByRole("link", { name: "Acompanhar pedido" }).click();
}

/**
 * A estação de impressão, num navegador separado, com a impressora trocada por um registro: cada
 * window.print() guarda o texto da comanda que estava na área de impressão.
 */
async function station(browser: Browser, email: string) {
  const context = await browser.newContext();
  const page = await context.newPage();
  await page.addInitScript(() => {
    const prints: string[] = [];
    Object.assign(window, { __prints: prints });
    window.print = () => {
      prints.push(document.querySelector("[data-print-ticket]")?.textContent ?? "");
    };
  });
  await loginThroughForm(page, email);
  await openMenu(page);
  await page.getByRole("navigation", { name: "Navegação" }).getByRole("link", { name: "Impressão de comandas" }).click();
  await expect(page).toHaveURL(/\/painel\/impressao$/);
  await expect(page.getByRole("status").filter({ hasText: "Ao vivo" })).toBeVisible();
  const prints = () => page.evaluate(() => (window as unknown as { __prints: string[] }).__prints);
  return { page, prints, close: () => context.close() };
}

test("cada pedido sai impresso uma vez, vai para em preparo, e o cancelamento sai na via CANCELADO", async ({
  page,
  request,
  browser,
}) => {
  const { qr, email, headers } = await restaurant(request);
  const balcao = await station(browser, email);
  const outra = await station(browser, email);
  try {
    // O teste da impressora.
    await balcao.page.getByRole("button", { name: "Imprimir uma comanda de teste" }).click();
    await expect.poll(balcao.prints).toEqual([expect.stringContaining("COMANDA DE TESTE")]);

    // O pedido da mesa sai sozinho, numa estação só.
    await order(page, qr, ["Guioza", "Sunomono"]);
    await expect
      .poll(async () => [...(await balcao.prints()), ...(await outra.prints())].filter((p) => p.includes("MESA 04")))
      .toHaveLength(1);
    const printed = [...(await balcao.prints()), ...(await outra.prints())].find((p) => p.includes("MESA 04"))!;
    expect(printed).toContain("Salão principal");
    expect(printed).toContain("Pedido nº 1");
    expect(printed).toContain("1x Guioza");
    expect(printed).toContain("1x Sunomono");

    // Impressa, a comanda põe os itens em preparo, e a mesa vê.
    await expect(page.getByRole("article").filter({ hasText: "Em preparo" })).toHaveCount(2, { timeout: 5000 });

    // O garçom cancela um item já impresso: sai a via "CANCELADO", com o motivo.
    const queue = await (await request.get(`${API_URL}/api/v1/order-items`, { headers })).json();
    const sunomono = queue.content.find((i: { productName: string }) => i.productName === "Sunomono");
    await request.post(`${API_URL}/api/v1/order-items/${sunomono.id}/cancel`, {
      headers,
      data: { reason: "Acabou o pepino" },
    });
    await expect
      .poll(async () =>
        [...(await balcao.prints()), ...(await outra.prints())].filter((p) => p.includes("*** CANCELADO ***")),
      )
      .toEqual([expect.stringContaining("motivo: Acabou o pepino")]);
    // A mesa vê o aviso do prato cancelado pela equipe, com o motivo (desenho 4.3).
    await expect(page.getByText("Sunomono cancelado pela equipe:")).toBeVisible({ timeout: 5000 });
    await expect(page.getByText(/Acabou o pepino\. O valor não entra na conta\./)).toBeVisible();

    // A reimpressão, pela lista das impressas.
    await balcao.page.getByRole("button", { name: "Reimprimir a comanda da mesa 04, pedido nº 1" }).click();
    await expect.poll(async () => (await balcao.prints()).at(-1)).toContain("REIMPRESSÃO");
  } finally {
    await balcao.close();
    await outra.close();
  }
});

test("pausada, a estação guarda os pedidos e imprime ao voltar", async ({ page, request, browser }) => {
  const { qr, email } = await restaurant(request);
  const balcao = await station(browser, email);
  try {
    await balcao.page.getByRole("button", { name: "Pausar" }).click();
    await expect(balcao.page.getByText("Impressão pausada: os pedidos esperam aqui")).toBeVisible();

    await order(page, qr, ["Guioza"]);
    // Pausada: nada sai, e o item continua na fila.
    await page.waitForTimeout(3000);
    expect(await balcao.prints()).toEqual([]);
    await expect(page.getByRole("article", { name: "1× Guioza" })).toContainText("Na fila");

    await balcao.page.getByRole("button", { name: "Voltar a imprimir" }).click();
    await expect.poll(balcao.prints).toEqual([expect.stringContaining("1x Guioza")]);
  } finally {
    await balcao.close();
  }
});
