import { expect, test } from "@playwright/test";
import { gzipSync } from "node:zlib";
import { API_URL, apiLogin, createAccount } from "./support/account";

/**
 * O orçamento da página da mesa (CLAUDE.md): menos de 200 KB de JavaScript comprimido para abrir.
 * Conta os scripts que o HTML da página traz, comprimidos com gzip. Fica de fora o que o Next
 * pré-carrega depois, quando a página já abriu (o código das páginas dos links: cardápio, conta),
 * e os polyfills noModule, que os navegadores com módulos (todos os que a mesa atende) não baixam.
 */
const BUDGET_KB = 200;

test("cada página da mesa abre com menos de 200 KB de JavaScript", async ({ request }) => {
  const { email } = await createAccount(request);
  const headers = { Authorization: `Bearer ${await apiLogin(request, email)}` };
  const post = async (path: string, data: object) =>
    (await request.post(`${API_URL}${path}`, { headers, data })).json();
  await request.put(`${API_URL}/api/v1/presence-rule`, { headers, data: { networks: ["127.0.0.1", "::1"] } });
  const sector = await post("/api/v1/sectors", { name: "Salão" });
  const table = await post("/api/v1/tables", { label: "04", sectorId: sector.id });
  const category = await post("/api/v1/menu-categories", { name: "Entradas" });
  await post("/api/v1/products", {
    categoryId: category.id,
    name: "Guioza",
    priceCents: 2990,
    featured: true,
    available: true,
  });

  // Cada script baixado uma vez, direto pela API do Playwright (sem o cache do navegador).
  const sizes = new Map<string, number>();
  const sizeOf = async (src: string) => {
    if (!sizes.has(src)) sizes.set(src, gzipSync(await (await request.get(src)).body()).length);
    return sizes.get(src)!;
  };

  const report: Record<string, number> = {};
  for (const path of ["", "/cardapio", "/carrinho", "/pedidos", "/conta"]) {
    const html = await (await request.get(`/m/${table.publicToken}${path}`)).text();
    // Os scripts do HTML, menos os polyfills noModule (só navegador sem módulos baixa; ADR 0009).
    const tags = [...html.matchAll(/<script\b[^>]*\bsrc="([^"]+)"[^>]*>/g)];
    const scripts = new Set(tags.filter(([tag]) => !/\bnomodule\b/i.test(tag)).map(([, src]) => src));
    expect(scripts.size, `scripts de ${path || "/"}`).toBeGreaterThan(0);
    let total = 0;
    for (const src of scripts) total += await sizeOf(src);
    report[path || "/"] = Math.round(total / 102.4) / 10;
  }
  test.info().annotations.push({ type: "JavaScript da mesa (KB)", description: JSON.stringify(report) });
  for (const [path, kb] of Object.entries(report)) expect(kb, `JavaScript de ${path}`).toBeLessThan(BUDGET_KB);
});
