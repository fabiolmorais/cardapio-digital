import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import type { Schemas } from "@/lib/api/client";

const t = messages.panel.service;

export type ServiceOffer = Schemas["ServiceOffer"];

/**
 * A linha da taxa de serviço opcional (RF39) para o garçom: o percentual, para quem vai, o valor e o
 * interruptor. Começa como a mesa escolheu; o garçom troca se o cliente mudar de ideia na maquininha.
 */
export function ServiceSwitch({
  service,
  on,
  onChange,
  disabled = false,
}: {
  service: ServiceOffer;
  on: boolean;
  onChange: (next: boolean) => void;
  disabled?: boolean;
}) {
  return (
    <label className="flex min-h-12 cursor-pointer items-center gap-3 rounded-xl border border-border px-4 py-2.5 has-focus-visible:ring-3 has-focus-visible:ring-ring/50">
      <span className="flex flex-1 flex-col">
        <span className="font-semibold">{t.label(service.percent)}</span>
        <span className="text-sm text-muted-foreground">
          {service.waiterName ? t.forWaiter(service.waiterName) : t.forTeam}
        </span>
      </span>
      <span className={cn("font-mono tabular-nums", !on && "text-muted-foreground line-through")}>
        {formatCents(service.amountCents)}
      </span>
      <input
        type="checkbox"
        role="switch"
        aria-label={t.switch(service.percent)}
        checked={on}
        disabled={disabled}
        onChange={(event) => onChange(event.target.checked)}
        className="peer sr-only"
      />
      <span
        aria-hidden
        className={cn(
          "relative h-7 w-12 shrink-0 rounded-full border border-border transition-colors",
          on ? "bg-emerald-700" : "bg-muted",
        )}
      >
        <span
          className={cn(
            "absolute top-0.5 size-5.5 rounded-full bg-white transition-[left]",
            on ? "left-[22px]" : "left-0.5",
          )}
        />
      </span>
    </label>
  );
}

/** O que falta receber, com ou sem a taxa (os dois vêm calculados do back). */
export function dueOf(service: ServiceOffer | null | undefined, on: boolean, dueCents: number): number {
  if (!service) return dueCents;
  return on ? service.dueWithCents : service.dueWithoutCents;
}
