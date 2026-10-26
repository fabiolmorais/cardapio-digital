"use client";

import { useSyncExternalStore } from "react";
import { getSession, subscribeToSession, type Session } from "./session";

/** Sessão atual; a tela redesenha quando alguém entra ou sai. No servidor é sempre nula. */
export function useSession(): Session | null {
  return useSyncExternalStore(subscribeToSession, getSession, () => null);
}
