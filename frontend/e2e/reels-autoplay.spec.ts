import { expect, test, type Page } from "@playwright/test";
import { recordVideo, restaurant } from "./support/reels";

test.describe.configure({ timeout: 90_000 });

/**
 * O Playwright abre o Chromium liberando o autoplay. Aqui não: vale a regra de verdade dos
 * celulares (vídeo sem som pode tocar sozinho; com som, só depois de um toque).
 */
test.use({ launchOptions: { ignoreDefaultArgs: ["--autoplay-policy=no-user-gesture-required"] } });

/**
 * Simula o celular que recusa o play(): `first` recusa só a primeira tentativa (o iPhone, às vezes,
 * mesmo com o vídeo mudo e pronto); `gesture` recusa até o cliente tocar (o modo de pouca bateria).
 */
async function refusePlay(page: Page, mode: "first" | "gesture") {
  await page.addInitScript((refusal) => {
    const original = HTMLMediaElement.prototype.play;
    let calls = 0;
    HTMLMediaElement.prototype.play = function () {
      calls++;
      const refuse = refusal === "first" ? calls === 1 : !(window as unknown as { tapped?: boolean }).tapped;
      if (refuse) return Promise.reject(new DOMException("O navegador recusou", "NotAllowedError"));
      return original.call(this);
    };
  }, mode);
}

async function openReels(page: Page, qr: string) {
  await page.goto(`/m/${qr}/cardapio`);
  await page.getByRole("link", { name: "Ver Guioza em fotos e vídeos" }).click();
  const region = page.getByRole("region", { name: "Guioza" });
  const video = region.locator("video").first();
  return { region, video, playing: () => video.evaluate((v: HTMLVideoElement) => !v.paused) };
}

test("o vídeo do Reels toca sozinho mesmo se o celular recusar a primeira tentativa", async ({ page, request }) => {
  const video = await recordVideo(page);
  const { qr } = await restaurant(request, video);
  await refusePlay(page, "first");

  const reels = await openReels(page, qr);
  await expect.poll(reels.playing).toBe(true);
  await expect(reels.region.getByText("Toque para assistir")).not.toBeAttached();
});

test("se o celular não deixa tocar sozinho, aparece o ▶ e o toque toca o vídeo", async ({ page, request }) => {
  const video = await recordVideo(page);
  const { qr } = await restaurant(request, video);
  await refusePlay(page, "gesture");

  const reels = await openReels(page, qr);
  await expect(reels.region.getByText("Toque para assistir")).toBeAttached();
  expect(await reels.playing()).toBe(false);
  await page.evaluate(() => Object.assign(window, { tapped: true }));
  await reels.video.click();
  await expect.poll(reels.playing).toBe(true);
  await expect(reels.region.getByText("Toque para assistir")).not.toBeAttached();
  // Tocar de novo pausa, como antes.
  await reels.video.click();
  await expect.poll(reels.playing).toBe(false);
});
