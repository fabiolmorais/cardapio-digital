import { expect, test, type Page } from "@playwright/test";
import { API_URL } from "./support/account";
import { addVideo, recordVideo, restaurant } from "./support/reels";

test.describe.configure({ timeout: 90_000 });

async function playing(page: Page, name: string) {
  return page
    .getByRole("region", { name })
    .locator("video")
    .first()
    .evaluate((v: HTMLVideoElement) => !v.paused);
}

test("o cliente abre o Reels pelo cardápio, assiste, desliza e adiciona ao pedido", async ({ page, request }) => {
  const video = await recordVideo(page);
  const { qr } = await restaurant(request, video);
  await page.goto(`/m/${qr}/cardapio`);

  await expect(page.getByRole("link", { name: "Ver Chá verde em fotos e vídeos" })).toHaveCount(0);
  await page.getByRole("link", { name: "Ver Guioza em fotos e vídeos" }).click();
  await expect(page).toHaveURL(/\/reels\?produto=/);

  const guioza = page.getByRole("region", { name: "Guioza" });
  await expect(guioza.getByRole("heading", { name: "Guioza" })).toBeVisible();
  await expect(guioza).toContainText("Destaque");
  // No topo, ao lado do nome, a logo do restaurante; sem logo, as iniciais na cor da marca.
  await expect(page.getByText("RL", { exact: true })).toBeVisible();
  await expect.poll(() => playing(page, "Guioza")).toBe(true);
  // E continua tocando (a barra de progresso atualizando a tela não pode pausar o vídeo).
  await page.waitForTimeout(1500);
  expect(await playing(page, "Guioza")).toBe(true);
  // A barra do topo enche conforme o vídeo passa (como no Instagram), em vez de vir cheia.
  const bar = guioza.locator("[aria-hidden] > span > span").first();
  await expect.poll(() => bar.evaluate((el) => parseFloat((el as HTMLElement).style.width))).toBeGreaterThan(0);
  expect(await bar.evaluate((el) => parseFloat((el as HTMLElement).style.width))).toBeLessThan(100);
  await page.getByRole("button", { name: "Ativar som" }).click();
  await expect(page.getByRole("button", { name: "Tirar o som" })).toHaveAttribute("aria-pressed", "true");

  // Desliza para o próximo prato: o vídeo dele toca, e o do anterior para.
  await page.getByRole("region", { name: "Sunomono" }).scrollIntoViewIfNeeded();
  await expect.poll(() => playing(page, "Sunomono")).toBe(true);
  await expect.poll(() => playing(page, "Guioza")).toBe(false);

  // O Sunomono é o último: deslizar mais uma vez volta ao primeiro, como se não tivesse fim.
  await page
    .getByRole("region", { name: "Sunomono" })
    .evaluate((el) => el.parentElement!.scrollBy({ top: el.clientHeight, behavior: "instant" }));
  await expect(page.getByRole("region", { name: "Guioza" })).toBeInViewport();
  await expect.poll(() => playing(page, "Guioza")).toBe(true);
  await expect.poll(() => playing(page, "Sunomono")).toBe(false);
  await page.getByRole("region", { name: "Sunomono" }).scrollIntoViewIfNeeded();
  await expect.poll(() => playing(page, "Sunomono")).toBe(true);

  await page.getByRole("button", { name: "Adicionar Sunomono" }).click();
  await page
    .getByRole("dialog", { name: "Sunomono" })
    .getByRole("button", { name: /^Adicionar/ })
    .click();
  await expect(page.getByRole("link", { name: /Ver pedido/ })).toContainText("R$ 19,90");

  await page.getByRole("link", { name: "Fechar os vídeos" }).click();
  await expect(page).toHaveURL(new RegExp(`/m/${qr}/cardapio$`));
  // Fechado o Reels, nenhum vídeo continua tocando (nem o som) escondido atrás do cardápio.
  await expect
    .poll(() => page.evaluate(() => [...document.querySelectorAll("video")].every((v) => v.paused)))
    .toBe(true);
});

test("o destaque com vídeo abre o Reels, tocar na borda passa a mídia, e fechar volta ao início", async ({
  page,
  request,
}) => {
  const video = await recordVideo(page);
  const { qr, headers, guiozaId } = await restaurant(request, video);
  await addVideo(request, headers, guiozaId, video); // o Guioza com dois vídeos
  await page.goto(`/m/${qr}`);

  await page
    .getByRole("region", { name: "Destaques" })
    .getByRole("link", { name: /Guioza/ })
    .click();
  await expect(page).toHaveURL(/\/reels\?produto=.*&de=inicio/);
  const guioza = page.getByRole("region", { name: "Guioza" });
  await expect(guioza.getByText("1 de 2")).toBeAttached();

  // Como no Instagram: um toque perto da borda direita passa para a próxima; da esquerda, volta.
  await guioza.getByRole("button", { name: "Próxima foto ou vídeo" }).click();
  await expect(guioza.getByText("2 de 2")).toBeAttached();
  await guioza.getByRole("button", { name: "Foto ou vídeo anterior" }).click();
  await expect(guioza.getByText("1 de 2")).toBeAttached();

  await page.getByRole("link", { name: "Fechar os vídeos" }).click();
  await expect(page).toHaveURL(new RegExp(`/m/${qr}$`));
});

test("fechar o Reels volta para o mesmo lugar do cardápio, e não para o topo", async ({ page, request }) => {
  const video = await recordVideo(page);
  const { qr, headers } = await restaurant(request, video);
  // Pratos o bastante para o cardápio rolar, e um com vídeo lá embaixo.
  const post = async (path: string, data: object) =>
    (await request.post(`${API_URL}${path}`, { headers, data })).json();
  const pratos = await post("/api/v1/menu-categories", { name: "Pratos" });
  for (let i = 1; i <= 10; i++) {
    await post("/api/v1/products", {
      categoryId: pratos.id,
      name: `Prato ${i}`,
      priceCents: 3000,
      featured: false,
      available: true,
    });
  }
  const sobremesas = await post("/api/v1/menu-categories", { name: "Sobremesas" });
  const mochi = await post("/api/v1/products", {
    categoryId: sobremesas.id,
    name: "Mochi",
    priceCents: 1500,
    featured: false,
    available: true,
  });
  await addVideo(request, headers, mochi.id, video);

  await page.goto(`/m/${qr}/cardapio`);
  const link = page.getByRole("link", { name: "Ver Mochi em fotos e vídeos" });
  await link.scrollIntoViewIfNeeded();
  await page.waitForTimeout(300);
  const before = await page.evaluate(() => scrollY);
  expect(before).toBeGreaterThan(300);
  await link.click();
  await expect(page).toHaveURL(/\/reels\?produto=/);
  await expect(page.getByRole("region", { name: "Mochi" })).toBeVisible();

  await page.getByRole("link", { name: "Fechar os vídeos" }).click();
  await expect(page).toHaveURL(new RegExp(`/m/${qr}/cardapio$`));
  await expect.poll(() => page.evaluate(() => scrollY)).toBeGreaterThan(before - 80);
  await expect(page.getByRole("link", { name: "Ver Mochi em fotos e vídeos" })).toBeInViewport();
});
