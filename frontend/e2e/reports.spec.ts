import { expect, test, type APIRequestContext, type Browser } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm, openMenu } from "./support/account";

/** Uma comanda de hoje, pedida pelo celular, entregue e fechada pela API (o dono entrega e fecha). */
async function closeOneTab(request: APIRequestContext, browser: Browser, email: string) {
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
  const order = await (
    await phone.request.post(`${tableUrl}/orders`, { data: { items: [{ productId: prato.id, quantity: 1 }] } })
  ).json();
  await phone.close();
  for (const item of order.items) await request.post(`${API_URL}/api/v1/order-items/${item.id}/deliver`, { headers });
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
  await page.getByRole("link", { name: "Relatórios" }).click();
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
