"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useState, useSyncExternalStore } from "react";
import { connectTvEvents } from "@/lib/realtime/device-events";
import type { ServerEvent } from "@/lib/realtime/sse-parser";
import { subscribeStaffEvents } from "@/lib/realtime/shared-staff-events";
import { isSoundOn, subscribeToSound } from "./alert-sound";
import { fetchCallQueue, fetchTvCallQueue, type StaffCall } from "./calls-api";

/** Sem o tempo real (rede ruim, servidor reiniciando), a fila volta a perguntar de tempos em tempos. */
const FALLBACK_REFRESH_MS = 15_000;

/** De onde vêm a fila e os avisos: a equipe (com login) ou a TV vinculada (pelo cookie). */
export type LiveCallsSource = {
  queryKey: string;
  fetchQueue: () => Promise<StaffCall[]>;
  connect: (onEvent: (event: ServerEvent) => void, onConnectedChange: (connected: boolean) => void) => () => void;
  /** Conferir a fila de tempos em tempos mesmo com o tempo real ligado (a TV, para notar se foi desvinculada). */
  safetyRefreshMs?: number;
};

export const staffSource: LiveCallsSource = {
  queryKey: "call-queue",
  fetchQueue: fetchCallQueue,
  connect: subscribeStaffEvents,
};

export const tvDeviceSource: LiveCallsSource = {
  queryKey: "tv-call-queue",
  fetchQueue: fetchTvCallQueue,
  connect: connectTvEvents,
  safetyRefreshMs: 60_000,
};

/**
 * A fila de chamados ao vivo: busca pela API e atualiza a cada aviso do canal de tempo real
 * ("ready" ao conectar, "call" quando um chamado muda). Chama onNewCall para cada chamado que
 * aparece pela primeira vez (o som e o alerta da TV); os que já estavam na tela ao abrir, não.
 */
export function useLiveCalls(onNewCall?: (call: StaffCall) => void, source: LiveCallsSource = staffSource) {
  const queryClient = useQueryClient();
  const [connected, setConnected] = useState(false);
  const queue = useQuery({
    queryKey: [source.queryKey],
    queryFn: source.fetchQueue,
    refetchInterval: connected ? (source.safetyRefreshMs ?? false) : FALLBACK_REFRESH_MS,
    retry: false,
  });

  useEffect(
    () =>
      source.connect((event) => {
        if (event.name === "ready" || event.name === "call") {
          // O prefixo pega também o quadro do Modo TV ([chave, "board"]).
          void queryClient.invalidateQueries({ queryKey: [source.queryKey] });
        } else if (event.name === "order") {
          void queryClient.invalidateQueries({ queryKey: [source.queryKey, "board"] });
        }
      }, setConnected),
    [queryClient, source],
  );

  // Os chamados que esta tela já viu. Na primeira carga, todos entram sem alerta.
  const seen = useRef<Set<string> | null>(null);
  const notify = useRef(onNewCall);
  useEffect(() => {
    notify.current = onNewCall;
  }, [onNewCall]);
  useEffect(() => {
    if (!queue.data) return;
    const first = seen.current === null;
    seen.current ??= new Set();
    for (const call of queue.data) {
      if (seen.current.has(call.id)) continue;
      seen.current.add(call.id);
      if (!first && call.status === "PENDING") notify.current?.(call);
    }
  }, [queue.data]);

  return { queue, connected };
}

/** Se o som está ligado nesta aba (muda quando alguém toca em "Ativar som"). */
export function useSoundOn() {
  return useSyncExternalStore(subscribeToSound, isSoundOn, () => false);
}
