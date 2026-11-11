"use client";

import { useEffect, useRef } from "react";
import { baseUrl } from "@/lib/api/base";
import { startVisit } from "./table-calls";

/** Sem o tempo real (conexão caída), a página volta a perguntar de tempos em tempos. */
const FALLBACK_REFRESH_MS = 15_000;
/** Quanto esperar para abrir o canal de novo quando o servidor o recusa (sessão vencida, por exemplo). */
const REOPEN_MS = 3000;

/** Todos os avisos que alguma tela da mesa ouve: o canal assina todos, e cada hook filtra os seus. */
const ALL_EVENTS = ["ready", "call", "order", "rodizio", "tab", "presence"];

type Subscriber = { names: Set<string>; onEvent: (event: string) => void };
type Channel = { subscribers: Set<Subscriber>; live: boolean; stop: () => void };

/**
 * Uma conexão por mesa, dividida entre os hooks da página: os botões, os pedidos e o rodízio
 * ouviriam cada um com a sua, e o navegador só abre 6 conexões por servidor (no HTTP/1.1).
 */
const channels = new Map<string, Channel>();

function openChannel(token: string): Channel {
  const channel: Channel = { subscribers: new Set(), live: false, stop: () => {} };
  let source: EventSource | null = null;
  let reopen: number | undefined;
  let stopped = false;
  const url = `${baseUrl}/api/v1/public/tables/${encodeURIComponent(token)}/events`;
  function open() {
    source = new EventSource(url, { withCredentials: true });
    const onEvent = (event: Event) => {
      channel.live = true;
      for (const subscriber of channel.subscribers) {
        if (event.type === "ready" || subscriber.names.has(event.type)) subscriber.onEvent(event.type);
      }
    };
    for (const name of ALL_EVENTS) source.addEventListener(name, onEvent);
    source.onerror = () => {
      channel.live = false;
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
  channel.stop = () => {
    stopped = true;
    window.clearTimeout(reopen);
    source?.close();
  };
  return channel;
}

function join(token: string, subscriber: Subscriber): { channel: Channel; leave: () => void } {
  let channel = channels.get(token);
  if (!channel) {
    channel = openChannel(token);
    channels.set(token, channel);
  }
  channel.subscribers.add(subscriber);
  const joined = channel;
  return {
    channel: joined,
    leave: () => {
      joined.subscribers.delete(subscriber);
      if (joined.subscribers.size === 0) {
        joined.stop();
        channels.delete(token);
      }
    },
  };
}

/**
 * O canal de tempo real da mesa (SSE). "ready" ao conectar e os eventos pedidos (`call`, `order`…)
 * chamam `onUpdate(true, nome do aviso)`: o aviso não traz dados, só o motivo, e a tela busca de
 * novo. A primeira busca fica para logo depois da montagem, e, enquanto o canal não está aberto,
 * uma rede de segurança pergunta de tempos em tempos (essas duas chamam `onUpdate(false)`).
 */
export function useTableChannel(
  token: string,
  events: readonly string[],
  onUpdate: (fromChannel: boolean, event?: string) => void,
) {
  // O canal chama a versão mais nova, sem reabrir a conexão.
  const update = useRef(onUpdate);
  useEffect(() => {
    update.current = onUpdate;
  }, [onUpdate]);
  const names = events.join(",");

  useEffect(() => {
    const first = window.setTimeout(() => update.current(false), 0);
    const { channel, leave } = join(token, {
      names: new Set(names.split(",")),
      onEvent: (event) => update.current(true, event),
    });
    const fallback = window.setInterval(() => {
      if (!channel.live && document.visibilityState === "visible") update.current(false);
    }, FALLBACK_REFRESH_MS);
    return () => {
      window.clearTimeout(first);
      window.clearInterval(fallback);
      leave();
    };
  }, [names, token]);
}
