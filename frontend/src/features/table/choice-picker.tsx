"use client";

import { Check, Minus, Play, Plus, Search, UtensilsCrossed } from "lucide-react";
import { lazy, Suspense, useId, useState } from "react";
import { createPortal } from "react-dom";
import { formatCents } from "@/lib/format";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";
import {
  hasPhotos,
  inRound,
  optionPrice,
  rulesOf,
  searchable,
  type GroupsById,
  type OptionGroup,
  type Picked,
} from "./choices";
import type { MenuItem } from "./table-data";

const t = tablePage.choices;
const focus = "focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none";
const chip =
  "flex min-h-11 items-center gap-2 rounded-2xl border px-3.5 py-2 text-left text-[15px] transition-colors disabled:opacity-45";
const on = "border-(--brand) bg-(--brand)/15";
const off = "border-(--mesa-stroke) bg-(--mesa-glass)";
/** Com mais opções que isto (somando os grupos), a escolha ganha a busca pelo nome. */
const SEARCH_FROM = 8;

/** O Reels das opções, carregado só quando o cliente toca numa foto (o cardápio abre sem ele). */
const OptionReels = lazy(() => import("./option-reels"));

type Option = OptionGroup["items"][number];

/** A foto pequena da opção: a capa (a primeira foto) ou a capa do vídeo. */
function thumbOf(option: Option) {
  for (const m of option.media) {
    const src = m.image?.thumb ?? m.video?.poster;
    if (src) return src;
  }
  return null;
}

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
  const [query, setQuery] = useState("");
  const [reels, setReels] = useState<{ groupId: string; optionId: string } | null>(null);
  // O Reels cobre a tela: vai direto no <dialog> do detalhe (o cartão de vidro em volta da escolha
  // prenderia o "fixed" dentro dele) ou, fora de um diálogo, no body.
  const [host, setHost] = useState<HTMLElement | null>(null);
  const linked = item.optionGroups.flatMap((link) => {
    const group = groups.get(link.groupId);
    return group ? [{ link, group }] : [];
  });
  const total = linked.reduce((sum, { group }) => sum + group.items.length, 0);
  const searching = total > SEARCH_FROM || linked.some(({ group }) => hasPhotos(group));
  const wanted = searchable(query.trim());
  const matches = (option: Option) => searchable(option.name).includes(wanted);
  const noMatch = wanted !== "" && !linked.some(({ group }) => group.items.some(matches));

  return (
    <div
      ref={(el) => {
        if (el) setHost(el.closest("dialog") ?? document.body);
      }}
      className="flex flex-col gap-5"
    >
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

      {searching && (
        <label className="relative flex items-center">
          <Search aria-hidden className="pointer-events-none absolute left-4 size-4 text-(--mesa-muted)" />
          <span className="sr-only">{t.search}</span>
          <input
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={t.search}
            autoComplete="off"
            className="h-12 w-full rounded-2xl border border-(--mesa-stroke) bg-(--mesa-glass) pr-4 pl-11 text-base text-(--mesa-fg) outline-none placeholder:text-(--mesa-muted) focus-visible:ring-3 focus-visible:ring-(--brand)"
          />
        </label>
      )}

      {linked.map(({ link, group }) => {
        const rules = rulesOf(group, link, variantId);
        const options = group.items.filter(
          (o) => !round || inRound(rules, optionPrice(item, o.id, o.priceCents, variantId)),
        );
        if (options.length === 0) return null;
        const count = options.reduce((sum, o) => sum + (picked[o.id] ?? 0), 0);
        const single = rules.max === 1 && rules.maxPerOption === 1;
        const headingId = `${id}${group.id}`;
        const rich = hasPhotos(group);
        const visible = wanted ? options.filter(matches) : options;
        const priceOf = (option: Option) => {
          const price = optionPrice(item, option.id, option.priceCents, variantId);
          return round || price === 0 ? null : price;
        };

        function set(optionId: string, quantity: number) {
          const next = { ...picked };
          if (quantity > 0) next[optionId] = quantity;
          else delete next[optionId];
          onPicked(next);
        }

        /** Tocar na opção (na lista ou no Reels): marca ou desmarca, pelas regras do grupo. */
        function toggle(option: Option) {
          if ((picked[option.id] ?? 0) > 0) {
            // Num grupo de uma escolha obrigatória, tocar na marcada não desmarca.
            if (!(single && rules.min > 0)) set(option.id, 0);
          } else if (single) {
            const next = { ...picked };
            options.forEach((o) => delete next[o.id]);
            onPicked({ ...next, [option.id]: 1 });
          } else {
            set(option.id, 1);
          }
        }
        const canPick = (option: Option) => option.available && (single || count < rules.max);

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
              {visible.map((option) => {
                const quantity = picked[option.id] ?? 0;
                const price = priceOf(option);
                const priceText =
                  price === null ? null : rules.pricing === "SUM" ? `+ ${formatCents(price)}` : formatCents(price);
                const full = count >= rules.max;
                const repeats = rules.maxPerOption > 1;
                const thumb = rich ? thumbOf(option) : null;
                const video = option.media.some((m) => m.video);
                return (
                  <li key={option.id} className={cn(rich && "flex items-center gap-3")}>
                    {rich &&
                      (thumb ? (
                        <button
                          type="button"
                          aria-label={t.seeMedia(option.name)}
                          onClick={() => setReels({ groupId: group.id, optionId: option.id })}
                          className={cn("relative size-18 shrink-0 overflow-hidden rounded-2xl", focus)}
                        >
                          {/* eslint-disable-next-line @next/next/no-img-element -- miniatura já no tamanho certo, vinda do R2 */}
                          <img
                            src={thumb}
                            alt=""
                            loading="lazy"
                            decoding="async"
                            className={cn("size-full object-cover", !option.available && "opacity-45")}
                          />
                          {video && (
                            <span className="absolute right-1 bottom-1 flex size-6 items-center justify-center rounded-full bg-black/55 text-white">
                              <Play aria-hidden className="size-3 fill-current" />
                            </span>
                          )}
                        </button>
                      ) : (
                        <span
                          aria-hidden
                          className="flex size-18 shrink-0 items-center justify-center rounded-2xl bg-(--mesa-glass) text-(--mesa-muted)"
                        >
                          <UtensilsCrossed className="size-6" />
                        </span>
                      ))}
                    {repeats ? (
                      <div className={cn(chip, "min-w-0 flex-1 justify-between", quantity > 0 ? on : off)}>
                        <span className={cn("flex min-w-0 flex-col", !option.available && "line-through")}>
                          <span>{option.name}</span>
                          {option.description && (
                            <span className="line-clamp-2 text-sm text-(--mesa-muted)">{option.description}</span>
                          )}
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
                        onClick={() => toggle(option)}
                        className={cn(chip, "w-full min-w-0 justify-between", focus, quantity > 0 ? on : off)}
                      >
                        <span className={cn("flex min-w-0 flex-col", !option.available && "line-through")}>
                          <span className={cn(rich && "font-semibold")}>{option.name}</span>
                          {option.description && (
                            <span className="line-clamp-2 text-sm text-(--mesa-muted)">{option.description}</span>
                          )}
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
            {reels?.groupId === group.id &&
              host &&
              createPortal(
                <Suspense fallback={null}>
                  <OptionReels
                    group={{ ...group, items: options }}
                    startId={reels.optionId}
                    priceOf={priceOf}
                    isPicked={(option) => (picked[option.id] ?? 0) > 0}
                    canPick={canPick}
                    onToggle={toggle}
                    onClose={() => setReels(null)}
                  />
                </Suspense>,
                host,
              )}
          </div>
        );
      })}
      {noMatch && <p className="text-sm text-(--mesa-muted)">{t.noMatch}</p>}
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
