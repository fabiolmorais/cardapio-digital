import { authApi } from "@/lib/api/base";
import type { components } from "@/lib/api/schema";

/**
 * Sessão de quem entrou no painel.
 *
 * O token de acesso (vida curta) fica só na memória da aba, nunca em localStorage nem em
 * cookie legível pelo JavaScript: assim um script injetado (XSS) não consegue copiá-lo.
 * Quem guarda a sessão entre recargas é o refresh token, num cookie HttpOnly que o
 * JavaScript não lê; a aba troca esse cookie por um token de acesso novo em /auth/refresh.
 *
 * Este módulo não usa hooks do React, para poder ser importado também no servidor.
 */
export type Session = {
  accessToken: string;
  expiresAt: string;
  userName: string;
  establishment: components["schemas"]["EstablishmentAccess"];
};

/**
 * unknown: a página acabou de abrir e ainda não sabemos se há sessão no cookie.
 * authenticated: há um token de acesso na memória.
 * anonymous: não há sessão (nunca entrou, saiu ou a sessão terminou).
 */
export type SessionState =
  | { status: "unknown"; session: null }
  | { status: "authenticated"; session: Session }
  | { status: "anonymous"; session: null };

const UNKNOWN: SessionState = { status: "unknown", session: null };
const ANONYMOUS: SessionState = { status: "anonymous", session: null };

/** Renova este tempo antes de o token vencer, para nenhuma chamada sair com token vencido. */
const RENEW_BEFORE_MS = 60_000;
/** Nome do Web Lock que deixa só uma aba por vez trocar o cookie. */
const REFRESH_LOCK = "cardapio:refresh-session";
const CHANNEL = "cardapio:session";

let state: SessionState = UNKNOWN;
/** Quando o token vence, no relógio desta aba (ver lifetimeMs). */
let expiresAtLocal = 0;
const listeners = new Set<() => void>();
let renewTimer: ReturnType<typeof setTimeout> | undefined;
let inFlight: Promise<Session | null> | null = null;

function setState(next: SessionState) {
  state = next;
  clearTimeout(renewTimer);
  if (next.status === "authenticated") {
    expiresAtLocal = Date.now() + lifetimeMs(next.session);
    if (typeof window !== "undefined") {
      const wait = expiresAtLocal - Date.now() - RENEW_BEFORE_MS;
      renewTimer = setTimeout(() => void refreshSession(), Math.max(wait, 0));
    }
  }
  for (const listener of listeners) listener();
}

/**
 * Tempo de vida do token (exp − iat do JWT). Contar o prazo a partir do relógio da aba,
 * e não comparar expiresAt com ele, evita renovar sem parar num celular com o relógio
 * adiantado. O JWT é só lido aqui, não verificado: quem confere a assinatura é o back.
 */
function lifetimeMs(session: Session): number {
  try {
    const part = session.accessToken.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const { iat, exp } = JSON.parse(atob(part)) as { iat?: unknown; exp?: unknown };
    if (typeof iat === "number" && typeof exp === "number") return (exp - iat) * 1000;
  } catch {
    // Token fora do formato esperado: usa o expiresAt da resposta.
  }
  return Date.parse(session.expiresAt) - Date.now();
}

export function getSessionState(): SessionState {
  return state;
}

/** Estado usado na renderização no servidor: lá nunca se sabe quem é o usuário. */
export function getServerSessionState(): SessionState {
  return UNKNOWN;
}

export function subscribeToSession(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

/** Guarda a sessão que o login devolveu. */
export function startSession(session: Session) {
  setState({ status: "authenticated", session });
}

function isFresh() {
  return expiresAtLocal - Date.now() > RENEW_BEFORE_MS;
}

/**
 * Troca o cookie do refresh token por um token de acesso novo. Chamadas ao mesmo tempo
 * nesta aba compartilham a mesma troca; entre abas, o Web Lock põe uma depois da outra.
 * Isso importa: o back troca o cookie a cada uso, e duas abas usando o mesmo cookie ao
 * mesmo tempo seriam vistas como roubo de token, derrubando a sessão.
 */
export function refreshSession(): Promise<Session | null> {
  inFlight ??= withRefreshLock(doRefresh).finally(() => {
    inFlight = null;
  });
  return inFlight;
}

async function withRefreshLock<T>(task: () => Promise<T>): Promise<T> {
  if (typeof navigator !== "undefined" && navigator.locks) {
    return await navigator.locks.request(REFRESH_LOCK, task);
  }
  return task();
}

async function doRefresh(): Promise<Session | null> {
  try {
    const { data } = await authApi.POST("/api/v1/auth/refresh");
    if (data) {
      startSession(data);
      return data;
    }
    setState(ANONYMOUS);
    return null;
  } catch {
    // Sem rede: mantém o token atual enquanto ele valer; a próxima chamada tenta de novo.
    if (state.status === "authenticated" && expiresAtLocal > Date.now()) {
      return state.session;
    }
    setState(ANONYMOUS);
    return null;
  }
}

/**
 * Token para a próxima chamada: renova antes se estiver para vencer. Quem nunca entrou
 * não dispara renovação (as páginas públicas não chamam /auth/refresh à toa).
 */
export async function getFreshAccessToken(): Promise<string | null> {
  if (state.status !== "authenticated") return null;
  if (isFresh()) return state.session.accessToken;
  const session = await refreshSession();
  return session?.accessToken ?? null;
}

/** Ao abrir o painel (ou depois de um F5): recupera a sessão pelo cookie, se houver. */
export function restoreSession(): Promise<Session | null> {
  if (state.status === "authenticated") return Promise.resolve(state.session);
  return refreshSession();
}

/** Sai: o back revoga o refresh token e apaga o cookie; as outras abas saem junto. */
export async function endSession() {
  try {
    await authApi.POST("/api/v1/auth/logout");
  } catch {
    // Mesmo sem rede, a aba esquece o token; o refresh token vence sozinho no back.
  }
  setState(ANONYMOUS);
  channel()?.postMessage("logout");
}

let broadcast: BroadcastChannel | null | undefined;

function channel(): BroadcastChannel | null {
  if (broadcast !== undefined) return broadcast;
  if (typeof BroadcastChannel === "undefined") return (broadcast = null);
  broadcast = new BroadcastChannel(CHANNEL);
  broadcast.onmessage = (event) => {
    if (event.data === "logout") setState(ANONYMOUS);
  };
  return broadcast;
}

// No navegador, já escuta a saída feita em outra aba.
if (typeof window !== "undefined") channel();
