import { UtensilsCrossed } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "cn";

/**
 * As cores de cada passo do item, na ordem do desenho (4.2 a 4.4): na fila (neutro, com borda),
 * em preparo (amarelo), pronto (azul), entregue (verde) e cancelado (vermelho). A mesma tag em
 * todas as telas de pedido.
 */
const TONES = {
  PENDING: "border border-(--mesa-stroke) bg-(--mesa-glass) text-(--mesa-muted)",
  PREPARING: "bg-(--mesa-warn-soft) text-(--mesa-warn)",
  READY: "bg-(--mesa-info-soft) text-(--mesa-info)",
  DELIVERED: "bg-(--mesa-ok-soft) text-(--mesa-ok)",
  CANCELED: "bg-(--mesa-danger-soft) text-(--mesa-danger)",
} as const;

export type StatusTone = keyof typeof TONES;

/** A tag do passo, com a bolinha na cor do texto (como no desenho). */
export function StatusBadge({ tone, children }: { tone: StatusTone; children: ReactNode }) {
  return (
    <span
      className={cn(
        "inline-flex h-6 shrink-0 items-center gap-1.5 rounded-full px-2.5 text-xs font-semibold whitespace-nowrap",
        TONES[tone],
      )}
    >
      <span aria-hidden className="size-1.5 rounded-full bg-current" />
      {children}
    </span>
  );
}

/** A miniatura do prato no pedido; sem foto, o ícone de talheres no mesmo lugar. */
export function ItemThumb({ src, size }: { src: string | null | undefined; size: "sm" | "md" }) {
  const box = size === "sm" ? "size-11 rounded-xl" : "size-15 rounded-[14px]";
  return (
    <span className={cn("flex shrink-0 overflow-hidden bg-(--mesa-stroke)", box)}>
      {src ? (
        // eslint-disable-next-line @next/next/no-img-element -- miniatura já no tamanho certo, vinda do R2
        <img src={src} alt="" width={60} height={60} loading="lazy" className="size-full object-cover" />
      ) : (
        <span className="flex size-full items-center justify-center text-(--mesa-muted)">
          <UtensilsCrossed aria-hidden className="size-4" />
        </span>
      )}
    </span>
  );
}
