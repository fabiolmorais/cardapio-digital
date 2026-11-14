import { centsToInput, parseCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import type { ProductOptions, ProductOptionsRequest } from "./options-api";

const t = messages.options.product;

/**
 * O rascunho dos tamanhos e dos grupos de um produto, como a tela edita: textos nos campos de preço
 * e uma chave por tamanho (o tamanho novo ainda não tem id). Vira o corpo do PUT só ao salvar.
 */
export type VariantDraft = {
  key: string;
  id?: string;
  name: string;
  price: string;
  promo: string;
  available: boolean;
  rodizioDefault: boolean;
};

/** Um limite em branco ("") vale o do grupo. */
export type LimitDraft = { min: string; max: string };

export type GroupDraft = { groupId: string; limits: Record<string, LimitDraft> };

export type OptionsDraft = {
  variants: VariantDraft[];
  groups: GroupDraft[];
  /** O preço de uma opção num tamanho: chave `${optionId}|${variantKey}`; em branco, o da opção. */
  prices: Record<string, string>;
};

let keys = 0;
export const newVariantKey = () => `variant-${++keys}`;

export const EMPTY_DRAFT: OptionsDraft = { variants: [], groups: [], prices: {} };

export const priceKey = (optionId: string, variantKey: string) => `${optionId}|${variantKey}`;

export function isEmptyDraft(draft: OptionsDraft) {
  return draft.variants.length === 0 && draft.groups.length === 0;
}

export function fromOptions(options: ProductOptions): OptionsDraft {
  const variants = options.variants.map((v) => ({
    key: newVariantKey(),
    id: v.id,
    name: v.name,
    price: centsToInput(v.priceCents),
    promo: v.promoPriceCents != null ? centsToInput(v.promoPriceCents) : "",
    available: v.available,
    rodizioDefault: v.rodizioDefault,
  }));
  const keyOf = new Map(variants.map((v) => [v.id, v.key]));
  const groups = options.groups.map((g) => ({
    groupId: g.group.id,
    limits: Object.fromEntries(
      g.limits
        .filter((l) => keyOf.has(l.variantId))
        .map((l) => [keyOf.get(l.variantId)!, { min: String(l.minChoices), max: String(l.maxChoices) }]),
    ),
  }));
  const prices = Object.fromEntries(
    options.prices
      .filter((p) => keyOf.has(p.variantId))
      .map((p) => [priceKey(p.optionId, keyOf.get(p.variantId)!), centsToInput(p.priceCents)]),
  );
  return { variants, groups, prices };
}

const valid = (cents: number | null): cents is number => cents !== null && !Number.isNaN(cents);

/** O menor preço dos tamanhos (com a promoção): fica como o preço do produto, que deixa de valer. */
export function cheapestVariant(draft: OptionsDraft): number | null {
  const prices = draft.variants
    .map((v) => {
      const promo = parseCents(v.promo);
      return valid(promo) ? promo : parseCents(v.price);
    })
    .filter(valid);
  return prices.length ? Math.min(...prices) : null;
}

/**
 * O corpo do PUT /products/{id}/options. Os limites e os preços apontam para o tamanho pela posição
 * na lista. Lança um erro com a frase para a tela se algum campo não fecha.
 *
 * @param optionIds as opções dos grupos ligados: preço de opção de um grupo que saiu não vai
 */
export function toRequest(draft: OptionsDraft, optionIds: Set<string>): ProductOptionsRequest {
  const variants = draft.variants.map((v) => {
    const price = parseCents(v.price);
    const promo = parseCents(v.promo);
    if (!v.name.trim() || !valid(price) || Number.isNaN(promo) || (valid(promo) && promo >= price)) {
      throw new Error(t.invalid);
    }
    return {
      ...(v.id ? { id: v.id } : {}),
      name: v.name.trim(),
      priceCents: price,
      ...(valid(promo) ? { promoPriceCents: promo } : {}),
      available: v.available,
      rodizioDefault: v.rodizioDefault,
    };
  });
  const index = new Map(draft.variants.map((v, i) => [v.key, i]));
  const groups = draft.groups.map((g) => ({
    groupId: g.groupId,
    limits: Object.entries(g.limits)
      .filter(([key, l]) => index.has(key) && l.max !== "")
      .map(([key, l]) => {
        const min = l.min === "" ? 0 : Number(l.min);
        const max = Number(l.max);
        if (!Number.isInteger(min) || !Number.isInteger(max) || min < 0 || max < 1 || min > max) {
          throw new Error(t.invalidLimits);
        }
        return { variant: index.get(key)!, minChoices: min, maxChoices: max };
      }),
  }));
  const prices = Object.entries(draft.prices)
    .filter(([, text]) => text.trim() !== "")
    .map(([key, text]) => {
      const [optionId, variantKey] = key.split("|");
      const cents = parseCents(text);
      if (!valid(cents)) throw new Error(t.invalid);
      return { optionId, variantKey, cents };
    })
    .filter((p) => index.has(p.variantKey) && optionIds.has(p.optionId))
    .map((p) => ({ optionId: p.optionId, variant: index.get(p.variantKey)!, priceCents: p.cents }));
  return { variants, groups, prices };
}
