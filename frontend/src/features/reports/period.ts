/**
 * Os períodos dos relatórios (ADR 0013). As datas são dias do restaurante em ISO ("2026-11-21"),
 * calculados a partir do "hoje" que o back manda (no fuso do restaurante, não do navegador).
 */
export type PeriodKey = "today" | "7d" | "30d" | "month";

export const PERIOD_KEYS: readonly PeriodKey[] = ["today", "7d", "30d", "month"];

export type DayRange = { from: string; to: string };

export type Period = DayRange & { key: PeriodKey };

export function parsePeriodKey(value: string | null | undefined): PeriodKey {
  return PERIOD_KEYS.includes(value as PeriodKey) ? (value as PeriodKey) : "7d";
}

function addDays(iso: string, days: number) {
  const date = new Date(`${iso}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

function length({ from, to }: DayRange) {
  return Math.round((Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)) / 86_400_000) + 1;
}

/** O período até hoje; não começa antes do primeiro dia que o plano deixa consultar. */
export function resolvePeriod(key: PeriodKey, today: string, earliestDay?: string | null): Period {
  const start = {
    today,
    "7d": addDays(today, -6),
    "30d": addDays(today, -29),
    month: `${today.slice(0, 8)}01`,
  }[key];
  const from = earliestDay && start < earliestDay ? earliestDay : start;
  return { key, from, to: today };
}

/**
 * A base da comparação: o período de mesmo tamanho logo antes. "Hoje" compara com o mesmo dia da
 * semana anterior (sexta com sexta), como manda o ADR 0013.
 */
export function previousOf(period: Period): DayRange {
  if (period.key === "today") {
    const lastWeek = addDays(period.from, -7);
    return { from: lastWeek, to: lastWeek };
  }
  const days = length(period);
  return { from: addDays(period.from, -days), to: addDays(period.from, -1) };
}

const UTC = { timeZone: "UTC" } as const;
const weekdayDayMonth = new Intl.DateTimeFormat("pt-BR", { ...UTC, weekday: "long", day: "numeric", month: "long" });
const dayMonth = new Intl.DateTimeFormat("pt-BR", { ...UTC, day: "numeric", month: "long" });
const dayMonthYear = new Intl.DateTimeFormat("pt-BR", { ...UTC, day: "numeric", month: "long", year: "numeric" });

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
