"use client";

import { useEffect } from "react";
import { baseUrl } from "@/lib/api/base";

/**
 * Abre (ou renova) a sessão deste celular na mesa: o back grava o cookie table_session
 * (HttpOnly, invisível para este código). Chamada direta com fetch, e não pelo cliente da API do
 * painel: aquele traz a sessão do login junto, que a página do cliente não usa (e o JavaScript
 * desta página precisa ser pequeno). Se falhar, a página continua: os botões que usam a sessão
 * (Etapa 6) tentam de novo.
 */
export function StartSession({ token }: { token: string }) {
  useEffect(() => {
    void fetch(`${baseUrl}/api/v1/public/tables/${encodeURIComponent(token)}/session`, {
      method: "POST",
      credentials: "include",
    }).catch(() => undefined);
  }, [token]);
  return null;
}
