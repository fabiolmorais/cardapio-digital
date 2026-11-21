"use client";

import { messages } from "@/messages/pt-BR";
import { ChartCard, KpiTile, NoData, type Delta } from "./charts";
import { CategoryFilter } from "./menu-report";
import { useKitchenReport, type KitchenReport } from "./reports-api";
import { BlockError, Loading, ReportsScreen, type ReportFilters } from "./reports-page";

const t = messages.reports;
const k = t.kitchen;

/**
 * Um tempo como a cozinha fala: "45 s", "3 min 40 s", "12 min" (de 10 min para cima, sem os segundos)
 * e "1 h 05 min".
 */
export function formatDuration(totalSeconds: number) {
  const seconds = Math.max(0, Math.round(totalSeconds));
  if (seconds < 60) return `${seconds} s`;
  if (seconds < 600) {
    const rest = seconds % 60;
    return rest === 0 ? `${seconds / 60} min` : `${Math.floor(seconds / 60)} min ${rest} s`;
  }
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes} min`;
  return `${Math.floor(minutes / 60)} h ${String(minutes % 60).padStart(2, "0")} min`;
}

/** A variação de um tempo: menos é melhor ("▼ 1 min, mais rápido", em verde). */
export function timeVariation(current: number, previous: number): Delta {
  const change = formatDuration(Math.abs(current - previous));
  if (Math.abs(current - previous) < 1) return { text: t.delta.same, tone: "flat" };
  return current < previous ? { text: k.faster(change), tone: "up" } : { text: k.slower(change), tone: "down" };
}

/** A tela da cozinha (desenho 14.4), com os blocos básicos (REL-30), e o filtro de categoria. */
export function KitchenScreen() {
  return (
    <ReportsScreen
      title={k.title}
      filter={(filters) => <CategoryFilter value={filters.categoryId} onChange={filters.chooseCategory} />}
    >
      {(_, filters) => <KitchenBody filters={filters} />}
    </ReportsScreen>
  );
}

function KitchenBody({ filters }: { filters: ReportFilters }) {
  const params = { ...filters.query, ...(filters.categoryId ? { categoryId: filters.categoryId } : {}) };
  const report = useKitchenReport(params);
  if (report.isError) return <BlockError onRetry={() => report.refetch()} />;
  if (!report.data) return <Loading inline />;
  const data = report.data;
  return (
    <>
      <section aria-label={k.title} className="grid grid-cols-2 gap-3.5 lg:grid-cols-4">
        <KpiTile
          label={k.averagePrep}
          value={data.averagePrepSeconds == null ? "—" : formatDuration(data.averagePrepSeconds)}
          delta={
            data.averagePrepSeconds != null && data.comparedAveragePrepSeconds != null
              ? timeVariation(data.averagePrepSeconds, data.comparedAveragePrepSeconds)
              : null
          }
          hint={data.averagePrepSeconds == null ? k.noPreparations : t.delta.none}
        />
        <KpiTile label={k.preparations} value={String(data.preparations)} hint={k.preparationsHint} />
      </section>
      <Slowest report={data} />
    </>
  );
}

function Slowest({ report }: { report: KitchenReport }) {
  const weekdays = t.heatmap.weekdays;
  return (
    <ChartCard title={k.slowest} hint={k.slowestHint} note={report.slowest.length > 0 ? k.note : undefined}>
      {report.slowest.length === 0 ? (
        <NoData text={report.preparations === 0 ? k.noPreparations : k.noSlowest} />
      ) : (
        <div className="relative -mx-5 overflow-x-auto px-5">
          <table className="w-full min-w-[520px] border-collapse text-sm">
            <thead>
              <tr className="text-left text-xs font-semibold tracking-[.4px] text-muted-foreground uppercase">
                <th scope="col" className="border-b border-border py-3 pr-4 font-semibold">
                  {k.columns.item}
                </th>
                <th scope="col" className="border-b border-border px-4 py-3 text-right font-semibold">
                  {k.columns.average}
                </th>
                <th scope="col" className="border-b border-border px-4 py-3 text-right font-semibold">
                  {k.columns.ninetyPercent}
                </th>
                <th scope="col" className="border-b border-border py-3 pl-4 font-semibold">
                  {k.columns.worstHour}
                </th>
              </tr>
            </thead>
            <tbody>
              {report.slowest.map((item) => (
                <tr key={item.productId} className="border-b border-border last:border-b-0">
                  <td className="py-3 pr-4">
                    <span className="flex flex-col">
                      <span>{item.name}</span>
                      <span className="text-xs text-muted-foreground">{item.category}</span>
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right font-mono whitespace-nowrap tabular-nums">
                    {formatDuration(item.averageSeconds)}
                  </td>
                  <td className="px-4 py-3 text-right font-mono whitespace-nowrap tabular-nums">
                    {formatDuration(item.ninetyPercentSeconds)}
                  </td>
                  <td className="py-3 pl-4 whitespace-nowrap text-muted-foreground">
                    {item.worstWeekday != null && item.worstHour != null ? (
                      k.worstHour(weekdays[item.worstWeekday - 1], item.worstHour)
                    ) : (
                      <>
                        <span aria-hidden>{k.noWorstHour}</span>
                        <span className="sr-only">{k.noWorstHourLabel}</span>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </ChartCard>
  );
}
