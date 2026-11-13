"use client";

import { useEffect, useState } from "react";

/** O relógio do topo, a cada 15 s. Começa vazio: a hora do servidor e a do navegador diferem. */
export function useMinuteClock() {
  const [now, setNow] = useState<Date | null>(null);
  useEffect(() => {
    const tick = () => setNow(new Date());
    const first = window.setTimeout(tick, 0);
    const timer = window.setInterval(tick, 15_000);
    return () => {
      window.clearTimeout(first);
      window.clearInterval(timer);
    };
  }, []);
  return now;
}

/** "20:47", no fuso do restaurante (sem fuso, no do aparelho). */
export function clockTime(now: Date, timeZone?: string) {
  return new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit", timeZone }).format(now);
}
