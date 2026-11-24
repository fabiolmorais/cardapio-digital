"use client";

import { messages } from "@/messages/pt-BR";
import { ChartCard, KpiTile, NoData } from "./charts";
import { formatDuration, timeVariation } from "./durations";
import { ByHourCard, CategoryPrepCard, DelayedCard, KitchenFullTiles, PeaksCard } from "./kitchen-full";
import { LockedCard } from "./locked";
import { CategoryFilter } from "./menu-report";
import { useKitchenReport, type KitchenReport } from "./reports-api";
import { BlockError, Loading, ReportsScreen, type ReportFilters } from "./reports-page";

const t = messages.reports;
const k = t.kitchen;

/** A tela da cozinha (desenho 14.4), com os blocos básicos (REL-30), e o filtro de categoria. */
export function KitchenScreen() {
  return (
    <ReportsScreen
      exportAs="kitchen"
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
  const full = data.full;
  const locked = k.full.locked;
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
        {full ? (
          <KitchenFullTiles full={full} />
        ) : (
          <>
            <KpiTile label={k.preparations} value={String(data.preparations)} hint={k.preparationsHint} />
            <div className="col-span-2">
              <LockedCard title={locked.moreNumbers} text={locked.moreNumbersText} small />
            </div>
          </>
        )}
      </section>
      {full ? <ByHourCard full={full} /> : <LockedCard title={k.full.byHour} text={locked.byHour} />}
      <div className="grid items-start gap-5 lg:grid-cols-2">
        <Slowest report={data} />
        {full ? <PeaksCard full={full} /> : <LockedCard title={k.full.peaks} text={locked.peaks} />}
      </div>
      <div className="grid items-start gap-5 lg:grid-cols-2">
        {full ? <CategoryPrepCard full={full} /> : <LockedCard title={k.full.byCategory} text={locked.byCategory} />}
        {full ? <DelayedCard full={full} /> : <LockedCard title={k.full.delayed} text={locked.delayed} />}
      </div>
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
