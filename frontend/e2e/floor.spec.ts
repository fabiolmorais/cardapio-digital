import { expect, test, type Page } from "@playwright/test";
import { createAccount, loginThroughForm } from "./support/account";

async function openFloor(page: Page) {
  await page.goto("/admin/mesas");
  await expect(page.getByRole("heading", { name: "Mesas e QR Codes" })).toBeVisible();
}

async function newSector(page: Page, name: string) {
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("Nome do setor").fill(name);
  await dialog.getByRole("button", { name: "Criar setor" }).click();
  await expect(dialog).toBeHidden();
}

async function newTable(page: Page, label: string) {
  await page.getByRole("button", { name: "Nova mesa" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("Número ou nome").fill(label);
  await dialog.getByRole("button", { name: "Criar mesa" }).click();
  await expect(dialog).toBeHidden();
}

test("dono monta o salão: setores, mesas e troca de QR", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await openFloor(page);

  // Sem setor ainda: a tela orienta a começar por ele.
  await page.getByRole("button", { name: "Criar o primeiro setor" }).click();
  await newSector(page, "Salão principal");
  for (const label of ["10", "2", "1"]) await newTable(page, label);

  // "1", "2", "10" nessa ordem, e o setor contando 3 mesas.
  await expect(page.getByRole("button", { name: /^Mesa / })).toHaveText(["Mesa 1", "Mesa 2", "Mesa 10"]);
  await expect(page.getByRole("button", { name: "Salão principal · 3" })).toBeVisible();
  // A situação de cada mesa (sem comanda: livre) e o uso do plano no topo.
  await expect(page.getByRole("row", { name: /Mesa 1\b/ })).toContainText("Livre");
  await expect(page.getByRole("meter", { name: "Mesas usadas no plano" })).toHaveAttribute("aria-valuenow", "3");

  // Rótulo repetido: o erro do back aparece no formulário.
  await page.getByRole("button", { name: "Nova mesa" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("Número ou nome").fill("2");
  await dialog.getByRole("button", { name: "Criar mesa" }).click();
  await expect(dialog.getByRole("alert")).toContainText("Já existe uma mesa \"2\"");
  await dialog.getByRole("button", { name: "Cancelar" }).click();

  // Painel da mesa e troca do QR com confirmação.
  await page.getByRole("button", { name: "Mesa 2" }).click();
  const panel = page.getByRole("complementary", { name: "Mesa 2" });
  await expect(panel).toContainText("QR ativo");
  await panel.getByRole("button", { name: "Trocar QR da mesa 2" }).click();
  await page.getByRole("alertdialog").getByRole("button", { name: "Trocar QR" }).click();
  await expect(page.getByRole("status").filter({ hasText: "QR trocado" })).toBeVisible();

  // Desativar a mesa pelo interruptor.
  const toggle = page.getByRole("switch", { name: "Mesa 2 ativa" });
  await toggle.click();
  await expect(toggle).toHaveAttribute("aria-checked", "false");
  await expect(page.getByRole("button", { name: "Salão principal · 2" })).toBeVisible();
});

test("setor com mesas ativas não pode ser desativado; desativado, aparece marcado e volta com as mesas", async ({
  page,
  request,
}) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await openFloor(page);
  await page.getByRole("button", { name: "Criar o primeiro setor" }).click();
  await newSector(page, "Deck");
  await newTable(page, "D1");

  await page.getByRole("button", { name: "Desativar setor" }).click();
  const confirm = page.getByRole("alertdialog");
  await confirm.getByRole("button", { name: "Desativar" }).click();
  await expect(confirm.getByRole("alert")).toHaveText("Este setor tem 1 mesa ativa. Mova ou desative a mesa antes.");
  await confirm.getByRole("button", { name: "Cancelar" }).click();

  await page.getByRole("switch", { name: "Mesa D1 ativa" }).click();
  await expect(page.getByRole("switch", { name: "Mesa D1 ativa" })).toHaveAttribute("aria-checked", "false");
  await page.getByRole("button", { name: "Desativar setor" }).click();
  await page.getByRole("alertdialog").getByRole("button", { name: "Desativar" }).click();

  // O setor não some: fica marcado como desativado, e a mesa mostra o nome dele.
  const deck = page.getByRole("button", { name: "Deck · desativado" });
  await expect(deck).toBeVisible();
  await expect(page.getByRole("row", { name: /Mesa D1/ })).toContainText("Deck");
  await expect(page.getByRole("button", { name: "Nova mesa" })).toHaveCount(0);

  // Ativar a mesa explica o que fazer, em vez de "Setor não encontrado".
  await page.getByRole("switch", { name: "Mesa D1 ativa" }).click();
  await expect(page.getByRole("alert").filter({ hasText: "O setor Deck está desativado" })).toContainText(
    "Reative o setor ou mova a mesa",
  );

  await deck.click();
  await expect(page.getByText("Este setor está desativado.")).toBeVisible();
  await page.getByRole("button", { name: "Reativar setor" }).click();
  await expect(page.getByRole("status")).toContainText("Setor Deck reativado.");
  await expect(page.getByRole("button", { name: "Deck · 0" })).toBeVisible();

  await page.getByRole("switch", { name: "Mesa D1 ativa" }).click();
  await expect(page.getByRole("switch", { name: "Mesa D1 ativa" })).toHaveAttribute("aria-checked", "true");
});
