import { expect, test, type APIRequestContext } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

test.describe.configure({ timeout: 60_000 });

/** Um restaurante com duas categorias no cardápio (pela API). */
async function restaurant(request: APIRequestContext) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) => (await request.post(`${API_URL}${path}`, { headers, data })).json();
  const product = (categoryId: string, name: string) =>
    post("/api/v1/products", { categoryId, name, priceCents: 1990, featured: false, available: true });
  const entradas = await post("/api/v1/menu-categories", { name: "Entradas" });
  const sushis = await post("/api/v1/menu-categories", { name: "Sushis" });
  const guioza = await product(entradas.id, "Guioza");
  await product(entradas.id, "Sunomono");
  const salmao = await product(sushis.id, "Niguiri de salmão");
  const atum = await product(sushis.id, "Niguiri de atum");
  return { email, headers, ids: { guioza: guioza.id, salmao: salmao.id, atum: atum.id } };
}

test("o dono cadastra um rodízio com faixas, preço de fim de semana, produtos e regras", async ({ page, request }) => {
  const { email, headers, ids } = await restaurant(request);
  await loginThroughForm(page, email);
  await page.getByRole("link", { name: /^Rodízios/ }).click();
  await expect(page).toHaveURL(/\/admin\/rodizios$/);

  // Sem rodízio ainda: o formulário de um novo, já com Adulto e Criança.
  const editor = page.getByRole("region", { name: "Novo rodízio" });
  await expect(page.getByText("Nenhum rodízio ainda.")).toBeVisible();
  await expect(editor.getByLabel("Nome da faixa 1")).toHaveValue("Adulto");
  await expect(editor.getByLabel("Nome da faixa 2")).toHaveValue("Criança");

  await editor.getByLabel("Nome que o cliente vê").fill("Rodízio Japonês");
  await editor.getByLabel("O que está incluído (opcional)").fill("Peças à vontade");
  const adulto = editor.getByRole("group", { name: "Adulto", exact: true });
  await adulto.getByLabel("Preço padrão").fill("129,9");
  await adulto.getByLabel("Preço padrão").blur();
  await expect(adulto.getByLabel("Preço padrão")).toHaveValue("129,90");
  await editor.getByRole("group", { name: "Criança", exact: true }).getByLabel("Preço padrão").fill("64,90");

  // O preço especial começa em sexta e sábado, das 18h às 23h59.
  await adulto.getByRole("button", { name: "+ Preço especial por dia e horário" }).click();
  const weekend = adulto.getByRole("group", { name: "Adulto: Preço especial · sex e sáb" });
  await weekend.getByRole("button", { name: "domingo" }).click();
  const fullWeekend = adulto.getByRole("group", { name: "Adulto: Preço especial · sex a dom" });
  await fullWeekend.getByLabel("Preço", { exact: true }).fill("149,90");

  // Os produtos: um de Entradas e todos de Sushis.
  await editor.getByRole("button", { name: "Escolher produtos" }).click();
  await editor.getByRole("checkbox", { name: "Guioza" }).click();
  await editor.getByRole("button", { name: "Incluir todos de Sushis" }).click();
  await expect(editor.getByText("3 produtos incluídos")).toBeVisible();

  // Uma regra ligada; as outras ficam desligadas.
  await editor.getByRole("switch", { name: /Tempo limite do rodízio/ }).click();
  await expect(editor.getByLabel("Tempo limite do rodízio", { exact: true })).toHaveValue("120");
  await editor.getByRole("switch", { name: /Aviso de desperdício/ }).click();
  await editor.getByLabel("Aviso de desperdício", { exact: true }).fill("Sobras são cobradas à parte.");

  await page.getByRole("button", { name: "Criar rodízio" }).click();
  const saved = page.getByRole("region", { name: "Rodízio Japonês" });
  await expect(saved).toBeVisible();
  await expect(page.getByRole("navigation", { name: "Rodízios" }).getByRole("button", { name: "Rodízio Japonês" }))
    .toHaveAttribute("aria-current", "page");

  // O back guardou tudo.
  const list = await (await request.get(`${API_URL}/api/v1/rodizios`, { headers })).json();
  const rodizio = list.content[0];
  expect(rodizio).toMatchObject({
    name: "Rodízio Japonês",
    type: "A_LA_MINUTE",
    description: "Peças à vontade",
    rules: { timeLimitMinutes: 120, wasteNotice: "Sobras são cobradas à parte." },
    tiers: [
      {
        name: "Adulto",
        defaultPriceCents: 12990,
        prices: [{ startsAt: "18:00:00", endsAt: "23:59:00", priceCents: 14990 }],
      },
      { name: "Criança", defaultPriceCents: 6490, prices: [] },
    ],
  });
  expect(rodizio.tiers[0].prices[0].days.sort()).toEqual(["FRIDAY", "SATURDAY", "SUNDAY"]);
  expect(rodizio.productIds.sort()).toEqual([ids.guioza, ids.salmao, ids.atum].sort());
  expect(rodizio.rules.maxPerItemPerRound).toBeNull();

  // Reabre igual: o formulário volta com o que foi salvo.
  await expect(saved.getByRole("group", { name: "Adulto: Preço especial · sex a dom" }).getByLabel("Preço", { exact: true })).toHaveValue(
    "149,90",
  );
  await expect(saved.getByText("3 produtos incluídos")).toBeVisible();
});

test("o domínio recusa preços que se cruzam, e o rodízio desativado sai da lista", async ({ page, request }) => {
  const { email, headers } = await restaurant(request);
  await request.post(`${API_URL}/api/v1/rodizios`, {
    headers,
    data: {
      name: "Rodízio de Pizza",
      type: "A_LA_MINUTE",
      rules: {},
      tiers: [{ name: "Adulto", defaultPriceCents: 7990, prices: [] }],
      productIds: [],
    },
  });
  await loginThroughForm(page, email);
  await page.goto("/admin/rodizios");

  const editor = page.getByRole("region", { name: "Rodízio de Pizza" });
  const adulto = editor.getByRole("group", { name: "Adulto", exact: true });
  await expect(adulto.getByLabel("Preço padrão")).toHaveValue("79,90");

  // Preço vazio: o aviso sai antes de enviar.
  await adulto.getByLabel("Preço padrão").fill("");
  await page.getByRole("button", { name: "Salvar alterações" }).click();
  await expect(page.getByText("Confira os campos marcados.")).toBeVisible();
  await expect(adulto.getByText("Preço inválido")).toBeVisible();
  await adulto.getByLabel("Preço padrão").fill("79,90");

  // Dois preços especiais no sábado à noite: quem recusa é o domínio, com a frase dele.
  await adulto.getByRole("button", { name: "+ Preço especial por dia e horário" }).click();
  await adulto.getByRole("group", { name: /sex e sáb/ }).getByLabel("Preço", { exact: true }).fill("89,90");
  await adulto.getByRole("button", { name: "+ Preço especial por dia e horário" }).click();
  // Pela posição: o nome do grupo muda quando os dias mudam.
  const second = adulto.getByRole("group", { name: /Preço especial/ }).nth(1);
  await second.getByRole("button", { name: "sexta" }).click();
  await second.getByLabel("Das").fill("22:00");
  await second.getByLabel("Preço", { exact: true }).fill("99,90");
  await page.getByRole("button", { name: "Salvar alterações" }).click();
  await expect(page.getByRole("alert").filter({ hasText: "valem ao mesmo tempo" })).toBeVisible();

  await adulto.getByRole("group", { name: /· sáb$/ }).getByRole("button", { name: "Remover este preço especial" }).click();
  await page.getByRole("button", { name: "Salvar alterações" }).click();
  await expect(page.getByRole("status")).toHaveText("Rodízio salvo.");

  await page.getByRole("button", { name: "Desativar" }).click();
  await page.getByRole("alertdialog").getByRole("button", { name: "Desativar" }).click();
  await expect(page.getByText("Nenhum rodízio ainda.")).toBeVisible();
  const list = await (await request.get(`${API_URL}/api/v1/rodizios`, { headers })).json();
  expect(list.totalElements).toBe(0);
});
