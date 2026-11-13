"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { liveRefreshMs, useMissedEventWatch } from "@/lib/realtime/missed-events";
import { subscribeStaffEvents } from "@/lib/realtime/shared-staff-events";

/**
 * Uma lista de pedidos da equipe ao vivo (a fila da cozinha, as entregas, os pedidos para
 * confirmar): busca pela API e busca de novo a cada aviso "order" do canal de tempo real.
 * `onNew` é chamado quando aparece um id que a tela ainda não tinha visto (o som); os que já
 * estavam ao abrir, não. Com `enabled` falso (a sessão ainda voltando, depois de um F5), espera.
 * `events` diz quais avisos fazem buscar de novo, além do "ready" (as comandas ouvem "tab" também).
 * Com o tempo real ligado, confere a lista a cada minuto.
 */
export function useLiveOrders<T extends { id: string }>(
  queryKey: readonly unknown[],
  fetch: () => Promise<T[]>,
  onNew?: () => void,
  enabled = true,
  events: readonly string[] = ["order"],
) {
  const queryClient = useQueryClient();
  const [connected, setConnected] = useState(false);
  const list = useQuery({
    queryKey,
    queryFn: fetch,
    refetchInterval: liveRefreshMs(connected),
    retry: false,
    enabled,
  });
  const key = JSON.stringify(queryKey);
  const watch = useMissedEventWatch(list.data, String(queryKey[0]));
  const names = events.join(",");

  useEffect(() => {
    if (!enabled) return;
    return subscribeStaffEvents(
      (event) => {
        watch.eventReceived();
        if (event.name === "ready" || names.split(",").includes(event.name)) {
          void queryClient.invalidateQueries({ queryKey: JSON.parse(key) });
        }
      },
      (next) => {
        watch.connectedChanged(next);
        setConnected(next);
      },
    );
  }, [queryClient, key, enabled, names, watch]);

  const seen = useRef<Set<string> | null>(null);
  const notify = useRef(onNew);
  useEffect(() => {
    notify.current = onNew;
  }, [onNew]);
  useEffect(() => {
    if (!list.data) return;
    const first = seen.current === null;
    seen.current ??= new Set();
    let fresh = false;
    for (const item of list.data) {
      if (!seen.current.has(item.id)) {
        seen.current.add(item.id);
        fresh = true;
      }
    }
    if (fresh && !first) notify.current?.();
  }, [list.data]);

  return { list, connected };
}
