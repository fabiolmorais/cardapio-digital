"use client";

import { Check, Minus, Plus } from "lucide-react";
import { useId } from "react";
import { formatCents } from "@/lib/format";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";
import { inRound, optionPrice, rulesOf, type GroupsById, type Picked } from "./choices";
import type { MenuItem } from "./table-data";

const t = tablePage.choices;
const focus = "focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none";
const chip =
  "flex min-h-11 items-center gap-2 rounded-2xl border px-3.5 py-2 text-left text-[15px] transition-colors disabled:opacity-45";
const on = "border-(--brand) bg-(--brand)/15";
const off = "border-(--mesa-stroke) bg-(--mesa-glass)";

/**
 * As escolhas do prato (ADR 0010): o tamanho e cada grupo, com a regra ("Escolha 1", "Opcional · até
 * 5") e o "Obrigatório". Um grupo de uma escolha só troca a escolha num toque; os outros marcam e
 * desmarcam, e os que repetem ("2× bacon") têm − e +.
 *
 * @param round a rodada do rodízio: sem preços, sem tamanho e sem os adicionais pagos
 */
export function ChoicePicker({
  item,
  groups,
  variantId,
  onVariant,
  picked,
  onPicked,
  round = false,
}: {
  item: MenuItem;
  groups: GroupsById;
  variantId: string | null;
  onVariant: (id: string) => void;
  picked: Picked;
  onPicked: (picked: Picked) => void;
  round?: boolean;
}) {
  const id = useId();
  return (
    <div className="flex flex-col gap-5">
      {!round && item.variants.length > 0 && (
        <div role="group" aria-labelledby={`${id}size`} className="flex flex-col gap-2.5">
          <Heading id={`${id}size`} title={t.size} rule={null} required />
          <div className="flex flex-wrap gap-2">
            {item.variants.map((v) => (
              <button
                key={v.id}
                type="button"
                aria-pressed={variantId === v.id}
                disabled={!v.available}
                onClick={() => onVariant(v.id)}
                className={cn(chip, focus, variantId === v.id ? on : off)}
              >
                <span className="font-semibold">{v.name}</span>
                {v.available ? (
                  v.priceCents > 0 && (
                    <span className="text-sm text-(--mesa-muted)">
                      {formatCents(v.promoPriceCents ?? v.priceCents)}
                    </span>
                  )
                ) : (
                  <span className="text-xs text-(--mesa-muted)">{t.soldOut}</span>
                )}
              </button>
            ))}
          </div>
        </div>
      )}

      {item.optionGroups.map((link) => {
        const group = groups.get(link.groupId);
        if (!group) return null;
        const rules = rulesOf(group, link, variantId);
        const options = group.items.filter(
          (o) => !round || inRound(rules, optionPrice(item, o.id, o.priceCents, variantId)),
        );
        if (options.length === 0) return null;
        const count = options.reduce((sum, o) => sum + (picked[o.id] ?? 0), 0);
        const single = rules.max === 1 && rules.maxPerOption === 1;
        const headingId = `${id}${group.id}`;

        function set(optionId: string, quantity: number) {
          const next = { ...picked };
          if (quantity > 0) next[optionId] = quantity;
          else delete next[optionId];
          onPicked(next);
        }

        return (
          <div key={group.id} role="group" aria-labelledby={headingId} className="flex flex-col gap-2.5">
            <Heading
              id={headingId}
              title={group.title}
              rule={[
                t.rule(rules.min, rules.max),
                rules.free > 0 && !round ? t.free(rules.free) : null,
                rules.splits ? t.splits : null,
              ]
                .filter(Boolean)
                .join(" · ")}
              required={rules.min > 0}
            />
            <ul className="flex flex-col gap-2">
              {options.map((option) => {
                const quantity = picked[option.id] ?? 0;
                const price = optionPrice(item, option.id, option.priceCents, variantId);
                const priceText =
                  round || price === 0
                    ? null
                    : rules.pricing === "SUM"
                      ? `+ ${formatCents(price)}`
                      : formatCents(price);
                const full = count >= rules.max;
                const repeats = rules.maxPerOption > 1;
                return (
                  <li key={option.id}>
                    {repeats ? (
                      <div className={cn(chip, "justify-between", quantity > 0 ? on : off)}>
                        <span className={cn("flex flex-col", !option.available && "line-through")}>
                          <span>{option.name}</span>
                          {priceText && <span className="text-sm text-(--mesa-muted)">{priceText}</span>}
                        </span>
                        <span className="flex items-center">
                          <button
                            type="button"
                            aria-label={t.less(option.name)}
                            disabled={quantity === 0}
                            onClick={() => set(option.id, quantity - 1)}
                            className={cn(
                              "flex size-10 items-center justify-center rounded-full disabled:opacity-40",
                              focus,
                            )}
                          >
                            <Minus aria-hidden className="size-4" />
                          </button>
                          <span aria-live="polite" className="w-6 text-center font-bold">
                            {quantity}
                          </span>
                          <button
                            type="button"
                            aria-label={t.more(option.name)}
                            disabled={!option.available || full || quantity >= rules.maxPerOption}
                            onClick={() => set(option.id, quantity + 1)}
                            className={cn(
                              "flex size-10 items-center justify-center rounded-full disabled:opacity-40",
                              focus,
                            )}
                          >
                            <Plus aria-hidden className="size-4" />
                          </button>
                        </span>
                      </div>
                    ) : (
                      <button
                        type="button"
                        aria-pressed={quantity > 0}
                        disabled={!option.available || (quantity === 0 && full && !single)}
                        onClick={() => {
                          if (quantity > 0) {
                            // Num grupo de uma escolha obrigatória, tocar na marcada não desmarca.
                            if (!(single && rules.min > 0)) set(option.id, 0);
                          } else if (single) {
                            const next = { ...picked };
                            options.forEach((o) => delete next[o.id]);
                            onPicked({ ...next, [option.id]: 1 });
                          } else {
                            set(option.id, 1);
                          }
                        }}
                        className={cn(chip, "w-full justify-between", focus, quantity > 0 ? on : off)}
                      >
                        <span className={cn("flex flex-col", !option.available && "line-through")}>
                          <span>{option.name}</span>
                          {!option.available ? (
                            <span className="text-xs text-(--mesa-muted)">{t.soldOut}</span>
                          ) : (
                            priceText && <span className="text-sm text-(--mesa-muted)">{priceText}</span>
                          )}
                        </span>
                        <span
                          aria-hidden
                          className={cn(
                            "flex size-6 shrink-0 items-center justify-center border-2",
                            single ? "rounded-full" : "rounded-md",
                            quantity > 0 ? "border-(--brand) bg-(--brand) text-white" : "border-(--mesa-stroke)",
                          )}
                        >
                          {quantity > 0 && <Check className="size-3.5" />}
                        </span>
                      </button>
                    )}
                  </li>
                );
              })}
            </ul>
          </div>
        );
      })}
    </div>
  );
}

function Heading({ id, title, rule, required }: { id: string; title: string; rule: string | null; required: boolean }) {
  return (
    <div className="flex items-start justify-between gap-3">
      <span className="flex flex-col">
        <span id={id} className="font-semibold">
          {title}
        </span>
        {rule && <span className="text-sm text-(--mesa-muted)">{rule}</span>}
      </span>
      {required && (
        <span className="shrink-0 rounded-full bg-(--mesa-info-soft) px-2.5 py-1 text-xs font-semibold text-(--mesa-info)">
          {t.required}
        </span>
      )}
    </div>
  );
}
