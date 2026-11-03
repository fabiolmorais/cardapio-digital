"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { subscribeStaffEvents } from "@/lib/realtime/shared-staff-events";

/** Sem o tempo real (rede ruim, servidor reiniciando), a lista volta a perguntar de tempos em tempos. */
const FALLBACK_REFRESH_MS = 15_000;

/**
 * Uma lista de pedidos da equipe ao vivo (a fila da cozinha, as entregas, os pedidos para
 * confirmar): busca pela API e busca de novo a cada aviso "order" do canal de tempo real.
 * `onNew` é chamado quando aparece um id que a tela ainda não tinha visto (o som); os que já
 * estavam ao abrir, não. Com `enabled` falso (a sessão ainda voltando, depois de um F5), espera.
 */
export function useLiveOrders<T extends { id: string }>(
  queryKey: readonly unknown[],
  fetch: () => Promise<T[]>,
  onNew?: () => void,
  enabled = true,
) {
  const queryClient = useQueryClient();
  const [connected, setConnected] = useState(false);
  const list = useQuery({
    queryKey,
    queryFn: fetch,
    refetchInterval: connected ? false : FALLBACK_REFRESH_MS,
    retry: false,
    enabled,
  });
  const key = JSON.stringify(queryKey);

  useEffect(() => {
    if (!enabled) return;
    return subscribeStaffEvents((event) => {
      if (event.name === "ready" || event.name === "order") {
        void queryClient.invalidateQueries({ queryKey: JSON.parse(key) });
      }
    }, setConnected);
  }, [queryClient, key, enabled]);

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
