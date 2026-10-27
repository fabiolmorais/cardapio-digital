"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { restoreSession, type SessionState } from "./session";
import { useSession } from "./use-session";

/**
 * Para telas que exigem login: recupera a sessão pelo cookie (depois de um F5) e, sem
 * sessão, manda para /entrar. A tela mostra "carregando" enquanto o status não for
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
      router.replace("/entrar");
    }
  }, [status, router, queryClient]);

  return state;
}
