import { messages } from "@/messages/pt-BR";
import { cn } from "cn";

/**
 * O lugar da comanda, no quadrado à esquerda das listas: "MESA 04" ou, no balcão (ADR 0015),
 * "BALCÃO João"; o title diz o lugar inteiro ("Balcão do bar · João"). O nome comprido corta com reticências; o texto inteiro vai no title.
 */
export function PlaceBadge({
  tableLabel,
  counterName,
  size = "md",
}: {
  tableLabel: string;
  counterName?: string | null;
  size?: "md" | "lg";
}) {
  const counter = counterName != null;
  return (
    <span
      title={counter ? messages.panel.counter.label(tableLabel, counterName) : undefined}
      className={cn(
        "flex shrink-0 flex-col items-center justify-center rounded-xl leading-none",
        size === "lg" ? "h-14 min-w-14" : "h-12 min-w-12",
        counter ? "max-w-28 bg-primary/15 px-2 text-brand-text" : "bg-muted",
      )}
    >
      <span
        className={cn(
          "text-[10px] font-semibold tracking-wide uppercase",
          counter ? "text-brand-text" : "text-muted-foreground",
        )}
      >
        {counter ? messages.panel.counter.tag : messages.panel.calls.table}
      </span>
      <span className={cn("max-w-full truncate font-bold", size === "lg" ? "text-xl" : "text-lg")}>
        {counter ? counterName : tableLabel}
      </span>
    </span>
  );
}
