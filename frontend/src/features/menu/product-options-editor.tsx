"use client";

import { ArrowDown, ArrowUp, ListChecks, Plus, X } from "lucide-react";
import Link from "next/link";
import { useId, useState } from "react";
import { Button, buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { centsToInput, parseCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { rulesSummary } from "./option-groups-manager";
import type { OptionGroup } from "./options-api";
import {
  isEmptyDraft,
  newVariantKey,
  priceKey,
  type GroupDraft,
  type OptionsDraft,
  type VariantDraft,
} from "./product-options-draft";

const t = messages.options.product;

/**
 * Os tamanhos e os grupos de opções do produto (ADR 0010), recolhidos por padrão: quem não usa não
 * vê nada diferente. O rascunho é salvo junto com o produto, no mesmo "Salvar".
 */
export function ProductOptionsEditor({
  draft,
  onChange,
  library,
}: {
  draft: OptionsDraft;
  onChange: (draft: OptionsDraft) => void;
  library: OptionGroup[];
}) {
  const id = useId();
  const [open, setOpen] = useState(!isEmptyDraft(draft));
  const [picking, setPicking] = useState("");
  const byId = new Map(library.map((g) => [g.id, g]));
  const linked = draft.groups.filter((g) => byId.has(g.groupId));
  const available = library.filter((g) => !draft.groups.some((l) => l.groupId === g.id));

  function setVariant(index: number, patch: Partial<VariantDraft>) {
    onChange({ ...draft, variants: draft.variants.map((v, i) => (i === index ? { ...v, ...patch } : v)) });
  }

  function setGroup(groupId: string, patch: Partial<GroupDraft>) {
    onChange({ ...draft, groups: draft.groups.map((g) => (g.groupId === groupId ? { ...g, ...patch } : g)) });
  }

  function moveGroup(index: number, by: number) {
    const groups = [...draft.groups];
    const [group] = groups.splice(index, 1);
    groups.splice(index + by, 0, group);
    onChange({ ...draft, groups });
  }

  if (!open) {
    return (
      <section className="flex flex-col items-start gap-3 rounded-2xl border border-dashed border-border p-5">
        <h2 className="font-semibold">{t.title}</h2>
        <p className="text-sm text-muted-foreground">{t.intro}</p>
        <Button type="button" variant="outline" className="h-11" onClick={() => setOpen(true)}>
          <ListChecks aria-hidden />
          {t.open}
        </Button>
      </section>
    );
  }

  return (
    <section
      aria-labelledby={`${id}title`}
      className="flex flex-col gap-6 rounded-2xl border border-border bg-card p-5 sm:p-6"
    >
      <div className="flex flex-col gap-1">
        <h2 id={`${id}title`} className="text-lg font-semibold">
          {t.title}
        </h2>
        <p className="text-sm text-muted-foreground">{t.intro}</p>
      </div>

      {/* Tamanhos */}
      <fieldset className="flex flex-col gap-3">
        <legend className="font-semibold">{t.variants}</legend>
        <p className="text-sm text-muted-foreground">{t.variantsHint}</p>
        {draft.variants.length > 0 && (
          <ol className="flex flex-col gap-2">
            {draft.variants.map((variant, index) => (
              <li key={variant.key} className="flex flex-wrap items-center gap-2 rounded-xl border border-border p-2">
                <Input
                  aria-label={t.variantName(index + 1)}
                  required
                  maxLength={30}
                  autoComplete="off"
                  value={variant.name}
                  placeholder={t.variantNamePlaceholder}
                  onChange={(e) => setVariant(index, { name: e.target.value })}
                  className="h-11 min-w-32 flex-1 rounded-xl px-3 text-base"
                />
                <Money
                  label={t.variantPrice(index + 1)}
                  value={variant.price}
                  onChange={(price) => setVariant(index, { price })}
                />
                <Money
                  label={t.variantPromo(index + 1)}
                  placeholder={t.promoPlaceholder}
                  value={variant.promo}
                  onChange={(promo) => setVariant(index, { promo })}
                />
                <label className="flex min-h-11 items-center gap-2 px-1 text-sm">
                  <input
                    type="checkbox"
                    className="size-5 accent-(--success)"
                    checked={variant.available}
                    aria-label={t.variantAvailable(variant.name)}
                    onChange={(e) => setVariant(index, { available: e.target.checked })}
                  />
                  <span aria-hidden>{messages.menu.columns.available}</span>
                </label>
                <label className="flex min-h-11 items-center gap-2 px-1 text-sm">
                  <input
                    type="radio"
                    name={`${id}rodizio`}
                    className="size-5 accent-(--primary)"
                    checked={variant.rodizioDefault}
                    aria-label={t.rodizioLabel(variant.name)}
                    onChange={() =>
                      onChange({
                        ...draft,
                        variants: draft.variants.map((v, i) => ({ ...v, rodizioDefault: i === index })),
                      })
                    }
                  />
                  <span aria-hidden>{t.rodizio}</span>
                </label>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-lg"
                  className="size-11 shrink-0"
                  aria-label={t.removeVariant(variant.name || String(index + 1))}
                  onClick={() => {
                    const prices = Object.fromEntries(
                      Object.entries(draft.prices).filter(([key]) => !key.endsWith(`|${variant.key}`)),
                    );
                    onChange({ ...draft, variants: draft.variants.filter((_, i) => i !== index), prices });
                  }}
                >
                  <X aria-hidden />
                </Button>
              </li>
            ))}
          </ol>
        )}
        <Button
          type="button"
          variant="outline"
          className="h-11 self-start"
          disabled={draft.variants.length >= 10}
          onClick={() =>
            onChange({
              ...draft,
              variants: [
                ...draft.variants,
                {
                  key: newVariantKey(),
                  name: "",
                  price: "",
                  promo: "",
                  available: true,
                  rodizioDefault: draft.variants.length === 0,
                },
              ],
            })
          }
        >
          <Plus aria-hidden />
          {t.addVariant}
        </Button>
      </fieldset>

      {/* Grupos */}
      <fieldset className="flex flex-col gap-3">
        <legend className="font-semibold">{t.groups}</legend>
        <p className="text-sm text-muted-foreground">{t.groupsHint}</p>
        {linked.map((link, index) => {
          const group = byId.get(link.groupId)!;
          return (
            <div key={link.groupId} className="flex flex-col gap-3 rounded-xl border border-border p-3">
              <div className="flex items-start gap-2">
                <div className="mr-auto flex min-w-0 flex-col">
                  <span className="font-semibold break-words">{group.title}</span>
                  <span className="text-[13px] text-muted-foreground">{rulesSummary(group)}</span>
                </div>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-lg"
                  className="size-11 shrink-0"
                  aria-label={t.moveUp(group.title)}
                  disabled={index === 0}
                  onClick={() => moveGroup(index, -1)}
                >
                  <ArrowUp aria-hidden />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-lg"
                  className="size-11 shrink-0"
                  aria-label={t.moveDown(group.title)}
                  disabled={index === linked.length - 1}
                  onClick={() => moveGroup(index, 1)}
                >
                  <ArrowDown aria-hidden />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-lg"
                  className="size-11 shrink-0"
                  aria-label={t.removeGroup(group.title)}
                  onClick={() => onChange({ ...draft, groups: draft.groups.filter((g) => g.groupId !== link.groupId) })}
                >
                  <X aria-hidden />
                </Button>
              </div>

              {draft.variants.length > 0 && (
                <div className="flex flex-col gap-2">
                  <span className="text-sm font-semibold">
                    {t.limits} <span className="font-normal text-muted-foreground">· {t.limitsHint}</span>
                  </span>
                  <div className="flex flex-wrap gap-3">
                    {draft.variants.map((variant) => {
                      const limit = link.limits[variant.key] ?? { min: "", max: "" };
                      const setLimit = (patch: Partial<typeof limit>) =>
                        setGroup(link.groupId, { limits: { ...link.limits, [variant.key]: { ...limit, ...patch } } });
                      const name = variant.name || "?";
                      return (
                        <div key={variant.key} className="flex items-center gap-1.5 rounded-lg bg-muted px-2 py-1">
                          <span className="text-sm">{name}</span>
                          <Input
                            aria-label={t.limitMin(name)}
                            inputMode="numeric"
                            value={limit.min}
                            placeholder={String(group.minChoices)}
                            onChange={(e) => setLimit({ min: e.target.value.replace(/\D/g, "") })}
                            className="h-10 w-12 rounded-lg px-2 text-center font-mono"
                          />
                          <span aria-hidden className="text-muted-foreground">
                            –
                          </span>
                          <Input
                            aria-label={t.limitMax(name)}
                            inputMode="numeric"
                            value={limit.max}
                            placeholder={String(group.maxChoices)}
                            onChange={(e) => setLimit({ max: e.target.value.replace(/\D/g, "") })}
                            className="h-10 w-12 rounded-lg px-2 text-center font-mono"
                          />
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {draft.variants.length > 0 && group.items.some((i) => i.priceCents > 0) && (
                <div className="flex flex-col gap-2">
                  <span className="text-sm font-semibold">
                    {t.prices} <span className="font-normal text-muted-foreground">· {t.pricesHint}</span>
                  </span>
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr>
                          <th scope="col" className="sr-only">
                            {messages.options.form.items}
                          </th>
                          {draft.variants.map((v) => (
                            <th
                              key={v.key}
                              scope="col"
                              className="px-1 pb-1 text-left font-medium text-muted-foreground"
                            >
                              {v.name || "?"}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {group.items.map((item) => (
                          <tr key={item.id}>
                            <th scope="row" className="pr-2 text-left font-normal">
                              {item.name}
                            </th>
                            {draft.variants.map((v) => {
                              const key = priceKey(item.id, v.key);
                              return (
                                <td key={v.key} className="p-1">
                                  <Money
                                    compact
                                    label={t.priceFor(item.name, v.name || "?")}
                                    placeholder={centsToInput(item.priceCents)}
                                    value={draft.prices[key] ?? ""}
                                    onChange={(price) =>
                                      onChange({ ...draft, prices: { ...draft.prices, [key]: price } })
                                    }
                                  />
                                </td>
                              );
                            })}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          );
        })}

        {library.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t.noLibrary}</p>
        ) : (
          available.length > 0 && (
            <div className="flex flex-wrap items-end gap-2">
              <div className="flex min-w-56 flex-1 flex-col gap-2">
                <Label htmlFor={`${id}pick`} className="text-muted-foreground">
                  {t.pickGroup}
                </Label>
                <Select
                  id={`${id}pick`}
                  value={picking}
                  onValueChange={setPicking}
                  options={available.map((g) => ({
                    value: g.id,
                    label: g.name === g.title ? g.name : `${g.name} · ${g.title}`,
                  }))}
                />
              </div>
              <Button
                type="button"
                variant="outline"
                className="h-11"
                disabled={!picking || draft.groups.length >= 10}
                onClick={() => {
                  onChange({ ...draft, groups: [...draft.groups, { groupId: picking, limits: {} }] });
                  setPicking("");
                }}
              >
                <Plus aria-hidden />
                {t.addGroup}
              </Button>
            </div>
          )
        )}
        <Link
          href="/admin/cardapio/opcoes"
          className={cn(buttonVariants({ variant: "ghost" }), "h-11 self-start px-3 text-muted-foreground")}
        >
          <ListChecks aria-hidden />
          {t.manage}
        </Link>
      </fieldset>
    </section>
  );
}

/** Um campo de preço curto, que se arruma ao sair ("27,9" vira "27,90"). */
function Money({
  label,
  value,
  onChange,
  placeholder = "0,00",
  compact,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  compact?: boolean;
}) {
  const invalid = Number.isNaN(parseCents(value));
  return (
    <div className={cn("relative shrink-0", compact ? "w-24" : "w-32")}>
      <span
        aria-hidden
        className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-xs font-semibold text-muted-foreground"
      >
        R$
      </span>
      <Input
        aria-label={label}
        inputMode="decimal"
        autoComplete="off"
        value={value}
        placeholder={placeholder}
        aria-invalid={invalid || undefined}
        onChange={(e) => onChange(e.target.value)}
        onBlur={() => {
          const cents = parseCents(value);
          if (cents !== null && !Number.isNaN(cents)) onChange(centsToInput(cents));
        }}
        className={cn("rounded-xl pr-2 pl-9 font-mono text-base", compact ? "h-10" : "h-11")}
      />
    </div>
  );
}
