"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { liveRefreshMs } from "@/lib/realtime/missed-events";
import { subscribeStaffEvents } from "@/lib/realtime/shared-staff-events";
import { fetchLiveFloor } from "./live-api";

export const LIVE_FLOOR_KEY = ["live-floor"] as const;
/** Os avisos que mudam o mapa: chamado, pedido, comanda, rodízio e celular entrando ou saindo. */
const EVENTS = ["ready", "call", "order", "tab", "rodizio", "presence", "phone"];

/**
 * O salão ao vivo (mesas e resumo), atualizado a cada aviso do tempo real e conferido a cada
 * minuto. Sem o alerta de aviso perdido: o tempo médio de espera muda sozinho com o relógio.
 */
export function useLiveFloor(enabled = true) {
  const queryClient = useQueryClient();
  const [connected, setConnected] = useState(false);
  const floor = useQuery({
    queryKey: LIVE_FLOOR_KEY,
    queryFn: fetchLiveFloor,
    refetchInterval: liveRefreshMs(connected),
    retry: false,
    enabled,
  });
  useEffect(() => {
    if (!enabled) return;
    return subscribeStaffEvents((event) => {
      if (EVENTS.includes(event.name)) void queryClient.invalidateQueries({ queryKey: LIVE_FLOOR_KEY });
    }, setConnected);
  }, [queryClient, enabled]);
  return floor;
}

/** Se o tempo real está ligado (para o "tempo real" do topo virar "reconectando…"). */
export function useStaffConnected() {
  const [connected, setConnected] = useState(false);
  useEffect(() => subscribeStaffEvents(() => {}, setConnected), []);
  return connected;
}
