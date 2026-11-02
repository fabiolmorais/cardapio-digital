import { expect, test, type APIRequestContext } from "@playwright/test";
import { API_URL, apiLogin, createAccount } from "./support/account";

/**
 * Um restaurante montado pela API, como o dono deixaria: marca, uma mesa e um cardápio com
 * promoção, destaque, um item esgotado e um desativado. O "cliente" só tem o token do QR.
 */
async function sakura(request: APIRequestContext) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) => (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/establishment/brand`, {
    headers,
    data: {
      name: "Sakura Sushi",
      color: "PETROL",
      theme: "DARK",
      instagram: "sakurasushi.sp",
      googleReviewUrl: "https://g.page/r/CbX0exemplo/review",
    },
  });
  const sector = await post("/api/v1/sectors", { name: "Salão principal" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  const entradas = await post("/api/v1/menu-categories", { name: "Entradas" });
  const sobremesas = await post("/api/v1/menu-categories", { name: "Sobremesas" });
  await post("/api/v1/menu-categories", { name: "Vazia" });
  const product = (categoryId: string, name: string, priceCents: number, extra: object = {}) =>
    post("/api/v1/products", { categoryId, name, priceCents, featured: false, available: true, ...extra });
  await product(entradas.id, "Guioza", 2990, { description: "Pastel japonês selado na chapa." });
  await product(entradas.id, "Sunomono", 1990, { promoPriceCents: 1590, featured: true });
  const ceviche = await product(entradas.id, "Ceviche", 3490);
  await request.patch(`${API_URL}/api/v1/products/${ceviche.id}/availability`, { headers, data: { available: false } });
  const antigo = await product(entradas.id, "Prato antigo", 100);
  await request.delete(`${API_URL}/api/v1/products/${antigo.id}`, { headers });
  await product(sobremesas.id, "Açaí na tigela", 2200);
  return { qr: table.publicToken as string };
}

test("a mesa abre com a marca, os botões, os destaques e os links do restaurante", async ({ page, request }) => {
  const { qr } = await sakura(request);

  await page.goto(`/m/${qr}`);

  await expect(page.getByText("Sakura Sushi").first()).toBeVisible();
  await expect(page.getByText("Mesa 04 · Salão principal")).toBeVisible();
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Peça semesperar.");
  // Os dois botões da mesa (o fluxo dos chamados está em calls.spec.ts).
  await expect(page.getByRole("button", { name: /Chamar garçom/ })).toBeEnabled();
  await expect(page.getByRole("link", { name: /Pedir a conta/ })).toHaveAttribute("href", `/m/${qr}/conta`);
  // Destaques: só o Sunomono (destaque e disponível), no preço da promoção.
  const destaques = page.getByRole("region", { name: "Destaques" });
  await expect(destaques.getByRole("listitem")).toHaveCount(1);
  await expect(destaques).toContainText("Sunomono");
  await expect(destaques).toContainText("R$ 15,90");
  // Categorias sem produto não aparecem.
  await expect(page.getByRole("navigation", { name: "Categorias" }).getByRole("link")).toHaveText([
    "Entradas",
    "Sobremesas",
  ]);
  // Links para fora: outra aba, sem levar o endereço da mesa (que tem o token).
  const instagram = page.getByRole("link", { name: /Siga no Instagram/ });
  await expect(instagram).toHaveAttribute("href", "https://instagram.com/sakurasushi.sp");
  await expect(instagram).toHaveAttribute("rel", "noopener noreferrer");
  await expect(page.getByRole("link", { name: /Avalie no Google/ })).toHaveAttribute(
    "href",
    "https://g.page/r/CbX0exemplo/review",
  );

  // A página abriu a sessão da mesa: o cookie HttpOnly da API está no navegador (ele só vale
  // para /api/v1/public, então é pedindo um endereço desse caminho que ele aparece).
  await expect
    .poll(async () =>
      (await page.context().cookies(`${API_URL}/api/v1/public/tables`)).find((c) => c.name === "table_session"),
    )
    .toMatchObject({ httpOnly: true, sameSite: "Strict", path: "/api/v1/public" });
});

test("o cardápio tem as seções, o de/por, o esgotado e a busca sem acento", async ({ page, request }) => {
  const { qr } = await sakura(request);
  await page.goto(`/m/${qr}`);
  await page.getByRole("link", { name: "Cardápio", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Cardápio", level: 1 })).toBeVisible();

  await expect(page.getByRole("heading", { level: 2 })).toHaveText(["Entradas", "Sobremesas"]);
  const entradas = page.getByRole("region", { name: "Entradas" });
  await expect(entradas.getByRole("heading", { level: 3 })).toHaveText(["Guioza", "Sunomono", "Ceviche"]);
  const sunomono = entradas.getByRole("article").filter({ hasText: "Sunomono" });
  await expect(sunomono).toContainText("−20%");
  await expect(sunomono).toContainText("R$ 15,90");
  await expect(sunomono.locator("s")).toContainText("R$ 19,90");
  const ceviche = entradas.getByRole("article").filter({ hasText: "Ceviche" });
  await expect(ceviche).toHaveAttribute("aria-disabled", "true");
  await expect(ceviche).toContainText("Esgotado hoje");
  await expect(page.getByText("Prato antigo")).toHaveCount(0);

  // A busca não liga para acento nem maiúscula, e procura também na descrição.
  const search = page.getByRole("searchbox", { name: "Buscar no cardápio" });
  await search.fill("ACAI");
  await expect(page.getByText("1 item encontrado")).toBeVisible();
  await expect(page.getByRole("heading", { level: 3 })).toHaveText(["Açaí na tigela"]);
  await search.fill("chapa");
  await expect(page.getByRole("heading", { level: 3 })).toHaveText(["Guioza"]);
  await search.fill("pizza");
  await expect(page.getByText("Nada encontrado para “pizza”.")).toBeVisible();
});

test("o tema claro troca as cores da página", async ({ page, request }) => {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  await request.put(`${API_URL}/api/v1/establishment/brand`, {
    headers,
    data: { name: "Bar Claro", color: "INDIGO", theme: "LIGHT" },
  });
  const sector = await (await request.post(`${API_URL}/api/v1/sectors`, { headers, data: { name: "Deck" } })).json();
  const table = await (
    await request.post(`${API_URL}/api/v1/tables`, { headers, data: { label: "D1", sectorId: sector.id } })
  ).json();

  await page.goto(`/m/${table.publicToken}`);

  const highlight = page.getByRole("heading", { level: 1 }).locator("span");
  await expect(highlight).toHaveCSS("color", "rgb(55, 48, 163)"); // #3730A3, o tom do Anil no tema claro
  await expect(page.getByRole("button", { name: /Chamar garçom/ })).toHaveCSS("background-color", "rgb(55, 48, 163)");
});
