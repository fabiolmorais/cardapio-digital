import { useQuery } from "@tanstack/react-query";
import { api, type Schemas } from "@/lib/api/client";

export type Subscription = Schemas["Subscription"];

export async function fetchSubscription(): Promise<Subscription> {
  const { data, response } = await api.GET("/api/v1/subscription");
  if (!data) throw new Error(String(response.status));
  return data;
}

/** A assinatura do restaurante. Só dono e gerente a veem (para os outros, o back responde 403). */
export function useSubscription(enabled: boolean) {
  return useQuery({
    queryKey: ["subscription"],
    queryFn: fetchSubscription,
    enabled,
    retry: false,
  });
}

export function canSeeSubscription(role: string) {
  return role === "OWNER" || role === "MANAGER";
}

const DAY_MS = 24 * 60 * 60 * 1000;

function calendarDay(instant: number, timeZone: string) {
  const ymd = new Intl.DateTimeFormat("en-CA", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(instant);
  return Date.parse(`${ymd}T00:00:00Z`);
}

/** Quantos dias do calendário do restaurante faltam até a data: 0 se é hoje, 1 se é amanhã. */
export function daysUntil(iso: string, now: number, timeZone: string) {
  return Math.round((calendarDay(new Date(iso).getTime(), timeZone) - calendarDay(now, timeZone)) / DAY_MS);
}

/** A data no fuso do restaurante, como 19/11/2026. */
export function formatDate(iso: string, timeZone: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    timeZone,
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(iso));
}
