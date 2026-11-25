import { expect, test, type Page } from "@playwright/test";
import { createAccount, loginThroughForm, openMenu } from "./support/account";

test("sem o Enterprise, os relatórios não mostram Unidades, e o endereço explica", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await page.goto("/admin/relatorios");
  await openMenu(page);
  await expect(page.getByRole("link", { name: "Relatórios: Vendas" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Relatórios: Unidades" })).toHaveCount(0);

  await page.goto("/admin/relatorios/unidades");
  await expect(page.getByRole("heading", { name: "Unidades", level: 1 })).toBeVisible();
  await expect(page.getByRole("status")).toContainText("é do plano Enterprise");
  await expect(page.getByRole("link", { name: "Ver as unidades" })).toHaveAttribute("href", "/admin/unidades");
});

type Figures = {
  current: [number, number];
  compared: [number, number];
  prep: number | null;
  response: number | null;
  rating: number | null;
  ratings: number;
};

function figures(f: Figures) {
  const totals = ([revenueCents, tabs]: [number, number]) => ({
    revenueCents,
    tabs,
    averageTicketCents: tabs === 0 ? 0 : Math.round(revenueCents / tabs),
  });
  return {
    current: totals(f.current),
    compared: totals(f.compared),
    prepSeconds: f.prep ?? undefined,
    callResponseSeconds: f.response ?? undefined,
    ratingAverage: f.rating ?? undefined,
    ratings: f.ratings,
  };
}

const WEEKS = ["2026-10-27", "2026-11-02", "2026-11-09", "2026-11-16", "2026-11-23"];

function series(perDay: number[], tabs: number[]) {
  return WEEKS.map((start, i) => ({
    start,
    end: i === WEEKS.length - 1 ? "2026-11-25" : WEEKS[i + 1],
    revenuePerDayCents: perDay[i],
    tabsPerDay: tabs[i],
  }));
}

/** A comparação como o back a manda a um dono do Enterprise com três unidades (Moema, Pinheiros, Campinas). */
const COMPARISON = {
  period: { from: "2026-10-27", to: "2026-11-25" },
  comparison: { from: "2026-09-27", to: "2026-10-26" },
  grouping: "WEEK",
  units: [
    {
      id: "8b7f0d2e-6c1a-4b3e-9f5d-1a2b3c4d5e01",
      name: "Sakura Pinheiros",
      figures: figures({
        current: [18642000, 1512],
        compared: [17293000, 1450],
        prep: 720,
        response: 81,
        rating: 4.7,
        ratings: 412,
      }),
      series: series([590000, 605000, 620000, 640000, 660000], [48, 50, 50, 52, 54]),
    },
    {
      id: "8b7f0d2e-6c1a-4b3e-9f5d-1a2b3c4d5e02",
      name: "Sakura Moema",
      figures: figures({
        current: [14288000, 1230],
        compared: [12711000, 1180],
        prep: 840,
        response: 98,
        rating: 4.6,
        ratings: 301,
      }),
      series: series([430000, 450000, 470000, 490000, 510000], [37, 39, 41, 42, 44]),
    },
    {
      id: "8b7f0d2e-6c1a-4b3e-9f5d-1a2b3c4d5e03",
      name: "Sakura Campinas",
      figures: figures({
        current: [9831000, 905],
        compared: [10145000, 940],
        prep: 1020,
        response: null,
        rating: null,
        ratings: 3,
      }),
      series: series([345000, 335000, 325000, 320000, 310000], [32, 31, 30, 29, 29]),
    },
  ],
  network: figures({
    current: [42761000, 3647],
    compared: [40149000, 3570],
    prep: 840,
    response: 89,
    rating: 4.6,
    ratings: 716,
  }),
};

/** O Enterprise como o back responde a ele: duas unidades no /me e a comparação liberada no contexto. */
async function enterprise(page: Page) {
  await page.route("**/api/v1/me", async (route) => {
    const response = await route.fetch();
    const me = await response.json();
    const other = {
      id: COMPARISON.units[1].id,
      name: "Sakura Moema",
      slug: "sakura-moema",
      timeZone: "America/Sao_Paulo",
      role: "OWNER",
    };
    await route.fulfill({ response, json: { ...me, establishments: [...me.establishments, other] } });
  });
  await page.route("**/api/v1/reports/context", async (route) => {
    const response = await route.fetch();
    const context = await response.json();
    await route.fulfill({
      response,
      json: { ...context, level: "FULL", canExport: true, firstDataDay: undefined, unitComparison: true },
    });
  });
  await page.route("**/api/v1/reports/units?**", (route) => route.fulfill({ json: COMPARISON }));
}

test("no Enterprise, o dono compara as unidades lado a lado, ordena, vê a evolução e exporta", async ({
  page,
  request,
}) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await enterprise(page);
  await page.goto("/admin/relatorios?periodo=30d");
  await openMenu(page);
  await page.getByRole("link", { name: "Relatórios: Unidades" }).click();
  await expect(page).toHaveURL(/\/admin\/relatorios\/unidades/);
  await expect(page.getByRole("heading", { name: "Unidades", level: 1 })).toBeVisible();
  // Sem setor: cada unidade tem os seus.
  await expect(page.getByRole("combobox", { name: "Setor" })).toHaveCount(0);

  const network = page.getByRole("region", { name: "A rede no período" });
  await expect(network).toContainText("R$ 427.610,00");
  await expect(network).toContainText("3647");
  await expect(network).toContainText("4,6 de 5");
  await expect(network).toContainText("716 avaliações");

  // Lado a lado: pelo faturamento (o ranking), com a variação e o "melhor" de cada coluna.
  const table = page.getByRole("region", { name: "Lado a lado" });
  const names = table.locator("tbody th");
  await expect(names).toHaveText([/Sakura Pinheiros/, /Sakura Moema/, /Sakura Campinas/]);
  const pinheiros = table.getByRole("row", { name: /Sakura Pinheiros/ });
  await expect(pinheiros).toContainText("▲ 7,8%");
  await expect(pinheiros.getByText("R$ 186.420,00, o melhor")).toBeAttached();
  await expect(pinheiros).toContainText("12 min");
  await expect(table.getByRole("row", { name: /Sakura Campinas/ })).toContainText("▼ 3,1%");
  await expect(table.getByRole("row", { name: /Sakura Campinas/ })).toContainText("— · 3");
  await expect(table.getByRole("row", { name: /^Rede/ })).toContainText("R$ 427.610,00");

  // Ordenar pela resposta ao chamado: a mais rápida primeiro; quem não tem fica no fim.
  await table.getByRole("button", { name: "Ordenar por Resposta ao chamado" }).click();
  await expect(table.getByRole("columnheader", { name: /Resposta ao chamado/ })).toHaveAttribute(
    "aria-sort",
    "ascending",
  );
  await expect(names).toHaveText([/Sakura Pinheiros/, /Sakura Moema/, /Sakura Campinas/]);
  await table.getByRole("button", { name: "Ordenar por Preparo médio" }).click();
  await expect(names.first()).toHaveText(/Sakura Pinheiros/);
  await table.getByRole("button", { name: "Ordenar por Comandas" }).click();
  await expect(names.last()).toHaveText(/Sakura Campinas/);

  // A evolução: o faturamento por dia de cada unidade, e as comandas no outro botão.
  const evolution = page.getByRole("region", { name: "Evolução no período" });
  await expect(evolution.getByRole("img")).toHaveAccessibleName(
    /Sakura Campinas de R\$\s3\.450,00 para R\$\s3\.100,00/,
  );
  await evolution.getByRole("button", { name: "Comandas" }).click();
  await expect(evolution.getByRole("button", { name: "Comandas" })).toHaveAttribute("aria-pressed", "true");
  await expect(evolution.getByRole("img")).toHaveAccessibleName(/Sakura Moema de 37 para 44/);
  await expect(evolution).toContainText("A média por dia de cada semana");

  const share = page.getByRole("region", { name: "Parte no faturamento" });
  await expect(share.getByRole("img")).toHaveAccessibleName(
    /Sakura Pinheiros 44%, Sakura Moema 33%, Sakura Campinas 23%/,
  );

  // A exportação vai para a rota das unidades, só com as datas.
  const asked: URL[] = [];
  await page.route("**/api/v1/reports/units/export?**", async (route) => {
    asked.push(new URL(route.request().url()));
    await route.fulfill({ status: 200, contentType: "application/octet-stream", body: "conteúdo" });
  });
  await page.getByRole("button", { name: "Exportar" }).click();
  const download = page.waitForEvent("download");
  await page.getByRole("menuitem", { name: /Excel/ }).click();
  const url = asked[0] ?? new URL("http://x");
  expect((await download).suggestedFilename()).toBe(
    `ohchefe-unidades-${url.searchParams.get("from")}-a-${url.searchParams.get("to")}.xlsx`,
  );
  expect(url.searchParams.get("format")).toBe("XLSX");
  expect(url.searchParams.has("sectorId")).toBe(false);
});
