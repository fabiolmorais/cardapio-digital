"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { loginPath } from "./login-origin";
import { restoreSession, type SessionState } from "./session";
import { useSession } from "./use-session";

/**
 * Para telas que exigem login: recupera a sessão pelo cookie (depois de um F5) e, sem
 * sessão, manda para a tela de entrar (e-mail ou PIN, conforme a pessoa entrou nesta aba).
 * A tela mostra "carregando" enquanto o status não for
 * "authenticated".
 */
export function useRequiredSession(): SessionState {
  const router = useRouter();
  const queryClient = useQueryClient();
  const state = useSession();
  const { status } = state;

  useEffect(() => {
    if (status === "unknown") void restoreSession();
    if (status === "anonymous") {
      queryClient.clear();
      router.replace(loginPath());
    }
  }, [status, router, queryClient]);

  return state;
}
