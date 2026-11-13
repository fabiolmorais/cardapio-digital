"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { disablePush } from "@/lib/pwa/push";
import { loginPath } from "./login-origin";
import { endSession } from "./session";

/** Sair: encerra a sessão no back e recarrega a página inteira na tela de entrar (e-mail ou PIN). */
export function useLogout() {
  const queryClient = useQueryClient();
  const [leaving, setLeaving] = useState(false);

  async function logout() {
    setLeaving(true);
    // Quem sai deixa de receber os avisos neste aparelho (enquanto ainda tem a sessão para avisar
    // o back). Sem rede, não segura a saída: no máximo 3 segundos.
    await Promise.race([disablePush().catch(() => {}), new Promise((resolve) => setTimeout(resolve, 3000))]);
    await endSession();
    queryClient.clear();
    // Recarrega a página inteira: nada desta conta (dados, telas guardadas) fica na memória
    // para a próxima pessoa que usar o aparelho.
    window.location.replace(await loginPath());
  }

  return { leaving, logout };
}
