"use client";

import { useSyncExternalStore } from "react";
import { getServerSessionState, getSessionState, subscribeToSession, type SessionState } from "./session";

/** Estado da sessão; a tela redesenha quando alguém entra ou sai. */
export function useSession(): SessionState {
  return useSyncExternalStore(subscribeToSession, getSessionState, getServerSessionState);
}
