import { expect, test } from "@playwright/test";
import { API_URL, apiLogin, createAccount, loginThroughForm } from "./support/account";

test.describe.configure({ timeout: 90_000 });

test("o dono vê o teste grátis no Pro e o uso de cada limite", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);

  await page.getByRole("link", { name: /Assinatura/ }).click();
  await expect(page.getByRole("heading", { name: "Assinatura", level: 1 })).toBeVisible();
  const plan = page.getByRole("region", { name: "Plano Pro" });
  await expect(plan).toContainText("Teste grátis");
  await expect(plan.getByRole("status")).toContainText(/Teste grátis até \d{2}\/\d{2}\/\d{4} · faltam 14 dias/);
  const usage = page.getByRole("region", { name: "Uso do plano" });
  await expect(usage).toContainText("Mesas ativas0 de 50");
  await expect(usage).toContainText("Pessoas na equipe1 · ilimitado");
  await expect(usage).toContainText("Setores0 de 3");
  await expect(usage).toContainText("Incluídos");
});

test("a 51ª mesa do Pro é barrada, com o atalho para a assinatura", async ({ page, request }) => {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const sector = await (await request.post(`${API_URL}/api/v1/sectors`, { headers, data: { name: "Salão" } })).json();
  for (let i = 1; i <= 50; i++) {
    const created = await request.post(`${API_URL}/api/v1/tables`, {
      headers,
      data: { label: String(i), sectorId: sector.id },
    });
    expect(created.status()).toBe(201);
  }

  await loginThroughForm(page, email);
  await page.goto("/admin/mesas");
  await page.getByRole("button", { name: "Nova mesa" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("Número ou nome").fill("51");
  await dialog.getByRole("button", { name: "Criar mesa" }).click();
  const alert = dialog.getByRole("alert");
  await expect(alert).toContainText("O plano Pro permite até 50 mesas ativas. Para ter mais, mude de plano.");

  await alert.getByRole("link", { name: "Ver a assinatura" }).click();
  await expect(page.getByRole("region", { name: "Uso do plano" })).toContainText("Mesas ativas50 de 50");
});

test("o dono escolhe o Essencial anual, e a escolha fica à espera do pagamento", async ({ page, request }) => {
  const { email } = await createAccount(request);
  await loginThroughForm(page, email);
  await page.goto("/admin/assinatura");

  const chooser = page.getByRole("region", { name: "Assinar ou mudar de plano" });
  await chooser.getByRole("button", { name: "Anual (2 meses grátis)" }).click();
  const essential = chooser.getByRole("listitem", { name: "Essencial" });
  await expect(essential).toContainText("R$ 699,00/ano");
  await expect(chooser.getByRole("listitem", { name: "Pro" })).toContainText("Plano atual");
  await expect(chooser.getByRole("listitem", { name: "Enterprise" })).toContainText("Sob consulta");

  await essential.getByRole("button", { name: "Escolher o Essencial" }).click();
  await expect(chooser.getByRole("status")).toContainText("Você escolheu o Essencial anual em");
  await expect(essential).toContainText("Escolhido");
  // Os limites continuam os do teste (Pro) até o pagamento.
  await expect(page.getByRole("region", { name: "Uso do plano" })).toContainText("Mesas ativas0 de 50");

  await page.reload();
  await expect(chooser.getByRole("status")).toContainText("Você escolheu o Essencial anual em");
});
