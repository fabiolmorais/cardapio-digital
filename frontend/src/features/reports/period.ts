/**
 * Os períodos dos relatórios (ADR 0013). As datas são dias do restaurante em ISO ("2026-11-21"),
 * calculados a partir do "hoje" que o back manda (no fuso do restaurante, não do navegador).
 */
export type PresetKey = "today" | "7d" | "30d" | "month";
export type PeriodKey = PresetKey | "custom";

export const PERIOD_KEYS: readonly PresetKey[] = ["today", "7d", "30d", "month"];

export type DayRange = { from: string; to: string };

export type Period = DayRange & { key: PeriodKey };

export function parsePeriodKey(value: string | null | undefined): PresetKey {
  return PERIOD_KEYS.includes(value as PresetKey) ? (value as PresetKey) : "7d";
}

export function isIsoDate(value: string | null | undefined): value is string {
  return !!value && /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(Date.parse(`${value}T00:00:00Z`));
}

export function addDays(iso: string, days: number) {
  const date = new Date(`${iso}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

export function lengthOf({ from, to }: DayRange) {
  return Math.round((Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)) / 86_400_000) + 1;
}

/** Os limites de quem pede: até hoje, não antes do histórico do plano e no máximo `maxDays` de uma vez. */
export type Limits = { today: string; earliestDay?: string | null; maxDays: number };

/** Encaixa um intervalo nos limites (o fim primeiro: até hoje; depois o começo). */
export function clampRange({ from, to }: DayRange, { today, earliestDay, maxDays }: Limits): DayRange {
  const end = to > today ? today : to;
  let start = from > end ? end : from;
  if (earliestDay && start < earliestDay) start = earliestDay;
  if (lengthOf({ from: start, to: end }) > maxDays) start = addDays(end, -(maxDays - 1));
  return { from: start, to: end };
}

/** O período pronto até hoje; não começa antes do primeiro dia que o plano deixa consultar. */
export function resolvePeriod(key: PresetKey, today: string, earliestDay?: string | null): Period {
  const start = {
    today,
    "7d": addDays(today, -6),
    "30d": addDays(today, -29),
    month: `${today.slice(0, 8)}01`,
  }[key];
  const from = earliestDay && start < earliestDay ? earliestDay : start;
  return { key, from, to: today };
}

/** O período do endereço: ?de=&ate= (escolhido) ou ?periodo= (pronto; 7 dias se nada vier). */
export function periodFromParams(params: URLSearchParams, limits: Limits): Period {
  const from = params.get("de");
  const to = params.get("ate");
  if (isIsoDate(from) && isIsoDate(to) && from <= to) return { key: "custom", ...clampRange({ from, to }, limits) };
  return resolvePeriod(parsePeriodKey(params.get("periodo")), limits.today, limits.earliestDay);
}

/**
 * A base da comparação, a mesma regra do back (ADR 0013): um dia só compara com o mesmo dia da
 * semana anterior (sexta com sexta); os outros períodos, com o anterior de mesmo tamanho.
 */
export function previousOf(period: DayRange): DayRange {
  const days = lengthOf(period);
  if (days === 1) {
    const lastWeek = addDays(period.from, -7);
    return { from: lastWeek, to: lastWeek };
  }
  return { from: addDays(period.from, -days), to: addDays(period.from, -1) };
}

/** Os atalhos do "Escolher datas" (desenho 14.14). A semana começa na segunda. */
export function shortcuts(
  today: string,
): { key: "yesterday" | "lastWeek" | "lastMonth" | "last90"; range: DayRange }[] {
  const weekday = (new Date(`${today}T00:00:00Z`).getUTCDay() + 6) % 7; // segunda = 0
  const thisMonday = addDays(today, -weekday);
  const firstOfMonth = `${today.slice(0, 8)}01`;
  const lastMonthEnd = addDays(firstOfMonth, -1);
  return [
    { key: "yesterday", range: { from: addDays(today, -1), to: addDays(today, -1) } },
    { key: "lastWeek", range: { from: addDays(thisMonday, -7), to: addDays(thisMonday, -1) } },
    { key: "lastMonth", range: { from: `${lastMonthEnd.slice(0, 8)}01`, to: lastMonthEnd } },
    { key: "last90", range: { from: addDays(today, -89), to: today } },
  ];
}

const UTC = { timeZone: "UTC" } as const;
const weekdayDayMonth = new Intl.DateTimeFormat("pt-BR", { ...UTC, weekday: "long", day: "numeric", month: "long" });
const dayMonth = new Intl.DateTimeFormat("pt-BR", { ...UTC, day: "numeric", month: "long" });
const dayMonthYear = new Intl.DateTimeFormat("pt-BR", { ...UTC, day: "numeric", month: "long", year: "numeric" });
const shortWeekday = new Intl.DateTimeFormat("pt-BR", { ...UTC, weekday: "short" });
const shortMonth = new Intl.DateTimeFormat("pt-BR", { ...UTC, month: "short" });

const at = (iso: string) => new Date(`${iso}T00:00:00Z`);

/** "sexta-feira, 21 de novembro"; "13 a 19 de novembro"; "28 de outubro a 3 de novembro". */
export function formatRange({ from, to }: DayRange, join: (a: string, b: string) => string) {
  if (from === to) return weekdayDayMonth.format(at(from));
  if (from.slice(0, 4) !== to.slice(0, 4)) return join(dayMonthYear.format(at(from)), dayMonthYear.format(at(to)));
  if (from.slice(0, 7) === to.slice(0, 7)) return join(String(Number(from.slice(8))), dayMonth.format(at(to)));
  return join(dayMonth.format(at(from)), dayMonth.format(at(to)));
}

/** "2026-11-18" → "18/11/2026". */
export function formatDay(iso: string) {
  return `${iso.slice(8)}/${iso.slice(5, 7)}/${iso.slice(0, 4)}`;
}

/** "2026-11-18" → "18/11". */
export function formatDayMonth(iso: string) {
  return `${iso.slice(8)}/${iso.slice(5, 7)}`;
}

/** "2026-11-14" → "sáb" (sem o ponto). */
export function formatWeekday(iso: string) {
  return shortWeekday.format(at(iso)).replace(".", "");
}

/** "2026-11-01" → "nov" (sem o ponto). */
export function formatMonth(iso: string) {
  return shortMonth.format(at(iso)).replace(".", "");
}
