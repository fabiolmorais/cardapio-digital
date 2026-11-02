"use client";

import { useEffect } from "react";
import { startVisit } from "./table-calls";

/**
 * Abre (ou renova) a sessão deste celular na mesa: o back grava o cookie table_session
 * (HttpOnly, invisível para este código). Chamada direta com fetch, e não pelo cliente da API do
 * painel: aquele traz a sessão do login junto, que a página do cliente não usa (e o JavaScript
 * desta página precisa ser pequeno). Se falhar, a página continua: os botões que usam a sessão
 * (Etapa 6) tentam de novo.
 */
export function StartSession({ token }: { token: string }) {
  useEffect(() => {
    // Abre a sessão e, com ela aberta, confere a presença pela rede do salão (trava de presença).
    void startVisit(token);
  }, [token]);
  return null;
}
