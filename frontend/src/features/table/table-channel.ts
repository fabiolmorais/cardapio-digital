"use client";

import { useEffect, useRef } from "react";
import { baseUrl } from "@/lib/api/base";
import { startVisit } from "./table-calls";

/** Sem o tempo real (conexão caída), a página volta a perguntar de tempos em tempos. */
const FALLBACK_REFRESH_MS = 15_000;
/** Quanto esperar para abrir o canal de novo quando o servidor o recusa (sessão vencida, por exemplo). */
const REOPEN_MS = 3000;

/**
 * O canal de tempo real da mesa (SSE). "ready" ao conectar e os eventos pedidos (`call`, `order`)
 * chamam `onUpdate(true)`: o aviso não traz dados, só o motivo, e a tela busca de novo. A primeira busca
 * fica para logo depois da montagem, e, enquanto o canal não está aberto, uma rede de segurança
 * pergunta de tempos em tempos (essas duas chamam `onUpdate(false)`).
 */
export function useTableChannel(
  token: string,
  events: readonly string[],
  onUpdate: (fromChannel: boolean) => void,
) {
  // O canal chama a versão mais nova, sem reabrir a conexão.
  const update = useRef(onUpdate);
  useEffect(() => {
    update.current = onUpdate;
  }, [onUpdate]);
  const names = events.join(",");

  useEffect(() => {
    let live = false;
    let stopped = false;
    const first = window.setTimeout(() => update.current(false), 0);
    const fallback = window.setInterval(() => {
      if (!live && document.visibilityState === "visible") update.current(false);
    }, FALLBACK_REFRESH_MS);

    let source: EventSource | null = null;
    let reopen: number | undefined;
    const url = `${baseUrl}/api/v1/public/tables/${encodeURIComponent(token)}/events`;
    function open() {
      source = new EventSource(url, { withCredentials: true });
      const onEvent = () => {
        live = true;
        update.current(true);
      };
      for (const name of ["ready", ...names.split(",")]) source.addEventListener(name, onEvent);
      source.onerror = () => {
        live = false;
        // Se a conexão só caiu, o navegador reconecta sozinho. Se o servidor recusou (401: a
        // sessão venceu), ele desiste: aí a página renova a sessão e abre o canal de novo.
        if (source?.readyState === EventSource.CLOSED) {
          source = null;
          reopen = window.setTimeout(async () => {
            await startVisit(token, true);
            if (!stopped) open();
          }, REOPEN_MS);
        }
      };
    }
    // O canal só abre depois da visita (sessão e presença): sem ela, o servidor recusaria (401).
    void startVisit(token).then(() => {
      if (!stopped) open();
    });

    return () => {
      stopped = true;
      window.clearTimeout(first);
      window.clearInterval(fallback);
      window.clearTimeout(reopen);
      source?.close();
    };
  }, [names, token]);
}
