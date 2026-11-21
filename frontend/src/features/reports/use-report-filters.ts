"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { periodFromParams, type DayRange, type PresetKey } from "./period";
import type { Grouping, ReportContext } from "./reports-api";

const GROUPINGS: Record<string, Grouping> = { dia: "DAY", semana: "WEEK", mes: "MONTH" };
const GROUPING_PARAM: Record<Grouping, string> = { DAY: "dia", WEEK: "semana", MONTH: "mes" };

/**
 * Os filtros dos relatórios ficam no endereço, para o dono voltar ou mandar o link: ?periodo=30d
 * (pronto) ou ?de=2026-11-01&ate=2026-11-15 (escolhido), ?setor=<id> e, em Vendas, ?agrupar=semana.
 */
export function useReportFilters(context: ReportContext) {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const limits = { today: context.today, earliestDay: context.earliestDay, maxDays: context.maxPeriodDays };
  const period = periodFromParams(new URLSearchParams(params.toString()), limits);
  const sectorId = params.get("setor") ?? undefined;
  const grouping = GROUPINGS[params.get("agrupar") ?? ""] ?? "DAY";

  function update(change: (next: URLSearchParams) => void) {
    const next = new URLSearchParams(params.toString());
    change(next);
    const query = next.toString();
    router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false });
  }

  return {
    limits,
    period,
    sectorId,
    grouping,
    query: { from: period.from, to: period.to, ...(sectorId ? { sectorId } : {}) },
    choosePreset: (key: PresetKey) =>
      update((next) => {
        next.delete("de");
        next.delete("ate");
        next.set("periodo", key);
      }),
    chooseRange: ({ from, to }: DayRange) =>
      update((next) => {
        next.delete("periodo");
        next.set("de", from);
        next.set("ate", to);
      }),
    chooseSector: (id: string | null) => update((next) => (id ? next.set("setor", id) : next.delete("setor"))),
    chooseGrouping: (value: Grouping) =>
      update((next) => (value === "DAY" ? next.delete("agrupar") : next.set("agrupar", GROUPING_PARAM[value]))),
  };
}
