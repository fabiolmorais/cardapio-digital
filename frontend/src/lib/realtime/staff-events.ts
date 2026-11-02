import { baseUrl } from "@/lib/api/base";
import { getFreshAccessToken, refreshSession } from "@/lib/auth/session";
import { createSseParser, type ServerEvent } from "./sse-parser";

const MAX_BACKOFF_MS = 15_000;

/**
 * Liga a tela da equipe ao canal de tempo real do restaurante (GET /api/v1/events).
 *
 * Não usa o EventSource do navegador porque ele não envia o cabeçalho Authorization, e o token
 * da equipe fica só na memória (nunca em cookie legível). Por isso é um fetch comum, lendo a
 * resposta aos pedaços. Se a conexão cair (o servidor encerra a cada 15 min, ou a rede falha),
 * reconecta sozinho, esperando um pouco mais a cada falha seguida. Com 401, renova a sessão;
 * sem sessão, desiste.
 *
 * Devolve a função que desliga.
 */
export function connectStaffEvents(
  onEvent: (event: ServerEvent) => void,
  onConnectedChange: (connected: boolean) => void,
): () => void {
  const abort = new AbortController();
  let backoff = 1000;

  async function run() {
    while (!abort.signal.aborted) {
      try {
        const token = await getFreshAccessToken();
        if (!token) return;
        const response = await fetch(`${baseUrl}/api/v1/events`, {
          headers: { Authorization: `Bearer ${token}`, Accept: "text/event-stream" },
          signal: abort.signal,
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
      await new Promise((resolve) => setTimeout(resolve, backoff));
      backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
    }
  }

  void run();
  return () => {
    abort.abort();
    onConnectedChange(false);
  };
}
