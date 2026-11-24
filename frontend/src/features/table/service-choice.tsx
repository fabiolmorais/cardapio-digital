"use client";

import { useState } from "react";
import { centsToInput, formatCents, parseCents } from "@/lib/format";
import { pickCents, SERVICE_SHORTCUTS, validCustom, type ServicePick } from "@/lib/service-pick";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";
import type { TableOrders } from "./table-orders";

const t = tablePage.bill;

type Offer = NonNullable<TableOrders["service"]>;
type Option = "PERCENT" | `${(typeof SERVICE_SHORTCUTS)[number]}` | "OTHER" | "NONE";

/** O botão marcado para esta escolha ("Outro" aberto ganha dos atalhos). */
function optionOf(pick: ServicePick, otherOpen: boolean): Option {
  if (pick.kind === "PERCENT" || pick.kind === "NONE") return pick.kind;
  if (!otherOpen && SERVICE_SHORTCUTS.some((c) => c === pick.cents)) return `${pick.cents}` as Option;
  return "OTHER";
}

/**
 * A taxa de serviço na conta do celular (RF39). Sem "outro valor", é o interruptor de sempre
 * (aceitar ou tirar o percentual). Com "outro valor", são as opções: o percentual, R$ 5, R$ 10,
 * "Outro" (com o campo) e "Sem". {@code onChange} muda a tela; {@code onCommit} é quando a escolha
 * está feita (no campo, ao sair dele ou no Enter).
 */
export function ServiceChoice({
  service,
  pick,
  disabled,
  onChange,
  onCommit,
}: {
  service: Offer;
  pick: ServicePick;
  disabled: boolean;
  onChange: (pick: ServicePick) => void;
  onCommit: (pick: ServicePick) => void;
}) {
  const startsOther = pick.kind === "CUSTOM" && !SERVICE_SHORTCUTS.some((c) => c === pick.cents);
  const [otherOpen, setOtherOpen] = useState(startsOther);
  const [otherText, setOtherText] = useState(
    startsOther && pick.kind === "CUSTOM" && pick.cents !== null ? centsToInput(pick.cents) : "",
  );
  const amount = pickCents(service, pick);
  const destination = service.waiterName ? t.serviceWaiter(service.waiterName) : t.serviceTeam;

  if (!service.customAllowed) {
    const on = pick.kind !== "NONE";
    return (
      <label className="flex min-h-11 cursor-pointer items-center gap-3">
        <span className="flex flex-1 flex-col">
          <span className="font-semibold">{t.service(service.percent)}</span>
          <span className="text-sm text-(--mesa-muted)">{destination}</span>
        </span>
        <span className={cn("font-semibold tabular-nums", !on && "text-(--mesa-muted) line-through")}>
          {formatCents(service.percentAmountCents)}
        </span>
        {/* Interruptor de verdade (checkbox com papel de switch), com alvo de toque grande. */}
        <input
          type="checkbox"
          role="switch"
          aria-label={t.serviceSwitch(service.percent)}
          checked={on}
          disabled={disabled}
          onChange={(e) => {
            const next: ServicePick = { kind: e.target.checked ? "PERCENT" : "NONE" };
            onChange(next);
            onCommit(next);
          }}
          className="peer sr-only"
        />
        <span
          aria-hidden
          className={cn(
            "relative h-7 w-12 shrink-0 rounded-full border border-(--mesa-stroke) transition-colors peer-focus-visible:ring-3 peer-focus-visible:ring-(--brand)",
            on ? "bg-(--brand)" : "bg-(--mesa-glass)",
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

  const selected = optionOf(pick, otherOpen);
  const otherCents = parseCents(otherText);
  const otherInvalid = selected === "OTHER" && otherText.trim() !== "" && !validCustom(otherCents);
  const options: { id: Option; label: string; hint?: string; sr?: string }[] = [
    { id: "PERCENT", label: `${service.percent}%`, hint: formatCents(service.percentAmountCents) },
    ...SERVICE_SHORTCUTS.map((cents) => ({ id: `${cents}` as Option, label: formatCents(cents).replace(",00", "") })),
    { id: "OTHER", label: t.serviceOther, sr: t.serviceOtherLabel },
    { id: "NONE", label: t.serviceNone, sr: t.serviceNoneLabel },
  ];
  const choose = (id: Option) => {
    setOtherOpen(id === "OTHER");
    let next: ServicePick;
    if (id === "PERCENT" || id === "NONE") next = { kind: id };
    else if (id === "OTHER") next = { kind: "CUSTOM", cents: validCustom(otherCents) ? otherCents : null };
    else next = { kind: "CUSTOM", cents: Number(id) };
    onChange(next);
    if (id !== "OTHER" || validCustom(otherCents)) onCommit(next);
  };

  return (
    <fieldset className="flex flex-col gap-2.5" disabled={disabled}>
      <legend className="sr-only">{t.serviceChoices}</legend>
      <div className="flex items-start gap-3">
        <span className="flex flex-1 flex-col">
          <span className="font-semibold">{t.serviceTitle}</span>
          <span className="text-sm text-(--mesa-muted)">{destination}</span>
        </span>
        <span className={cn("font-semibold tabular-nums", amount === 0 && "text-(--mesa-muted)")}>
          {formatCents(amount)}
        </span>
      </div>
      <div className="grid grid-cols-5 gap-1.5">
        {options.map(({ id, label, hint, sr }) => (
          <label
            key={id}
            className={cn(
              "relative flex min-h-12 cursor-pointer flex-col items-center justify-center rounded-xl border border-(--mesa-stroke) bg-(--mesa-glass) px-1 text-center leading-tight has-focus-visible:ring-3 has-focus-visible:ring-(--brand)",
              selected === id && "border-(--brand) bg-(--brand)/15",
            )}
          >
            <input
              type="radio"
              name="service-choice"
              value={id}
              checked={selected === id}
              onChange={() => choose(id)}
              aria-label={sr ?? (hint ? `${label}, ${hint}` : label)}
              className="absolute inset-0 size-full cursor-pointer opacity-0"
            />
            <span className="text-sm font-semibold">{label}</span>
            {hint && <span className="text-[11px] text-(--mesa-muted) tabular-nums">{hint}</span>}
          </label>
        ))}
      </div>
      {selected === "OTHER" && (
        <div className="flex flex-col gap-1.5">
          <div
            className={cn(
              "flex h-12 items-center gap-2 rounded-xl border border-(--mesa-stroke) bg-(--mesa-glass) px-3.5 focus-within:ring-3 focus-within:ring-(--brand)",
              otherInvalid && "border-(--mesa-danger)",
            )}
          >
            <span aria-hidden className="text-sm font-semibold text-(--mesa-muted)">
              R$
            </span>
            <input
              autoFocus
              inputMode="decimal"
              autoComplete="off"
              placeholder="0,00"
              aria-label={t.serviceOtherLabel}
              aria-invalid={otherInvalid}
              aria-describedby={otherInvalid ? "service-other-error" : undefined}
              value={otherText}
              onChange={(e) => {
                const text = e.target.value.replace(/[^\d.,]/g, "").slice(0, 9);
                setOtherText(text);
                const cents = parseCents(text);
                onChange({ kind: "CUSTOM", cents: validCustom(cents) ? cents : null });
              }}
              onBlur={() => validCustom(otherCents) && onCommit({ kind: "CUSTOM", cents: otherCents })}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  if (validCustom(otherCents)) onCommit({ kind: "CUSTOM", cents: otherCents });
                }
              }}
              className="h-full min-w-0 flex-1 bg-transparent text-base font-semibold tabular-nums outline-none"
            />
          </div>
          {otherInvalid && (
            <span id="service-other-error" className="text-sm text-(--mesa-danger)">
              {t.serviceOtherInvalid}
            </span>
          )}
        </div>
      )}
    </fieldset>
  );
}
