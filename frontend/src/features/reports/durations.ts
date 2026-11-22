import { messages } from "@/messages/pt-BR";
import type { Delta } from "./charts";

const t = messages.reports;
const k = t.kitchen;

/**
 * Um tempo como a cozinha fala: "45 s", "3 min 40 s", "12 min" (de 10 min para cima, sem os segundos)
 * e "1 h 05 min".
 */
export function formatDuration(totalSeconds: number) {
  const seconds = Math.max(0, Math.round(totalSeconds));
  if (seconds < 60) return `${seconds} s`;
  if (seconds < 600) {
    const rest = seconds % 60;
    return rest === 0 ? `${seconds / 60} min` : `${Math.floor(seconds / 60)} min ${rest} s`;
  }
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes} min`;
  return `${Math.floor(minutes / 60)} h ${String(minutes % 60).padStart(2, "0")} min`;
}

/** A variação de um tempo: menos é melhor ("▼ 1 min, mais rápido", em verde). */
export function timeVariation(current: number, previous: number): Delta {
  const change = formatDuration(Math.abs(current - previous));
  if (Math.abs(current - previous) < 1) return { text: t.delta.same, tone: "flat" };
  return current < previous ? { text: k.faster(change), tone: "up" } : { text: k.slower(change), tone: "down" };
}
