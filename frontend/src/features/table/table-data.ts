import { notFound } from "next/navigation";
import { cache } from "react";
import { api, type Schemas } from "@/lib/api/client";

export type PublicTable = Schemas["PublicTable"];
export type PublicMenu = Schemas["PublicMenu"];
export type MenuItem = Schemas["PublicMenuItem"];

/**
 * Os dados da página da mesa, buscados NO SERVIDOR do Next (a página chega pronta ao celular).
 * O cache() do React junta chamadas repetidas na mesma requisição: o layout e a página pedem a
 * mesa, mas a API recebe um pedido só.
 *
 * 404 vira a página "QR não vale mais" (notFound); outra falha vira null, e a página mostra o aviso.
 */
export const getTable = cache(async (token: string): Promise<PublicTable | null> => {
  const { data, response } = await api
    .GET("/api/v1/public/tables/{token}", { params: { path: { token } } })
    .catch(() => ({ data: undefined, response: undefined }));
  if (response?.status === 404) notFound();
  return data ?? null;
});

export const getMenu = cache(async (token: string): Promise<PublicMenu | null> => {
  const { data, response } = await api
    .GET("/api/v1/public/tables/{token}/menu", { params: { path: { token } } })
    .catch(() => ({ data: undefined, response: undefined }));
  if (response?.status === 404) notFound();
  return data ?? null;
});

/** Desconto da promoção, em % inteiro (1990 → 1590 dá 20). */
export function discountPercent(item: MenuItem): number | null {
  if (item.promoPriceCents == null) return null;
  return Math.round((1 - item.promoPriceCents / item.priceCents) * 100);
}
