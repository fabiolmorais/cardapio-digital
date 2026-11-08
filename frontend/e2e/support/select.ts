import type { Locator, Page } from "@playwright/test";

/** Escolhe uma opção num campo de escolha do sistema (components/ui/select): abre a lista e clica. */
export async function choose(page: Page, field: Locator, option: string) {
  await field.click();
  await page.getByRole("option", { name: option, exact: true }).click();
}
