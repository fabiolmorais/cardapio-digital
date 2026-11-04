import { centsToInput, parseCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import type { DayOfWeek, Rodizio, RodizioRequest } from "./rodizio-api";

const t = messages.rodizio;

export const DAYS: DayOfWeek[] = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
export const MAX_TIERS = 6;
export const MAX_SPECIALS = 8;
export const MAX_RODIZIOS = 10;

export const NUMERIC_RULES = [
  "maxPerItemPerRound",
  "maxPendingPerGuest",
  "minRoundIntervalMinutes",
  "timeLimitMinutes",
] as const;
export type NumericRule = (typeof NUMERIC_RULES)[number];

/** O formulário guarda o que a pessoa digitou (textos); a conversão para a API é no envio. */
export type SpecialDraft = { key: string; days: DayOfWeek[]; startsAt: string; endsAt: string; price: string };
export type TierDraft = { key: string; name: string; price: string; specials: SpecialDraft[] };
export type Draft = {
  name: string;
  description: string;
  type: Rodizio["type"];
  tiers: TierDraft[];
  productIds: string[];
  /** null: a regra está desligada. */
  rules: Record<NumericRule, string | null>;
  wasteNotice: string | null;
};

let next = 0;
export const newKey = () => `k${++next}`;

export function newSpecial(): SpecialDraft {
  return { key: newKey(), days: ["FRIDAY", "SATURDAY"], startsAt: "18:00", endsAt: "23:59", price: "" };
}

export function newTier(name = ""): TierDraft {
  return { key: newKey(), name, price: "", specials: [] };
}

/** O rodízio do back no formulário; sem rodízio, um novo já com as faixas Adulto e Criança. */
export function toDraft(rodizio?: Rodizio): Draft {
  if (!rodizio) {
    return {
      name: "",
      description: "",
      type: "A_LA_MINUTE",
      tiers: t.defaultTiers.map((name) => newTier(name)),
      productIds: [],
      rules: { maxPerItemPerRound: null, maxPendingPerGuest: null, minRoundIntervalMinutes: null, timeLimitMinutes: null },
      wasteNotice: null,
    };
  }
  const rule = (value: number | undefined) => (value == null ? null : String(value));
  return {
    name: rodizio.name,
    description: rodizio.description,
    type: rodizio.type,
    tiers: rodizio.tiers.map((tier) => ({
      key: newKey(),
      name: tier.name,
      price: centsToInput(tier.defaultPriceCents),
      specials: tier.prices.map((p) => ({
        key: newKey(),
        days: DAYS.filter((d) => p.days.includes(d)),
        // O back responde "18:00:00"; o campo de horário usa "18:00".
        startsAt: p.startsAt.slice(0, 5),
        endsAt: p.endsAt.slice(0, 5),
        price: centsToInput(p.priceCents),
      })),
    })),
    productIds: rodizio.productIds,
    rules: {
      maxPerItemPerRound: rule(rodizio.rules.maxPerItemPerRound),
      maxPendingPerGuest: rule(rodizio.rules.maxPendingPerGuest),
      minRoundIntervalMinutes: rule(rodizio.rules.minRoundIntervalMinutes),
      timeLimitMinutes: rule(rodizio.rules.timeLimitMinutes),
    },
    wasteNotice: rodizio.rules.wasteNotice ?? null,
  };
}

/** Preço digitado é válido? (Vazio não é: o preço padrão e o especial são obrigatórios.) */
export function priceError(text: string): string | null {
  const cents = parseCents(text);
  return cents === null || Number.isNaN(cents) ? t.errors.invalidPrice : null;
}

export function ruleError(rule: NumericRule, text: string): string | null {
  const { min, max } = t.rules[rule];
  const value = Number(text);
  return /^\d+$/.test(text.trim()) && value >= min && value <= max ? null : t.rules.outOfRange(min, max);
}

export function specialError(special: SpecialDraft): string | null {
  if (special.days.length === 0) return t.errors.noDays;
  if (!special.startsAt || !special.endsAt || special.startsAt >= special.endsAt) return t.errors.timeOrder;
  return priceError(special.price);
}

/** O formulário tem algum erro que dá para apontar antes de enviar? */
export function hasErrors(draft: Draft): boolean {
  return (
    draft.name.trim() === "" ||
    draft.tiers.some((tier) => tier.name.trim() === "" || priceError(tier.price) || tier.specials.some(specialError)) ||
    NUMERIC_RULES.some((rule) => draft.rules[rule] !== null && ruleError(rule, draft.rules[rule]))
  );
}

/** O formulário (já sem erros) no formato da API. Regra desligada não vai. */
export function toRequest(draft: Draft): RodizioRequest {
  const rules: RodizioRequest["rules"] = {};
  for (const rule of NUMERIC_RULES) {
    const value = draft.rules[rule];
    if (value !== null) rules[rule] = Number(value);
  }
  if (draft.wasteNotice?.trim()) rules.wasteNotice = draft.wasteNotice.trim();
  return {
    name: draft.name.trim(),
    type: draft.type,
    description: draft.description.trim(),
    rules,
    tiers: draft.tiers.map((tier) => ({
      name: tier.name.trim(),
      defaultPriceCents: parseCents(tier.price)!,
      prices: tier.specials.map((s) => ({
        days: s.days,
        startsAt: s.startsAt,
        endsAt: s.endsAt,
        priceCents: parseCents(s.price)!,
      })),
    })),
    productIds: draft.productIds,
  };
}

/** "seg a sex", "sex e sáb", "todos os dias": o resumo dos dias de um preço especial. */
export function daysSummary(days: DayOfWeek[]): string {
  if (days.length === 7) return t.prices.allDays;
  const index = days.map((d) => DAYS.indexOf(d)).sort((a, b) => a - b);
  const contiguous = index.every((v, i) => i === 0 || v === index[i - 1] + 1);
  const short = (i: number) => t.days[DAYS[i]].short.toLowerCase();
  if (index.length >= 3 && contiguous) return `${short(index[0])} a ${short(index.at(-1)!)}`;
  if (index.length === 1) return short(index[0]);
  return `${index.slice(0, -1).map(short).join(", ")} e ${short(index.at(-1)!)}`;
}
