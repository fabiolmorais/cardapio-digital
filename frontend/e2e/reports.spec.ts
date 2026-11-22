import { expect, test, type APIRequestContext, type Browser } from "@playwright/test";
import { API_URL, OWNER_NAME, apiLogin, createAccount, loginThroughForm, openMenu } from "./support/account";

/** Um restaurante com uma mesa (Salão, 04) e um prato, e o pedido do prato pelo celular da mesa. */
async function tableWithAnOrder(request: APIRequestContext, browser: Browser, email: string, quantity: number) {
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) =>
    (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  const sector = await post("/api/v1/sectors", { name: "Salão" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  const pratos = await post("/api/v1/menu-categories", { name: "Pratos" });
  const prato = await post("/api/v1/products", {
    categoryId: pratos.id,
    name: "Prato da casa",
    priceCents: 6890,
    featured: false,
    available: true,
  });
  const phone = await browser.newContext();
  const tableUrl = `${API_URL}/api/v1/public/tables/${table.publicToken}`;
  await phone.request.post(`${tableUrl}/session`);
  await phone.request.post(`${tableUrl}/presence`);
  const items = Array.from({ length: quantity }, () => ({ productId: prato.id, quantity: 1 }));
  const order = await (await phone.request.post(`${tableUrl}/orders`, { data: { items } })).json();
  await phone.close();
  return { headers, orderId: order.id as string, items: order.items as { id: string }[] };
}

/** Uma comanda de hoje, pedida pelo celular, entregue e fechada pela API (o dono entrega e fecha). */
async function closeOneTab(request: APIRequestContext, browser: Browser, email: string) {
  const { headers, items } = await tableWithAnOrder(request, browser, email, 1);
  for (const item of items) await request.post(`${API_URL}/api/v1/order-items/${item.id}/deliver`, { headers });
  const open = await (await request.get(`${API_URL}/api/v1/tabs/open`, { headers })).json();
  const closed = await request.post(`${API_URL}/api/v1/tabs/${open.content[0].id}/close`, {
    headers,
    data: { paymentMethod: "PIX" },
  });
  expect(closed.ok(), await closed.text()).toBe(true);
}

test("o dono abre os relatórios pelo menu da gestão e troca o período", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);

  await openMenu(page);
  await page.getByRole("link", { name: "Relatórios", exact: true }).click();
  await expect(page).toHaveURL(/\/admin\/relatorios$/);
  await expect(page.getByRole("heading", { name: "Visão geral", level: 1 })).toBeVisible();
  // O teste grátis é do Pro: relatórios básicos.
  await expect(page.getByText("Plano Pro · relatórios básicos")).toBeVisible();

  const periods = page.getByRole("group", { name: "Período" });
  await expect(periods.getByRole("button", { name: "7 dias" })).toHaveAttribute("aria-pressed", "true");
  await expect(page.getByText(/^Últimos 7 dias · .+ · comparado com .+$/)).toBeVisible();
  // Restaurante novo: nenhuma comanda fechada ainda.
  await expect(page.getByRole("heading", { name: "Ainda não há comandas fechadas neste período" })).toBeVisible();

  // O período fica no endereço: dá para voltar ou mandar o link.
  await periods.getByRole("button", { name: "30 dias" }).click();
  await expect(page).toHaveURL(/\?periodo=30d$/);
  await expect(periods.getByRole("button", { name: "30 dias" })).toHaveAttribute("aria-pressed", "true");
  await expect(page.getByText(/^Últimos 30 dias · /)).toBeVisible();
  await page.reload();
  await expect(page.getByText(/^Últimos 30 dias · /)).toBeVisible();

  // Hoje compara com o mesmo dia da semana passada.
  await periods.getByRole("button", { name: "Hoje" }).click();
  await expect(page.getByText(/^Hoje · .+-feira|^Hoje · (sábado|domingo)/)).toBeVisible();
});

test("com uma comanda fechada hoje, a Visão geral mostra os números do período", async ({ page, request, browser }) => {
  const { email } = await createAccount(request);
  await closeOneTab(request, browser, email);
  await loginThroughForm(page, email);

  await page.goto("/admin/relatorios?periodo=today");
  await expect(page.getByRole("heading", { name: "Visão geral", level: 1 })).toBeVisible();
  const numbers = page.getByRole("region", { name: "Visão geral" });
  await expect(numbers).toContainText("FaturamentoR$ 68,90");
  await expect(numbers).toContainText("Comandas1");
  await expect(numbers).toContainText("Tíquete médioR$ 68,90");
  // Nada na sexta (ou no dia) da semana passada: sem base para comparar.
  await expect(numbers).toContainText("sem dados para comparar");

  // O gráfico diz o valor ao tocar na coluna.
  const byDay = page.getByRole("region", { name: "Faturamento por dia" });
  await byDay.getByRole("button", { name: /R\$ 68,90/ }).click();
  await expect(byDay.getByRole("status")).toContainText("R$ 68,90");

  await expect(page.getByRole("region", { name: "Forma de pagamento" })).toContainText("Pix100%R$ 68,90");
  await expect(page.getByRole("region", { name: "Itens mais vendidos" })).toContainText("Prato da casa");
  await expect(page.getByRole("region", { name: "Quando o salão vende" }).getByRole("img")).toHaveAccessibleName(
    /Os maiores picos: /,
  );
});

test("Vendas pelo submenu: agrupa por semana, escolhe datas e filtra o setor", async ({ page, request, browser }) => {
  const { email } = await createAccount(request);
  await closeOneTab(request, browser, email);
  // Um segundo setor, sem vendas: aparece o filtro.
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  await request.post(`${API_URL}/api/v1/sectors`, { headers, data: { name: "Deck" } });
  await loginThroughForm(page, email);

  await page.goto("/admin/relatorios");
  await openMenu(page);
  await page.getByRole("group", { name: "Relatórios" }).getByRole("link", { name: "Vendas" }).click();
  await expect(page).toHaveURL(/\/admin\/relatorios\/vendas/);
  await expect(page.getByRole("heading", { name: "Vendas", level: 1 })).toBeVisible();
  const numbers = page.getByRole("region", { name: "Vendas" });
  await expect(numbers).toContainText("FaturamentoR$ 68,90");
  await expect(numbers).toContainText("Comandas por dia0,1");

  // Agrupar por semana fica no endereço.
  const groupBy = page.getByRole("group", { name: "Agrupar por" });
  await groupBy.getByRole("button", { name: "Semana" }).click();
  await expect(page).toHaveURL(/agrupar=semana/);
  await expect(page.getByRole("heading", { name: "Faturamento por semana" })).toBeVisible();

  // O setor sem vendas: os números zeram e os blocos avisam.
  await page.getByRole("combobox", { name: "Setor" }).selectOption({ label: "Deck" });
  await expect(page).toHaveURL(/setor=/);
  await expect(numbers).toContainText("FaturamentoR$ 0,00");
  await expect(page.getByText("Sem vendas neste período").first()).toBeVisible();
  await page.getByRole("combobox", { name: "Setor" }).selectOption({ label: "Todos os setores" });
  await expect(numbers).toContainText("FaturamentoR$ 68,90");

  // Escolher datas: "Ontem" vai para o endereço; antes da primeira comanda, não há dados.
  await page.getByRole("button", { name: "Escolher datas" }).click();
  const dialog = page.getByRole("dialog", { name: "Escolher datas" });
  await dialog.getByRole("button", { name: "Ontem" }).click();
  await expect(dialog.getByRole("status")).toContainText("1 dia");
  await dialog.getByRole("button", { name: "Aplicar" }).click();
  await expect(page).toHaveURL(/de=\d{4}-\d{2}-\d{2}&ate=\d{4}-\d{2}-\d{2}/);
  await expect(page.getByRole("button", { name: "Escolher datas" })).toHaveAttribute("aria-pressed", "true");
  await expect(page.getByRole("heading", { name: "Ainda não há comandas fechadas neste período" })).toBeVisible();
});

test("Cardápio pelo submenu: a tabela de produtos, a categoria e os que vendem pouco", async ({
  page,
  request,
  browser,
}) => {
  const { email } = await createAccount(request);
  await closeOneTab(request, browser, email);
  // Uma segunda categoria, sem vendas: aparece o filtro de categoria.
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  await request.post(`${API_URL}/api/v1/menu-categories`, { headers, data: { name: "Bebidas" } });
  await loginThroughForm(page, email);

  await page.goto("/admin/relatorios");
  await openMenu(page);
  await page.getByRole("group", { name: "Relatórios" }).getByRole("link", { name: "Cardápio" }).click();
  await expect(page).toHaveURL(/\/admin\/relatorios\/cardapio/);
  await expect(page.getByRole("heading", { name: "Cardápio", level: 1 })).toBeVisible();

  const products = page.getByRole("region", { name: "Produtos" });
  const row = products.getByRole("row", { name: /Prato da casa/ });
  await expect(row).toContainText("1");
  await expect(row).toContainText("R$ 68,90");
  await expect(row).toContainText("100%");
  await expect(products).toContainText("Mostrando 1 a 1 de 1 produto vendido. 1 produto ativo no cardápio.");
  await products.getByRole("button", { name: "Quantidade" }).click();
  await expect(products.getByRole("button", { name: "Quantidade" })).toHaveAttribute("aria-pressed", "true");
  await expect(page.getByRole("region", { name: "Por categoria" })).toContainText("Pratos");
  // O prato acabou de ser cadastrado: ainda não é candidato a sair.
  await expect(page.getByRole("region", { name: "Vendem pouco" })).toContainText(
    "Nenhum produto com mais de 30 dias no cardápio",
  );
  // No celular, a tabela rola dentro do bloco: a página não fica mais larga que a tela.
  // (O celular alarga a própria área de layout quando a página passa da tela: compara com a tela.)
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
    page.viewportSize()!.width,
  );

  // A categoria fica no endereço; sem vendas nela, a tabela avisa.
  await page.getByRole("combobox", { name: "Categoria" }).selectOption({ label: "Bebidas" });
  await expect(page).toHaveURL(/categoria=/);
  await expect(products).toContainText("Nenhum item vendido neste período");
});

test("Cozinha pelo submenu: o preparo médio e os itens que mais demoram", async ({ page, request, browser }) => {
  const { email } = await createAccount(request);
  // A cozinha prepara 5 pratos: o mínimo para entrar nos que mais demoram.
  const { headers, orderId, items } = await tableWithAnOrder(request, browser, email, 5);
  // O pedido do celular vai para a cozinha quando o garçom confirma: o preparo conta dali.
  const approved = await request.post(`${API_URL}/api/v1/orders/${orderId}/approve`, { headers });
  expect(approved.ok(), await approved.text()).toBe(true);
  for (const item of items) {
    for (const step of ["start", "ready", "deliver"]) {
      const done = await request.post(`${API_URL}/api/v1/order-items/${item.id}/${step}`, { headers });
      expect(done.ok(), await done.text()).toBe(true);
    }
  }
  // Os relatórios contam a partir da primeira comanda fechada.
  const open = await (await request.get(`${API_URL}/api/v1/tabs/open`, { headers })).json();
  await request.post(`${API_URL}/api/v1/tabs/${open.content[0].id}/close`, { headers, data: { paymentMethod: "PIX" } });
  await loginThroughForm(page, email);

  await page.goto("/admin/relatorios");
  await openMenu(page);
  await page.getByRole("group", { name: "Relatórios" }).getByRole("link", { name: "Cozinha e tempos" }).click();
  await expect(page).toHaveURL(/\/admin\/relatorios\/cozinha$/);
  await expect(page.getByRole("heading", { name: "Cozinha e tempos", level: 1 })).toBeVisible();

  const numbers = page.getByRole("region", { name: "Cozinha e tempos" });
  await expect(numbers).toContainText("Itens preparados5");
  await expect(numbers).toContainText(/Preparo médio\d+ s/);
  // Nada na semana anterior: sem base para comparar.
  await expect(numbers).toContainText("sem dados para comparar");
  const slowest = page.getByRole("region", { name: "Itens que mais demoram" });
  await expect(slowest.getByRole("row", { name: /Prato da casa/ })).toBeVisible();
  await expect(slowest).toContainText("9 em 10 até");
  // Os 5 preparos foram na mesma hora de hoje: ela é o pior horário.
  await expect(slowest.getByRole("row", { name: /Prato da casa/ })).toContainText(
    /(Seg|Ter|Qua|Qui|Sex|Sáb|Dom), \d+h/,
  );
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
    page.viewportSize()!.width,
  );
});

test("Resumo diário pelo submenu: muda o horário e quem recebe, e manda um teste", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);

  await page.goto("/admin/relatorios");
  await openMenu(page);
  await page.getByRole("group", { name: "Relatórios" }).getByRole("link", { name: "Resumo diário" }).click();
  await expect(page).toHaveURL(/\/admin\/relatorios\/resumo$/);
  await expect(page.getByRole("heading", { name: "Resumo diário", level: 1 })).toBeVisible();

  // Começa ligado, às 2h, para o dono.
  const byEmail = page.getByRole("switch", { name: /Enviar por e-mail/ });
  const onPhone = page.getByRole("switch", { name: /Avisar no celular/ });
  await expect(byEmail).toBeChecked();
  await expect(onPhone).toBeChecked();
  await expect(page.getByRole("combobox", { name: "Horário" })).toHaveText(/2h, depois do fechamento/);
  const owner = page.getByRole("checkbox", { name: new RegExp(OWNER_NAME) });
  await expect(owner).toBeChecked();
  // Restaurante novo: ontem não teve comanda.
  await expect(page.getByText("Nenhuma comanda fechada neste dia: o resumo não sai.")).toBeVisible();

  await onPhone.uncheck();
  await page.getByRole("combobox", { name: "Horário" }).click();
  await page.getByRole("option", { name: "8h", exact: true }).click();
  await page.getByRole("button", { name: "Salvar" }).click();
  await expect(page.getByRole("status").filter({ hasText: "Resumo diário salvo." })).toBeVisible();
  await page.reload();
  await expect(page.getByRole("switch", { name: /Avisar no celular/ })).not.toBeChecked();
  await expect(page.getByRole("combobox", { name: "Horário" })).toHaveText(/^8h$/);

  // O teste vai para o e-mail do dono (sem a chave do Resend, só fica no log do back).
  await page.getByRole("button", { name: "Enviar um teste para mim" }).click();
  await expect(page.getByRole("status").filter({ hasText: "Enviado para o seu e-mail." })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
    page.viewportSize()!.width,
  );
});

test("no Pro, a Visão geral mostra os blocos do Business bloqueados", async ({ page, request, browser }) => {
  const { email } = await createAccount(request);
  await closeOneTab(request, browser, email);
  await loginThroughForm(page, email);

  await page.goto("/admin/relatorios?periodo=today");
  await expect(page.getByRole("region", { name: "Visão geral" })).toContainText("FaturamentoR$ 68,90");
  for (const block of ["Mais 5 números", "Hoje, até agora", "Garçons do período", "Pontos de atenção"]) {
    const locked = page.getByRole("region", { name: `${block} (disponível no Business)` });
    await expect(locked).toBeVisible();
    await expect(locked.getByRole("link", { name: "Conhecer o Business" })).toHaveAttribute(
      "href",
      "/admin/assinatura",
    );
  }
});

test("no Business, a Visão geral mostra a operação, hoje até agora, os garçons e os pontos de atenção", async ({
  page,
  request,
  browser,
}) => {
  const { email } = await createAccount(request);
  await closeOneTab(request, browser, email);
  // O plano vem do back; aqui, o nível do Business e os blocos dele, como o back os manda.
  await page.route("**/api/v1/reports/context", async (route) => {
    const response = await route.fetch();
    await route.fulfill({ response, json: { ...(await response.json()), level: "FULL" } });
  });
  const operation = {
    toTableSeconds: 1080,
    phoneOrders: 68,
    staffOrders: 32,
    phoneSharePercent: 68,
    callResponseSeconds: 102,
    staySeconds: 5640,
    canceledCents: 61200,
  };
  await page.route("**/api/v1/reports/overview?**", async (route) => {
    const response = await route.fetch();
    await route.fulfill({
      response,
      json: {
        ...(await response.json()),
        level: "FULL",
        full: {
          current: operation,
          compared: { ...operation, toTableSeconds: 1200, phoneSharePercent: 63, callResponseSeconds: 90 },
          today: { soFarCents: 612000, expectedCents: 940000, usualCents: 584000, weeks: 4 },
          waiters: [{ userId: crypto.randomUUID(), name: "Carlos", calls: 168, averageResponseSeconds: 81 }],
          attention: [
            { kind: "SLOW_ITEM", name: "Guioza", dayAverageSeconds: 1860, usualAverageSeconds: 840 },
            { kind: "FORGOTTEN_TABS", tables: ["07", "D3"] },
            { kind: "CANCELED_PRODUCT", name: "Temaki salmão", times: 9, reason: "Acabou o salmão", reasonTimes: 7 },
          ],
        },
      },
    });
  });
  await loginThroughForm(page, email);

  await page.goto("/admin/relatorios?periodo=today");
  const numbers = page.getByRole("region", { name: "Visão geral" });
  await expect(numbers).toContainText("Tempo até a mesa18 min▼ 2 min, mais rápido");
  await expect(numbers).toContainText("Pedidos pelo celular68%▲ 5 pontos");
  await expect(numbers).toContainText("Resposta ao chamado1 min 42 s▲ 12 s, mais lento");
  await expect(numbers).toContainText("Permanência média1 h 34 min");
  await expect(numbers).toContainText("Valor cancelado");
  const today = page.getByRole("region", { name: "Hoje, até agora" });
  await expect(today).toContainText("R$ 6.120,00 de R$ 9.400,00 previstos");
  await expect(today).toContainText("Nesse horário,");
  await expect(today).toContainText("hoje está 4,8% acima.");
  await expect(page.getByRole("region", { name: "Garçons do período" })).toContainText("Carlos1681 min 21 s");
  const attention = page.getByRole("region", { name: "Pontos de atenção" });
  await expect(attention).toContainText("Guioza levou 31 min em média (normal: 14 min).");
  await expect(attention).toContainText("2 comandas ficaram abertas de um dia para o outro (Mesas 07, D3).");
  await expect(attention).toContainText('Temaki salmão foi cancelado 9 vezes, 7 por "Acabou o salmão".');
  await expect(page.getByText("Conhecer o Business")).toHaveCount(0);
});

test("em Vendas, o Pro vê os blocos do Business bloqueados e o Business vê setores, mesas, tíquete e cancelamentos", async ({
  page,
  request,
  browser,
}) => {
  const { email } = await createAccount(request);
  await closeOneTab(request, browser, email);
  await loginThroughForm(page, email);

  await page.goto("/admin/relatorios/vendas?periodo=today");
  for (const block of [
    "Mais 2 números",
    "Por setor",
    "Mesas que mais vendem",
    "Tíquete médio",
    "Pagamentos e cancelamentos",
  ]) {
    await expect(page.getByRole("region", { name: `${block} (disponível no Business)` })).toBeVisible();
  }

  // O plano vem do back; aqui, o nível do Business e os blocos dele, como o back os manda.
  await page.route("**/api/v1/reports/context", async (route) => {
    const response = await route.fetch();
    await route.fulfill({ response, json: { ...(await response.json()), level: "FULL" } });
  });
  await page.route("**/api/v1/reports/sales?**", async (route) => {
    const response = await route.fetch();
    const body = await response.json();
    await route.fulfill({
      response,
      json: {
        ...body,
        level: "FULL",
        full: {
          bySector: [
            { sectorId: crypto.randomUUID(), name: "Salão", tables: 12, revenueCents: 6890, perTablePerDayCents: 574 },
          ],
          topTables: [
            {
              tableId: crypto.randomUUID(),
              label: "04",
              sector: "Salão",
              tabs: 1,
              averageTicketCents: 6890,
              revenueCents: 6890,
            },
          ],
          rodizio: { revenueCents: 31660, people: 2, averageCents: 15830 },
          comparedRodizio: { revenueCents: 31320, people: 2, averageCents: 15660 },
          people: { revenueCents: 0, people: 0 },
          comparedPeople: { revenueCents: 0, people: 0 },
          advanceTabs: 1,
          advanceCents: 3000,
          canceledCents: 2500,
          comparedCanceledCents: 0,
          reasons: [{ reason: "Acabou o ingrediente", amountCents: 1500 }, { amountCents: 1000 }],
        },
      },
    });
  });
  await page.reload();

  const numbers = page.getByRole("region", { name: "Vendas" });
  await expect(numbers).toContainText("Pago adiantadoR$ 30,001 mesa");
  await expect(numbers).toContainText("CanceladoR$ 25,00");
  const sectors = page.getByRole("region", { name: "Por setor" });
  await expect(sectors).toContainText("Salão (12 mesas)");
  await expect(sectors).toContainText("Por mesa, por dia · SalãoR$ 5,74");
  await expect(
    page.getByRole("region", { name: "Mesas que mais vendem" }).getByRole("row", { name: /Mesa 04/ }),
  ).toContainText("R$ 68,90");
  const ticket = page.getByRole("region", { name: "Tíquete médio" });
  await expect(ticket).toContainText("Por pessoa · rodízio▲ 1,1%R$ 158,30");
  await expect(ticket).toContainText("Por pessoa · fora do rodíziosem dado");
  const payments = page.getByRole("region", { name: "Pagamentos e cancelamentos" });
  await expect(payments).toContainText("Mesas que pagaram adiantado1 · R$ 30,00");
  await expect(payments).toContainText("Acabou o ingrediente");
  await expect(payments).toContainText("Outros");
  await expect(page.getByText("Conhecer o Business")).toHaveCount(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
    page.viewportSize()!.width,
  );
});

test("no Cardápio, o Pro vê os blocos do Business bloqueados e o Business vê a curva ABC e os blocos", async ({
  page,
  request,
  browser,
}) => {
  const { email } = await createAccount(request);
  await closeOneTab(request, browser, email);
  await loginThroughForm(page, email);

  await page.goto("/admin/relatorios/cardapio?periodo=today");
  const products = page.getByRole("region", { name: "Produtos" });
  await expect(products.getByRole("row", { name: /Prato da casa/ })).toBeVisible();
  await expect(products.getByRole("columnheader", { name: "Curva" })).toHaveCount(0);
  await expect(products.getByRole("button", { name: "Crescimento" })).toHaveCount(0);
  for (const block of [
    "Mais cancelados",
    "Tamanhos",
    "Opções e adicionais",
    "Pedidos juntos",
    "Com vídeo x sem vídeo",
  ]) {
    await expect(page.getByRole("region", { name: `${block} (disponível no Business)` })).toBeVisible();
  }

  // O plano vem do back; aqui, o nível do Business e os dados dele, como o back os manda.
  await page.route("**/api/v1/reports/context", async (route) => {
    const response = await route.fetch();
    await route.fulfill({ response, json: { ...(await response.json()), level: "FULL" } });
  });
  await page.route("**/api/v1/reports/menu/products?**", async (route) => {
    const response = await route.fetch();
    const body = await response.json();
    await route.fulfill({
      response,
      json: {
        ...body,
        content: body.content.map((p: object) => ({ ...p, abc: "A", comparedRevenueCents: 6000, canceled: 2 })),
      },
    });
  });
  await page.route("**/api/v1/reports/menu?**", async (route) => {
    const response = await route.fetch();
    await route.fulfill({
      response,
      json: {
        ...(await response.json()),
        level: "FULL",
        full: {
          mostCanceled: [{ name: "Temaki salmão", times: 9, reason: "Acabou o salmão", reasonTimes: 7 }],
          sizes: [
            {
              product: "Chopp",
              sizes: [
                { name: "500 ml", quantity: 247 },
                { name: "300 ml", quantity: 151 },
              ],
            },
          ],
          options: [
            { group: "Extras", name: "Cream cheese extra", times: 212, chargedCents: 106000, priced: true },
            { group: "Extras", name: "Gengibre extra", times: 96, chargedCents: 0, priced: true },
            { group: "Ponto", name: "Mal passado", times: 41, chargedCents: 0, priced: false },
          ],
          paidOptionsCents: 106000,
          pairs: [{ first: "Guioza", second: "Chopp 500 ml", firstTabs: 100, together: 41 }],
          video: { withVideo: 12, withVideoSold: 732, withoutVideo: 52, withoutVideoSold: 1976 },
        },
      },
    });
  });
  await page.reload();

  const row = products.getByRole("row", { name: /Prato da casa/ });
  await expect(row).toContainText("curva A");
  await expect(row).toContainText("▲ 14,8%");
  await expect(products.getByRole("button", { name: "Crescimento" })).toBeVisible();
  await expect(page.getByRole("region", { name: "Mais cancelados" })).toContainText('7 de 9: "Acabou o salmão"');
  await expect(page.getByRole("region", { name: "Tamanhos" })).toContainText("500 ml 62% · 300 ml 38%");
  const options = page.getByRole("region", { name: "Opções e adicionais" });
  await expect(options).toContainText("Cream cheese extra212×R$ 1.060,00");
  await expect(options).toContainText("Gengibre extra96×grátis");
  await expect(options).toContainText("Mal passado41×—");
  await expect(page.getByRole("region", { name: "Pedidos juntos" })).toContainText(
    "Guioza + Chopp 500 mlem 41% das mesas",
  );
  await expect(page.getByRole("region", { name: "Com vídeo x sem vídeo" })).toContainText("61 por item");
  await expect(page.getByText("Conhecer o Business")).toHaveCount(0);
});

test("no Essencial, os relatórios mostram o que o Pro e o Business trazem", async ({ page, request }) => {
  const { email } = await createAccount(request);
  // O plano vem do back; aqui, o mesmo contexto com o nível do Essencial.
  await page.route("**/api/v1/reports/context", async (route) => {
    const response = await route.fetch();
    await route.fulfill({ response, json: { ...(await response.json()), level: "NONE", canExport: false } });
  });
  await loginThroughForm(page, email);

  await page.goto("/admin/relatorios");
  await expect(page.getByRole("heading", { name: "Relatórios", level: 1 })).toBeVisible();
  await expect(page.getByText("Seu plano é o Essencial: o resumo do dia continua no Ao vivo.")).toBeVisible();
  await expect(page.getByRole("region", { name: "Pro · relatórios básicos" })).toContainText(
    "Forma de pagamento, para bater o caixa",
  );
  await expect(page.getByRole("region", { name: "Business · relatórios completos" })).toContainText("Exportar em CSV");
  await expect(page.getByRole("link", { name: "Mudar para o Pro" })).toHaveAttribute("href", "/admin/assinatura");
});
