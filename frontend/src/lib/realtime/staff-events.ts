import { baseUrl } from "@/lib/api/base";
import { getFreshAccessToken, refreshSession } from "@/lib/auth/session";
import { createSseParser, type ServerEvent } from "./sse-parser";

const MAX_BACKOFF_MS = 15_000;
/** Tempo em segundo plano a partir do qual a conexão é refeita ao voltar. */
const RECONNECT_AFTER_HIDDEN_MS = 10_000;

/**
 * Liga a tela da equipe ao canal de tempo real do restaurante (GET /api/v1/events).
 *
 * Não usa o EventSource do navegador porque ele não envia o cabeçalho Authorization, e o token
 * da equipe fica só na memória (nunca em cookie legível). Por isso é um fetch comum, lendo a
 * resposta aos pedaços. Se a conexão cair (o servidor encerra a cada 15 min, ou a rede falha),
 * reconecta sozinho, esperando um pouco mais a cada falha seguida. Com 401, renova a sessão;
 * sem sessão, desiste. Ao voltar de 10 s ou mais em segundo plano, reconecta na hora.
 *
 * Devolve a função que desliga.
 */
export function connectStaffEvents(
  onEvent: (event: ServerEvent) => void,
  onConnectedChange: (connected: boolean) => void,
): () => void {
  const abort = new AbortController();
  let backoff = 1000;
  // A conexão desta volta (refeita ao voltar do segundo plano) e o fim antecipado da espera.
  let attempt: AbortController | null = null;
  let wake: (() => void) | null = null;
  let hiddenAt: number | null = null;

  // No celular, a conexão morre calada com a tela apagada ou o app em segundo plano. Ao voltar,
  // reconecta na hora: o "ready" da nova conexão faz as listas buscarem tudo de novo.
  function onVisibilityChange() {
    if (document.visibilityState === "hidden") {
      hiddenAt = Date.now();
      return;
    }
    const away = hiddenAt === null ? 0 : Date.now() - hiddenAt;
    hiddenAt = null;
    if (away < RECONNECT_AFTER_HIDDEN_MS) return;
    backoff = 1000;
    attempt?.abort();
    wake?.();
  }
  document.addEventListener("visibilitychange", onVisibilityChange);

  async function run() {
    while (!abort.signal.aborted) {
      const current = new AbortController();
      attempt = current;
      try {
        const token = await getFreshAccessToken();
        if (!token) return;
        const response = await fetch(`${baseUrl}/api/v1/events`, {
          headers: { Authorization: `Bearer ${token}`, Accept: "text/event-stream" },
          signal: current.signal,
        });
        if (response.status === 401) {
          // Token recusado: renova e tenta de novo depois da espera. Sem sessão, desiste.
          if (!(await refreshSession())) return;
          throw new Error("401");
        }
        if (!response.ok || !response.body) throw new Error(String(response.status));
        const parse = createSseParser((event) => {
          if (event.name === "ready") {
            backoff = 1000;
            onConnectedChange(true);
          }
          onEvent(event);
        });
        const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
        for (;;) {
          const { value, done } = await reader.read();
          if (done) break;
          parse(value);
        }
      } catch {
        if (abort.signal.aborted) return;
      }
      onConnectedChange(false);
      if (current.signal.aborted) continue; // voltou do segundo plano: sem espera
      await new Promise<void>((resolve) => {
        const timer = setTimeout(resolve, backoff);
        wake = () => {
          clearTimeout(timer);
          resolve();
        };
      });
      wake = null;
      backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
    }
  }

  void run();
  return () => {
    abort.abort();
    attempt?.abort();
    wake?.();
    document.removeEventListener("visibilitychange", onVisibilityChange);
    onConnectedChange(false);
  };
}
