"use client";

import { useState } from "react";
import { Input } from "@/components/ui/input";
import { centsToInput, formatCents, parseCents } from "@/lib/format";
import { dueWith, pickCents, validCustom, type ServicePick } from "@/lib/service-pick";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import type { Schemas } from "@/lib/api/client";

const t = messages.panel.service;

export type ServiceOffer = Schemas["ServiceOffer"];

/** O nome da linha do serviço no resumo: o percentual, ou "outro valor". */
export function serviceLabel(service: ServiceOffer, pick: ServicePick): string {
  return pick.kind === "CUSTOM" ? t.customLabel : t.label(service.percent);
}

/** O que falta receber com esta escolha; sem taxa, o que o back disse. */
export function dueOf(
  service: ServiceOffer | null | undefined,
  pick: ServicePick,
  due: { dueCents: number; totalCents: number; paidCents: number },
): number {
  return service ? dueWith(service, pick, due.totalCents, due.paidCents) : due.dueCents;
}

/**
 * A taxa de serviço opcional (RF39) para o garçom: o percentual, para quem vai e o valor. Começa
 * como a mesa escolheu; o garçom troca se o cliente mudar de ideia na maquininha. Sem "outro valor"
 * no restaurante, é o interruptor (cobrar ou não o percentual); com ele, as três opções e o campo.
 */
export function ServiceSwitch({
  service,
  pick,
  onChange,
  disabled = false,
}: {
  service: ServiceOffer;
  pick: ServicePick;
  onChange: (next: ServicePick) => void;
  disabled?: boolean;
}) {
  const [otherText, setOtherText] = useState(
    pick.kind === "CUSTOM" && pick.cents !== null ? centsToInput(pick.cents) : "",
  );
  const destination = service.waiterName ? t.forWaiter(service.waiterName) : t.forTeam;

  if (!service.customAllowed) {
    const on = pick.kind !== "NONE";
    return (
      <label className="relative flex min-h-12 cursor-pointer items-center gap-3 rounded-xl border border-border px-4 py-2.5 has-focus-visible:ring-3 has-focus-visible:ring-ring/50">
        <span className="flex flex-1 flex-col">
          <span className="font-semibold">{t.label(service.percent)}</span>
          <span className="text-sm text-muted-foreground">{destination}</span>
        </span>
        <span className={cn("font-mono tabular-nums", !on && "text-muted-foreground line-through")}>
          {formatCents(service.percentAmountCents)}
        </span>
        <input
          type="checkbox"
          role="switch"
          aria-label={t.switch(service.percent)}
          checked={on}
          disabled={disabled}
          onChange={(event) => onChange({ kind: event.target.checked ? "PERCENT" : "NONE" })}
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

  const otherCents = parseCents(otherText);
  const otherInvalid = pick.kind === "CUSTOM" && otherText.trim() !== "" && !validCustom(otherCents);
  const options: { kind: ServicePick["kind"]; label: string; hint?: string }[] = [
    { kind: "PERCENT", label: t.percentOption(service.percent), hint: formatCents(service.percentAmountCents) },
    { kind: "CUSTOM", label: t.other },
    { kind: "NONE", label: t.none },
  ];

  return (
    <fieldset disabled={disabled} className="flex flex-col gap-2.5 rounded-xl border border-border px-4 py-3">
      <legend className="sr-only">{t.choices}</legend>
      <div className="flex items-start gap-3">
        <span className="flex flex-1 flex-col">
          <span className="font-semibold">{t.choices}</span>
          <span className="text-sm text-muted-foreground">{destination}</span>
        </span>
        <span className="font-mono tabular-nums">{formatCents(pickCents(service, pick))}</span>
      </div>
      <div className="grid grid-cols-3 gap-2">
        {options.map(({ kind, label, hint }) => (
          <label
            key={kind}
            className={cn(
              "relative flex min-h-12 cursor-pointer flex-col items-center justify-center rounded-xl border border-border px-1 text-center leading-tight has-focus-visible:ring-3 has-focus-visible:ring-ring/50",
              pick.kind === kind && "border-emerald-600 bg-emerald-700/15",
            )}
          >
            <input
              type="radio"
              name="staff-service-choice"
              value={kind}
              checked={pick.kind === kind}
              aria-label={hint ? `${label}, ${hint}` : label}
              onChange={() =>
                onChange(
                  kind === "CUSTOM"
                    ? { kind, cents: validCustom(otherCents) ? otherCents : null }
                    : { kind: kind as "PERCENT" | "NONE" },
                )
              }
              className="absolute inset-0 size-full cursor-pointer opacity-0"
            />
            <span className="text-sm font-semibold">{label}</span>
            {hint && <span className="font-mono text-xs text-muted-foreground tabular-nums">{hint}</span>}
          </label>
        ))}
      </div>
      {pick.kind === "CUSTOM" && (
        <div className="flex flex-col gap-1.5">
          <Input
            value={otherText}
            inputMode="decimal"
            autoComplete="off"
            placeholder="0,00"
            aria-label={t.otherLabel}
            aria-invalid={otherInvalid}
            onChange={(event) => {
              const text = event.target.value.replace(/[^\d.,]/g, "").slice(0, 9);
              setOtherText(text);
              const cents = parseCents(text);
              onChange({ kind: "CUSTOM", cents: validCustom(cents) ? cents : null });
            }}
            className="h-11 rounded-xl px-4 font-mono"
          />
          {otherInvalid && <span className="text-sm text-destructive">{t.otherInvalid}</span>}
        </div>
      )}
    </fieldset>
  );
}
