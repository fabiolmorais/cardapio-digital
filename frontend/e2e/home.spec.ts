import { expect, test } from "@playwright/test";

test("a página inicial apresenta o OhChefe e mostra os planos vindos da API", async ({ page }) => {
  await page.goto("/");

  await expect(page).toHaveTitle(/OhChefe/);
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Ô, chefe!");

  const essential = page.getByTestId("plan-essential");
  await expect(essential).toContainText("Essencial");
  await expect(essential).toContainText("R$ 69,90");
  await expect(essential).toContainText("Até 20 mesas");

  const pro = page.getByTestId("plan-pro");
  await expect(pro).toContainText("Vídeos nos pratos");
  await expect(pro).toContainText("Teste grátis");
  await expect(page.getByTestId("plan-enterprise")).toContainText("Sob consulta");

  // No anual, o preço é o do ano, com o equivalente por mês.
  await page.getByRole("button", { name: /Anual/ }).click();
  await expect(essential).toContainText("R$ 699,00/ano");
  await expect(essential).toContainText("Equivale a R$ 58,25 por mês");
});

test("a página inicial leva ao teste grátis e ao login", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("banner").getByRole("link", { name: "Entrar" }).click();
  await expect(page).toHaveURL(/\/entrar$/);

  await page.goto("/");
  await page.getByRole("link", { name: "Testar grátis por 14 dias" }).first().click();
  await expect(page).toHaveURL(/\/cadastro$/);
});

test("as perguntas frequentes abrem e fecham", async ({ page }) => {
  await page.goto("/");
  const faq = page.getByRole("region", { name: "Ficou alguma dúvida?" });
  const answer = faq.getByText("A trava de presença confere");
  await expect(answer).toBeHidden();
  await faq.getByText("E se alguém tirar foto do QR Code e chamar de casa?").click();
  await expect(answer).toBeVisible();
});

test("a aba do navegador mostra o ícone do OhChefe", async ({ request }) => {
  const icon = await request.get("/favicon.ico");
  expect(icon.ok()).toBe(true);
  expect((await request.get("/apple-icon.png")).ok()).toBe(true);
});
