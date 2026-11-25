import { expect, test, type Page } from "@playwright/test";
import { API_URL, createAccount, loginThroughForm, openMenu } from "./support/account";
import { choose } from "./support/select";

const MOEMA = "7d2c4f6e-1b3a-4c5d-8e9f-0a1b2c3d4e5f";

test("no Pro, Unidades mostra a unidade de agora e o convite do Enterprise, sem seletor", async ({ page, request }) => {
  const { email, establishmentName } = await createAccount(request);
  await loginThroughForm(page, email);

  // Uma unidade só: o topo do menu é o nome, sem botão de troca.
  await expect(page.getByRole("button", { name: /Trocar de unidade/ })).toHaveCount(0);
  await openMenu(page);
  await expect(page.getByRole("link", { name: "Unidades" })).toBeVisible();

  await page.goto("/admin/unidades");
  await expect(page.getByRole("heading", { name: "Unidades", level: 1 })).toBeVisible();
  const list = page.getByRole("region", { name: "Unidades da empresa" });
  await expect(list.getByRole("listitem")).toHaveCount(1);
  await expect(list).toContainText(establishmentName);
  await expect(list).toContainText("Você está aqui");
  await expect(page.getByText("1 de 1")).toBeVisible();
  const locked = page.getByRole("region", { name: "Várias unidades (disponível no Enterprise)" });
  await expect(locked.getByRole("link", { name: "Conhecer o Enterprise" })).toHaveAttribute(
    "href",
    "/admin/assinatura",
  );
  await expect(page.getByRole("button", { name: "Criar unidade" })).toHaveCount(0);
});

/**
 * O Enterprise, como o back responde a ele: a segunda unidade no /me, a lista e o limite. A
 * troca de verdade (sessão nova na outra unidade, o 404 de outra empresa) está nos testes do back.
 */
async function enterprise(page: Page, home: { id: string; name: string }) {
  const moema = { id: MOEMA, name: "Sakura Moema", slug: "sakura-moema", timeZone: "America/Sao_Paulo", role: "OWNER" };
  await page.route("**/api/v1/me", async (route) => {
    const response = await route.fetch();
    const me = await response.json();
    await route.fulfill({ response, json: { ...me, establishments: [...me.establishments, moema] } });
  });
  await page.route("**/api/v1/units/allowance", (route) =>
    route.fulfill({ json: { planName: "Enterprise", used: 2, max: null, canAddMore: true } }),
  );
  await page.route("**/api/v1/units?**", (route) =>
    route.fulfill({
      json: {
        content: [
          {
            ...home,
            slug: "sakura",
            createdAt: "2026-03-12T15:00:00Z",
            activeTables: 24,
            activeTeam: 11,
            current: true,
          },
          {
            id: MOEMA,
            name: "Sakura Moema",
            slug: "sakura-moema",
            createdAt: "2026-08-02T15:00:00Z",
            activeTables: 18,
            activeTeam: 8,
            current: false,
          },
        ],
        page: 0,
        size: 20,
        totalElements: 2,
        totalPages: 1,
      },
    }),
  );
}

test("no Enterprise, o dono troca de unidade pelo menu e cria uma unidade nova", async ({ page, request }) => {
  const { email, establishmentName } = await createAccount(request);
  await loginThroughForm(page, email);
  const homeId = await page.evaluate(async (api) => {
    const r = await fetch(`${api}/api/v1/auth/refresh`, { method: "POST", credentials: "include" });
    return (await r.json()).establishment.id as string;
  }, API_URL);
  await enterprise(page, { id: homeId, name: establishmentName });
  await page.reload();

  // O seletor (desenho 12.10): as unidades com o papel, o ✓ na de agora e "Gerenciar unidades".
  const switcher = page.getByRole("button", { name: `Unidade: ${establishmentName}. Trocar de unidade` });
  await switcher.click();
  const mine = page.getByRole("list", { name: "Suas unidades" });
  await expect(mine.getByRole("button")).toHaveCount(2);
  await expect(mine.getByRole("button", { name: new RegExp(establishmentName) })).toHaveAttribute(
    "aria-current",
    "true",
  );
  await expect(mine.getByRole("button", { name: /Sakura Moema/ })).toContainText("Dono");

  // A troca recusada (a unidade sumiu, por exemplo) avisa e deixa a pessoa onde está.
  let asked: unknown = null;
  await page.route("**/api/v1/auth/switch", async (route) => {
    asked = route.request().postDataJSON();
    await route.fulfill({ status: 404, json: { detail: "Unidade não encontrada" } });
  });
  await mine.getByRole("button", { name: /Sakura Moema/ }).click();
  await expect(page.getByRole("alert").filter({ hasText: "Não foi possível trocar de unidade" })).toBeVisible();
  expect(asked).toEqual({ establishmentId: MOEMA });

  await page.getByRole("link", { name: "Gerenciar unidades" }).click();
  await expect(page).toHaveURL(/\/admin\/unidades$/);
  const list = page.getByRole("region", { name: "Unidades da empresa" });
  await expect(list.getByRole("listitem")).toHaveCount(2);
  await expect(list).toContainText("sakura-moema · desde 02/08/2026");
  await expect(page.getByText("2 · sem limite")).toBeVisible();

  // A unidade nova: nome e fuso; o pedido leva os dois.
  let created: unknown = null;
  await page.route("**/api/v1/units", async (route) => {
    if (route.request().method() !== "POST") return route.fallback();
    created = route.request().postDataJSON();
    await route.fulfill({
      status: 201,
      json: {
        id: "0f1e2d3c-4b5a-4968-8776-a5b4c3d2e1f0",
        name: "Sakura Manaus",
        slug: "sakura-manaus",
        createdAt: new Date().toISOString(),
        activeTables: 0,
        activeTeam: 1,
        current: false,
      },
    });
  });
  const form = page.getByRole("form", { name: "Nova unidade" });
  await form.getByLabel("Nome da unidade").fill("Sakura Manaus");
  await choose(page, form.getByLabel("Fuso horário"), "Amazonas (Manaus)");
  await form.getByRole("button", { name: "Criar unidade" }).click();
  await expect(form.getByRole("status")).toContainText("Unidade Sakura Manaus criada.");
  expect(created).toEqual({ name: "Sakura Manaus", timeZone: "America/Manaus" });

  // Entrar em outra unidade abre o "Ao vivo" de novo (aqui o back devolve uma sessão nova).
  await page.unroute("**/api/v1/auth/switch");
  await page.route("**/api/v1/auth/switch", async (route) => {
    const response = await route.fetch({ url: `${API_URL}/api/v1/auth/refresh`, postData: "" });
    await route.fulfill({ response });
  });
  await list.getByRole("button", { name: "Entrar na unidade Sakura Moema" }).click();
  await expect(page).toHaveURL(/\/painel$/, { timeout: 15_000 });
});
