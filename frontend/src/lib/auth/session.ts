import type { Schemas } from "@/lib/api/client";

/**
 * Sessão de quem entrou no painel.
 *
 * O token de acesso fica só na memória da aba, nunca em localStorage nem em cookie
 * legível pelo JavaScript: assim um script injetado (XSS) não consegue copiá-lo.
 * Ao recarregar a página a sessão some; a renovação pelo cookie HttpOnly vem com o
 * refresh token (Guia 3).
 */
export type Session = {
  accessToken: string;
  expiresAt: string;
  userName: string;
  establishment: Schemas["EstablishmentAccess"];
};

let current: Session | null = null;
const listeners = new Set<() => void>();

function emit() {
  for (const listener of listeners) listener();
}

export function setSession(session: Session | null) {
  current = session;
  emit();
}

export function getAccessToken(): string | null {
  if (!current) return null;
  if (Date.parse(current.expiresAt) <= Date.now()) {
    current = null;
    emit();
    return null;
  }
  return current.accessToken;
}

export function getSession(): Session | null {
  return current;
}

export function subscribeToSession(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}
