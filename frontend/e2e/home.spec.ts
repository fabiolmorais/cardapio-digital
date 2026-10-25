import { expect, test } from "@playwright/test";

test("página inicial mostra os planos vindos da API", async ({ page }) => {
  await page.goto("/");

  await expect(page.getByRole("heading", { level: 1 })).toContainText("Cardápio digital");

  const essential = page.getByTestId("plan-essential");
  await expect(essential).toContainText("Essencial");
  await expect(essential).toContainText("R$ 69,90");
  await expect(essential).toContainText("Até 20 mesas");

  await expect(page.getByTestId("plan-pro")).toContainText("Vídeos nos pratos");
  await expect(page.getByTestId("plan-enterprise")).toContainText("Sob consulta");
});
