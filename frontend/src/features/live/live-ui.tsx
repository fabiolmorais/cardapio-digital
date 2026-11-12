import type { ReactNode } from "react";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";

/**
 * As peças do painel e do Modo TV no desenho (7.1, 7.2, 8.1): a caixa da mesa, o cronômetro e as
 * tags. As cores vêm dos tokens do tema (success, warning, info, destructive, brand), as mesmas
 * do desenho no tema escuro.
 */

/** Os tons do desenho: o fundo é a própria cor, bem clara (a 15%), e o texto a cor cheia. */
export const TONE = {
  neutral: "bg-secondary text-muted-foreground",
  ok: "bg-success/15 text-success",
  warn: "bg-warning/15 text-warning",
  info: "bg-info/15 text-info",
  danger: "bg-destructive/15 text-destructive",
  accent: "bg-primary/15 text-brand-text",
} as const;

export type Tone = keyof typeof TONE;

/** Os passos do item, com a cor de cada um (igual à mesa): na fila, em preparo, pronto, entregue, cancelado. */
export const ITEM_TONE: Record<string, Tone> = {
  PENDING: "neutral",
  PREPARING: "warn",
  READY: "info",
  DELIVERED: "ok",
  CANCELED: "danger",
};

/** A tag com a bolinha (sem a bolinha com `plain`). */
export function Badge({
  tone,
  plain,
  className,
  children,
}: {
  tone: Tone;
  plain?: boolean;
  className?: string;
  children: ReactNode;
}) {
  return (
    <span
      className={cn(
        "inline-flex h-6 shrink-0 items-center gap-1.5 rounded-full px-2.5 text-xs font-semibold whitespace-nowrap",
        TONE[tone],
        className,
      )}
    >
      {!plain && <span aria-hidden className="size-1.5 rounded-full bg-current" />}
      {children}
    </span>
  );
}

/**
 * A caixa da mesa: "MESA" pequeno em cima e o número grande. O tom pinta a caixa (azul da conta e
 * do pronto, laranja do chamado); `size="tv"` é a do Modo TV, para ver de longe.
 */
export function TableBox({ label, tone, size = "md" }: { label: string; tone?: Tone; size?: "md" | "tv" }) {
  const tv = size === "tv";
  return (
    <span
      className={cn(
        "flex shrink-0 flex-col items-center justify-center font-bold",
        tv
          ? "size-[110px] rounded-[26px] text-[56px] leading-[56px]"
          : "h-13 min-w-13 rounded-[14px] px-2 text-xl leading-5",
        tone ? TONE[tone] : "bg-secondary",
      )}
    >
      <span
        className={cn(
          "font-semibold tracking-[.6px]",
          tv ? "text-[15px] leading-5" : "mb-0.5 text-[9px] leading-3",
          !tone && "text-muted-foreground",
        )}
      >
        {messages.panel.calls.table.toUpperCase()}
      </span>
      {label}
    </span>
  );
}

/** Até 2 min, em dia (verde); até 4 min, atenção (amarelo); depois, atrasado (vermelho). */
export const WARN_AFTER_S = 120;
export const LATE_AFTER_S = 240;

export type WaitLevel = "ok" | "warn" | "late" | "taken";

export function waitLevel(seconds: number, taken = false): WaitLevel {
  if (taken) return "taken";
  return seconds >= LATE_AFTER_S ? "late" : seconds >= WARN_AFTER_S ? "warn" : "ok";
}

const TIMER: Record<WaitLevel, string> = {
  ok: TONE.ok,
  warn: TONE.warn,
  late: TONE.danger,
  taken: TONE.neutral,
};

/** O cronômetro da espera, no fundo da cor do tempo. */
export function Timer({ level, size = "md", children }: { level: WaitLevel; size?: "md" | "tv"; children: ReactNode }) {
  return (
    <span
      className={cn(
        "inline-flex shrink-0 items-center font-mono font-semibold tabular-nums",
        size === "tv" ? "rounded-[18px] px-[18px] py-2 text-[34px] font-bold" : "h-7 rounded-full px-2.5 text-[13px]",
        TIMER[level],
      )}
    >
      {children}
    </span>
  );
}

/** O cabeçalho de cada coluna do "Ao vivo": o título, a contagem e, se houver, um texto à direita. */
export function ColumnHead({
  id,
  title,
  count,
  hint,
  tone,
}: {
  id: string;
  title: string;
  count: number;
  hint?: string;
  tone?: Tone;
}) {
  return (
    <div className="flex items-center gap-2.5 px-4 pt-4 pb-3">
      <h2 id={id} className="text-xl leading-[26px] font-semibold tracking-tight">
        {title}
      </h2>
      <span
        className={cn(
          "flex h-6 min-w-6 items-center justify-center rounded-full px-2 text-xs font-bold",
          tone ? TONE[tone] : "bg-secondary",
        )}
      >
        {count}
      </span>
      {hint && <span className="ml-auto text-[13px] text-muted-foreground/80">{hint}</span>}
    </div>
  );
}

/** "4:12": minutos e segundos de espera. */
export function elapsed(seconds: number) {
  const s = Math.max(0, Math.floor(seconds));
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;
}
