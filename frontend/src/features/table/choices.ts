import type { Schemas } from "@/lib/api/client";
import type { MenuItem } from "./table-data";

/**
 * As escolhas do prato na mesa (ADR 0010): o tamanho e as opções. Espelha a regra do back só para
 * MOSTRAR o preço enquanto o cliente escolhe; quem calcula e confere de verdade é o back, no envio.
 */
export type OptionGroup = Schemas["OptionGroup"];
export type GroupLink = MenuItem["optionGroups"][number];
export type GroupsById = Map<string, OptionGroup>;
/** As opções escolhidas: o id da opção e quantas vezes. */
export type Picked = Record<string, number>;
/** O tamanho e as escolhas copiados no pedido (as telas de "Meus pedidos" e do carrinho mostram). */
export type ItemChoices = Schemas["ItemChoices"];

export type Rules = {
  min: number;
  max: number;
  maxPerOption: number;
  splits: boolean;
  pricing: OptionGroup["pricing"];
  free: number;
};

export function groupsById(groups: OptionGroup[]): GroupsById {
  return new Map(groups.map((g) => [g.id, g]));
}

/** O produto pede escolha antes de ir para o pedido: tem tamanhos ou grupos. */
export function hasChoices(item: MenuItem) {
  return item.variants.length > 0 || item.optionGroups.length > 0;
}

/** As regras do grupo neste tamanho (com o mínimo e o máximo do tamanho, se houver). */
export function rulesOf(group: OptionGroup, link: GroupLink, variantId: string | null): Rules {
  const limit = variantId ? link.limits.find((l) => l.variantId === variantId) : undefined;
  const max = limit?.maxChoices ?? group.maxChoices;
  return {
    min: limit?.minChoices ?? group.minChoices,
    max,
    maxPerOption: Math.min(group.maxPerOption, max),
    splits: group.splitsItem,
    pricing: group.pricing,
    free: Math.min(group.freeQuantity, max),
  };
}

/** O preço da opção aqui: o do tamanho; sem ele, o deste produto; sem nenhum, o da opção (como o back). */
export function optionPrice(item: MenuItem, optionId: string, basePrice: number, variantId: string | null) {
  const inSize = variantId && item.optionPrices.find((p) => p.optionId === optionId && p.variantId === variantId);
  const inProduct = item.optionPrices.find((p) => p.optionId === optionId && !p.variantId);
  return (inSize || inProduct)?.priceCents ?? basePrice;
}

/** Na rodada do rodízio, os adicionais pagos (opções com preço em grupos que somam) não entram. */
export function inRound(rules: Rules, price: number) {
  return rules.pricing !== "SUM" || price === 0;
}

/** Quanto as escolhas de um grupo somam ao item, pela regra dele (soma com grátis, maior ou média). */
function groupCents(rules: Rules, units: number[]): number {
  if (units.length === 0) return 0;
  if (rules.pricing === "HIGHEST") return Math.max(...units);
  if (rules.pricing === "AVERAGE") return Math.ceil(units.reduce((a, b) => a + b, 0) / units.length);
  return [...units]
    .sort((a, b) => a - b)
    .slice(rules.free)
    .reduce((a, b) => a + b, 0);
}

export type Evaluation = {
  /** O preço de uma unidade, como o back vai cobrar. */
  unitCents: number;
  /** O primeiro que falta escolher (o título do grupo, ou null para o tamanho); undefined: completo. */
  missing?: string | null;
};

/**
 * O preço e o que falta, com estas escolhas. `round`: a rodada do rodízio (preço zero, sem os
 * adicionais pagos, e o tamanho é o do rodízio: não se escolhe).
 */
export function evaluate(
  item: MenuItem,
  groups: GroupsById,
  variantId: string | null,
  picked: Picked,
  round = false,
): Evaluation {
  const variant = item.variants.find((v) => v.id === variantId);
  let unitCents = variant ? (variant.promoPriceCents ?? variant.priceCents) : (item.promoPriceCents ?? item.priceCents);
  let missing: string | null | undefined = !round && item.variants.length > 0 && !variant ? null : undefined;
  for (const link of item.optionGroups) {
    const group = groups.get(link.groupId);
    if (!group) continue;
    const rules = rulesOf(group, link, variantId);
    const units = group.items.flatMap((option) =>
      Array<number>(picked[option.id] ?? 0).fill(optionPrice(item, option.id, option.priceCents, variantId)),
    );
    if (units.length < rules.min && missing === undefined) missing = group.title;
    unitCents += groupCents(rules, units);
  }
  return { unitCents: round ? 0 : unitCents, missing };
}

/** "½ Calabresa", "2× Bacon", "Catupiry": uma escolha como aparece embaixo do item. */
function partLabel(name: string, quantity: number, parts: number) {
  if (parts > 1) return `${["", "", "½", "⅓", "¼"][parts] ?? ""} ${name}`.trim();
  return quantity > 1 ? `${quantity}× ${name}` : name;
}

/** O tamanho e cada escolha, um por linha (a comanda impressa): ["Grande", "½ Calabresa", "½ Frango"]. */
export function choiceLines(choices: ItemChoices | undefined): string[] {
  if (!choices) return [];
  const byGroup = new Map<string, number>();
  for (const o of choices.options) if (o.splitsItem) byGroup.set(o.groupTitle, (byGroup.get(o.groupTitle) ?? 0) + 1);
  return [
    choices.variantName,
    ...choices.options.map((o) => partLabel(o.name, o.quantity, o.splitsItem ? (byGroup.get(o.groupTitle) ?? 1) : 1)),
  ].filter((line): line is string => Boolean(line));
}

/** O tamanho e as escolhas numa linha: "Grande · ½ Calabresa · ½ Frango · Catupiry". */
export function choicesText(choices: ItemChoices | undefined): string {
  return choiceLines(choices).join(" · ");
}

/** As escolhas desta tela no mesmo formato do pedido (para o carrinho mostrar igual a Meus pedidos). */
export function toChoices(item: MenuItem, groups: GroupsById, variantId: string | null, picked: Picked): ItemChoices {
  const variant = item.variants.find((v) => v.id === variantId);
  return {
    variantName: variant?.name,
    options: item.optionGroups.flatMap((link) => {
      const group = groups.get(link.groupId);
      if (!group) return [];
      return group.items
        .filter((o) => (picked[o.id] ?? 0) > 0)
        .map((o) => ({ groupTitle: group.title, name: o.name, quantity: picked[o.id], splitsItem: group.splitsItem }));
    }),
  };
}
