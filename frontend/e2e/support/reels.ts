import { expect, type APIRequestContext, type Page } from "@playwright/test";
import { API_URL, apiLogin, createAccount } from "./account";

/**
 * Um vídeo curto de verdade (WebM), gravado no próprio navegador a partir de um canvas: nenhum
 * arquivo de vídeo no repositório, e o navegador do teste consegue tocá-lo.
 */
export async function recordVideo(page: Page): Promise<Buffer> {
  await page.goto("about:blank");
  const base64 = await page.evaluate(async () => {
    const canvas = document.createElement("canvas");
    canvas.width = 360;
    canvas.height = 640;
    const context = canvas.getContext("2d")!;
    const recorder = new MediaRecorder(canvas.captureStream(30), { mimeType: "video/webm" });
    const parts: Blob[] = [];
    recorder.ondataavailable = (event) => parts.push(event.data);
    recorder.start();
    for (let i = 0; i < 30; i++) {
      context.fillStyle = `hsl(${i * 10}, 70%, 50%)`;
      context.fillRect(0, 0, 360, 640);
      await new Promise((resolve) => setTimeout(resolve, 33));
    }
    recorder.stop();
    await new Promise((resolve) => (recorder.onstop = resolve));
    const bytes = new Uint8Array(await new Blob(parts).arrayBuffer());
    let text = "";
    for (const byte of bytes) text += String.fromCharCode(byte);
    return btoa(text);
  });
  return Buffer.from(base64, "base64");
}

/** Envia o vídeo de um produto pelo serviço de vídeo falso (o mesmo TUS do navegador) e espera ficar pronto. */
export async function addVideo(
  request: APIRequestContext,
  headers: Record<string, string>,
  productId: string,
  video: Buffer,
) {
  const upload = await (
    await request.post(`${API_URL}/api/v1/products/${productId}/videos`, {
      headers,
      data: { contentType: "video/webm", sizeBytes: video.length },
    })
  ).json();
  const created = await request.post(`${API_URL}${new URL(upload.endpoint).pathname}`, {
    headers: {
      ...upload.headers,
      "Tus-Resumable": "1.0.0",
      "Upload-Length": String(video.length),
      "Upload-Metadata": `filetype ${Buffer.from("video/webm").toString("base64")}`,
    },
  });
  expect(created.status()).toBe(201);
  const patched = await request.patch(`${API_URL}${created.headers()["location"]}`, {
    headers: { "Tus-Resumable": "1.0.0", "Upload-Offset": "0", "Content-Type": "application/offset+octet-stream" },
    data: video,
  });
  expect(patched.status()).toBe(204);
  // A galeria do admin pergunta ao serviço e grava "pronto" (no computador, o aviso do serviço não chega).
  const media = await (await request.get(`${API_URL}/api/v1/products/${productId}/media`, { headers })).json();
  expect(media.content.at(-1).status).toBe("READY");
}

export async function restaurant(request: APIRequestContext, video: Buffer) {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) =>
    (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  const sector = await post("/api/v1/sectors", { name: "Salão" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  const entradas = await post("/api/v1/menu-categories", { name: "Entradas" });
  const product = (name: string, priceCents: number, extra: object = {}) =>
    post("/api/v1/products", { categoryId: entradas.id, name, priceCents, featured: false, available: true, ...extra });
  const guioza = await product("Guioza", 2990, { featured: true, description: "Pastel japonês selado na chapa." });
  const sunomono = await product("Sunomono", 1990);
  await product("Chá verde", 1200); // sem foto nem vídeo: fica fora do Reels
  await addVideo(request, headers, guioza.id, video);
  await addVideo(request, headers, sunomono.id, video);
  return { qr: table.publicToken as string, headers, guiozaId: guioza.id as string };
}

/** O vídeo de um prato está tocando? */
