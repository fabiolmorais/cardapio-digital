import type { components } from "@/lib/api/schema";

/**
 * Desconto da promoção, em % inteiro (1990 → 1590 dá 20).
 *
 * Fica fora do table-data.ts de propósito: aquele busca os dados com o cliente da API (que traz
 * o login do painel junto), e esta função roda no celular, onde só o cálculo deve pesar.
 */
export function discountPercent(item: components["schemas"]["PublicMenuItem"]): number | null {
  if (item.promoPriceCents == null) return null;
  return Math.round((1 - item.promoPriceCents / item.priceCents) * 100);
}
