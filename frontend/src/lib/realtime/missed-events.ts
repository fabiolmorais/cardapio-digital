"use client";

import { useEffect, useMemo, useRef } from "react";

/**
 * Com o tempo real ligado, as listas da equipe ainda conferem tudo a cada minuto: se um aviso se
 * perder no caminho (a escuta do banco surda, um proxy que engoliu o evento), a tela se corrige
 * sozinha em até 1 minuto em vez de ficar parada até alguém recarregar.
 */
export const SAFETY_REFRESH_MS = 60_000;
/** Sem o tempo real (rede ruim, servidor reiniciando), as listas perguntam bem mais seguido. */
export const FALLBACK_REFRESH_MS = 15_000;

/** O intervalo de busca de uma lista ao vivo: a conferência com o tempo real ligado, a reserva sem ele. */
export function liveRefreshMs(connected: boolean) {
  return connected ? SAFETY_REFRESH_MS : FALLBACK_REFRESH_MS;
}

/** Folga entre um aviso e a mudança que ele causou (a busca, a rede, uma ação da própria tela). */
const GRACE_MS = 5_000;
/** No máximo um alerta a cada 10 minutos por aba: um problema não vira uma enxurrada no Sentry. */
const REPORT_EVERY_MS = 10 * 60_000;
let lastReport = 0;

/**
 * Manda ao Sentry (se configurado) que uma lista mudou sem aviso do tempo real. Só o nome da lista
 * vai junto: nada do restaurante, da mesa ou do cliente.
 */
function reportMissedEvent(list: string) {
  const now = Date.now();
  if (now - lastReport < REPORT_EVERY_MS) return;
  lastReport = now;
  if (!process.env.NEXT_PUBLIC_SENTRY_DSN) return;
  void import("@sentry/nextjs").then((Sentry) =>
    Sentry.captureMessage("Tempo real: a lista mudou sem aviso", {
      level: "warning",
      tags: { realtime: "missed-event", list },
    }),
  );
}

export type MissedEventWatch = {
  /** Chamar a cada aviso recebido do tempo real (qualquer um). */
  eventReceived: () => void;
  /** Chamar quando a conexão do tempo real liga ou cai. */
  connectedChanged: (connected: boolean) => void;
};

/**
 * Vigia uma lista ao vivo: se os dados mudarem com o tempo real ligado e sem nenhum aviso por
 * perto (5 s antes ou depois), a mudança só pode ter vindo da conferência de segurança, ou seja,
 * um aviso se perdeu. Aí avisa o Sentry. Mudanças durante a reconexão não contam: o "ready" da
 * conexão nova já faz buscar tudo.
 *
 * `data` precisa manter a mesma referência quando nada muda (o TanStack Query já faz isso).
 */
export function useMissedEventWatch(data: unknown, list: string): MissedEventWatch {
  const lastEvent = useRef(0);
  const connectedSince = useRef<number | null>(null);
  const previous = useRef(data);

  useEffect(() => {
    const before = previous.current;
    previous.current = data;
    if (before === undefined || data === undefined || before === data) return;
    const changedAt = Date.now();
    const since = connectedSince.current;
    if (since === null || changedAt - since < GRACE_MS) return;
    const timer = setTimeout(() => {
      if (connectedSince.current === null) return;
      if (lastEvent.current >= changedAt - GRACE_MS) return;
      reportMissedEvent(list);
    }, GRACE_MS);
    return () => clearTimeout(timer);
  }, [data, list]);

  return useMemo(
    () => ({
      eventReceived: () => {
        lastEvent.current = Date.now();
      },
      connectedChanged: (connected: boolean) => {
        if (!connected) connectedSince.current = null;
        else connectedSince.current ??= Date.now();
      },
    }),
    [],
  );
}
