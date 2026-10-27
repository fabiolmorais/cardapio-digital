"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { loginPath } from "./login-origin";
import { endSession } from "./session";

/** Sair: encerra a sessão no back e recarrega a página inteira na tela de entrar (e-mail ou PIN). */
export function useLogout() {
  const queryClient = useQueryClient();
  const [leaving, setLeaving] = useState(false);

  async function logout() {
    setLeaving(true);
    await endSession();
    queryClient.clear();
    // Recarrega a página inteira: nada desta conta (dados, telas guardadas) fica na memória
    // para a próxima pessoa que usar o aparelho.
    window.location.replace(loginPath());
  }

  return { leaving, logout };
}
