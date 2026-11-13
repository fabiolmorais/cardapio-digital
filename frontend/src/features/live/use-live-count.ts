"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { fetchCallQueue } from "@/features/calls/calls-api";
import { fetchKitchenQueue, fetchWaitingOrders } from "@/features/orders/orders-api";
import { liveRefreshMs } from "@/lib/realtime/missed-events";
import { subscribeStaffEvents } from "@/lib/realtime/shared-staff-events";

type Role = "OWNER" | "MANAGER" | "STAFF" | "KITCHEN";

/**
 * O número ao lado do "Ao vivo" no menu (desenho 7.1): o que espera alguém agora (chamados
 * abertos, pedidos para confirmar e itens prontos para levar). Usa as mesmas listas do Ao vivo
 * (mesma chave, sem buscar duas vezes) e atualiza pelo tempo real em qualquer tela do painel,
 * conferindo a cada minuto.
 */
export function useLiveCount(role: Role | undefined) {
  const queryClient = useQueryClient();
  const enabled = role !== undefined;
  const [connected, setConnected] = useState(false);
  const refetchInterval = liveRefreshMs(connected);
  const calls = useQuery({ queryKey: ["call-queue"], queryFn: fetchCallQueue, enabled, refetchInterval, retry: false });
  const waiting = useQuery({
    queryKey: ["waiting-orders"],
    queryFn: fetchWaitingOrders,
    enabled: enabled && role !== "KITCHEN",
    refetchInterval,
    retry: false,
  });
  const kitchen = useQuery({
    queryKey: ["kitchen-queue"],
    queryFn: () => fetchKitchenQueue(),
    enabled,
    refetchInterval,
    retry: false,
  });

  useEffect(() => {
    if (!enabled) return;
    return subscribeStaffEvents((event) => {
      if (event.name === "ready" || event.name === "call") {
        void queryClient.invalidateQueries({ queryKey: ["call-queue"] });
      }
      if (event.name === "ready" || event.name === "order") {
        void queryClient.invalidateQueries({ queryKey: ["waiting-orders"] });
        void queryClient.invalidateQueries({ queryKey: ["kitchen-queue"] });
      }
    }, setConnected);
  }, [queryClient, enabled]);

  return (
    (calls.data?.length ?? 0) +
    (waiting.data?.length ?? 0) +
    (kitchen.data ?? []).filter((item) => item.status === "READY").length
  );
}
