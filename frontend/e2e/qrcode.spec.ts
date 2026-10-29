import { expect, test, type APIRequestContext } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

/** Restaurante com o setor "Salão principal" e a mesa 04, montados pela API. */
async function restaurantWithTable(request: APIRequestContext) {
  const { email, establishmentName } = await createAccount(request);
  const token = await apiLogin(request, email);
  const headers = { Authorization: `Bearer ${token}` };
  const sector = await (await request.post(`${API_URL}/api/v1/sectors`, { headers, data: { name: "Salão principal" } })).json();
  const table = await (
    await request.post(`${API_URL}/api/v1/tables`, { headers, data: { label: "04", sectorId: sector.id } })
  ).json();
  return { email, establishmentName, headers, table: table as { id: string; publicToken: string } };
}

test("painel mostra o QR da mesa e baixa PNG, PDF e o PDF do setor", async ({ page, request }) => {
  const { email } = await restaurantWithTable(request);
  await loginThroughForm(page, email);
  await page.goto("/admin/mesas");
  await page.getByRole("button", { name: "Mesa 04" }).click();

  await expect(page.getByRole("img", { name: "QR Code da mesa 04" })).toBeVisible();

  for (const [format, fileName] of [["PNG", "mesa-04.png"], ["PDF", "mesa-04.pdf"]] as const) {
    const download = page.waitForEvent("download");
    await page.getByRole("button", { name: `Baixar ${format} da mesa 04` }).click();
    expect((await download).suggestedFilename()).toBe(fileName);
  }
  const sectorPdf = page.waitForEvent("download");
  await page.getByRole("button", { name: "Baixar todos do setor Salão principal (PDF)" }).click();
  expect((await sectorPdf).suggestedFilename()).toBe("setor-salao-principal.pdf");
});

test("o QR abre a página da mesa certa, e o QR antigo para de abrir depois da troca", async ({ page, request }) => {
  const { establishmentName, headers, table } = await restaurantWithTable(request);

  await page.goto(`/m/${table.publicToken}`);
  await expect(page.getByRole("heading", { name: "Mesa 04" })).toBeVisible();
  await expect(page.getByText(establishmentName, { exact: false })).toBeVisible();

  const rotated = await (await request.post(`${API_URL}/api/v1/tables/${table.id}/token-rotation`, { headers })).json();

  await page.goto(`/m/${table.publicToken}`);
  await expect(page.getByRole("heading", { name: "Este QR Code não vale mais" })).toBeVisible();
  await page.goto(`/m/${rotated.publicToken}`);
  await expect(page.getByRole("heading", { name: "Mesa 04" })).toBeVisible();
});

test("token inventado mostra o aviso de QR inválido", async ({ page }) => {
  await page.goto("/m/token-que-ninguem-gerou-0000000");
  await expect(page.getByRole("heading", { name: "Este QR Code não vale mais" })).toBeVisible();
});
