import { baseUrl } from "@/lib/api/base";
import type { ServerEvent } from "./sse-parser";

/** Quanto esperar para abrir o canal de novo quando o servidor o recusa. */
const REOPEN_MS = 5000;

/**
 * Liga a TV vinculada ao canal de tempo real (GET /api/v1/auth/device/tv/events). Aqui dá para
 * usar o EventSource do navegador: quem identifica a TV é o cookie HttpOnly do aparelho, que
 * ele envia sozinho (withCredentials), sem cabeçalho nenhum.
 *
 * Se a conexão só cair, o EventSource reconecta sozinho. Se o servidor recusar (a TV foi
 * desvinculada), ele desiste: aqui tentamos de novo depois de um tempo.
 *
 * Devolve a função que desliga.
 */
export function connectTvEvents(
  onEvent: (event: ServerEvent) => void,
  onConnectedChange: (connected: boolean) => void,
): () => void {
  let source: EventSource | null = null;
  let reopen: ReturnType<typeof setTimeout> | undefined;
  let stopped = false;

  function open() {
    source = new EventSource(`${baseUrl}/api/v1/auth/device/tv/events`, { withCredentials: true });
    for (const name of ["ready", "call"]) {
      source.addEventListener(name, (message) => {
        if (name === "ready") onConnectedChange(true);
        try {
          onEvent({ name, data: JSON.parse((message as MessageEvent<string>).data) });
        } catch {
          // Aviso fora do formato esperado: ignora.
        }
      });
    }
    source.onerror = () => {
      onConnectedChange(false);
      if (source?.readyState === EventSource.CLOSED && !stopped) {
        source = null;
        reopen = setTimeout(open, REOPEN_MS);
      }
    };
  }

  open();
  return () => {
    stopped = true;
    clearTimeout(reopen);
    source?.close();
    onConnectedChange(false);
  };
}
