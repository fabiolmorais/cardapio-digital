"use client";

import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { ChartCard, ColumnChart, KpiTile, NoData } from "./charts";
import { formatDayMonth, formatMonth, formatRange, formatWeekday } from "./period";
import { useSalesReport, type Grouping, type SalesReport } from "./reports-api";
import { BlockError, Loading, ReportsScreen, type ReportFilters } from "./reports-page";

const t = messages.reports;

const decimal = new Intl.NumberFormat("pt-BR", { minimumFractionDigits: 1, maximumFractionDigits: 1 });

/** Vendas (desenho 14.2), com os blocos básicos (REL-01, 03, 05, 08). */
export function SalesBody({ filters }: { filters: ReportFilters }) {
  const report = useSalesReport({ ...filters.query, groupBy: filters.grouping });
  if (report.isError) return <BlockError onRetry={() => report.refetch()} />;
  if (!report.data) return <Loading inline />;
  return <Sales report={report.data} onGrouping={filters.chooseGrouping} />;
}

function Sales({ report, onGrouping }: { report: SalesReport; onGrouping: (g: Grouping) => void }) {
  const { current, compared } = report;
  const peak = report.byHour.find((h) => h.hour === report.peakHour);
  return (
    <>
      <section aria-label={t.sales} className="grid grid-cols-2 gap-3.5 lg:grid-cols-3">
        <KpiTile
          label={t.kpi.revenue}
          value={formatCents(current.revenueCents)}
          current={current.revenueCents}
          previous={compared.revenueCents}
        />
        <KpiTile label={t.kpi.tabs} value={String(current.tabs)} current={current.tabs} previous={compared.tabs} />
        <KpiTile
          label={t.kpi.ticket}
          value={formatCents(current.averageTicketCents)}
          current={current.averageTicketCents}
          previous={compared.averageTicketCents}
        />
        <KpiTile
          label={t.kpi.tabsPerDay}
          value={decimal.format(report.tabsPerDay)}
          current={report.tabsPerDay}
          previous={report.comparedTabsPerDay}
        />
        <KpiTile
          label={t.kpi.bestDay}
          value={report.bestDay ? `${formatWeekday(report.bestDay.day)} ${formatDayMonth(report.bestDay.day)}` : "—"}
          hint={report.bestDay ? formatCents(report.bestDay.revenueCents) : undefined}
        />
        <KpiTile
          label={t.kpi.peak}
          value={report.peakHour != null ? `${report.peakHour}h` : "—"}
          hint={peak ? t.kpi.peakHint(formatCents(peak.revenuePerDayCents)) : undefined}
        />
      </section>
      <Series report={report} onGrouping={onGrouping} />
      <ByHour report={report} />
    </>
  );
}

function pointLabel(start: string, end: string, grouping: Grouping, count: number) {
  if (grouping === "MONTH") return formatMonth(start);
  if (grouping === "WEEK") return formatDayMonth(start);
  return count <= 7 ? formatWeekday(start) : String(Number(start.slice(8)));
}

function Series({ report, onGrouping }: { report: SalesReport; onGrouping: (g: Grouping) => void }) {
  const title = t.series.title[report.grouping];
  const points = report.series;
  const best = points.reduce((a, b) => (b.revenueCents > a.revenueCents ? b : a), points[0]);
  const describe = (start: string, end: string) => formatRange({ from: start, to: end }, t.range);
  return (
    <ChartCard
      title={title}
      action={
        <div
          role="group"
          aria-label={t.series.groupBy}
          className="flex gap-1 rounded-[14px] border border-border bg-card p-1"
        >
          {(["DAY", "WEEK", "MONTH"] as const).map((g) => (
            <button
              key={g}
              type="button"
              aria-pressed={report.grouping === g}
              onClick={() => onGrouping(g)}
              className={cn(
                "h-9 rounded-[10px] px-3 text-sm font-semibold text-muted-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                report.grouping === g && "bg-foreground text-background",
              )}
            >
              {t.series.grouping[g]}
            </button>
          ))}
        </div>
      }
    >
      {points.some((p) => p.revenueCents > 0) ? (
        <ColumnChart
          label={t.series.chart(title, `${describe(best.start, best.end)}, ${formatCents(best.revenueCents)}`)}
          columns={points.map((p) => ({
            key: p.start,
            label: pointLabel(p.start, p.end, report.grouping, points.length),
            value: p.revenueCents,
            tip: t.series.tip(describe(p.start, p.end), formatCents(p.revenueCents), p.tabs),
          }))}
        />
      ) : (
        <NoData height={220} />
      )}
    </ChartCard>
  );
}

function ByHour({ report }: { report: SalesReport }) {
  const hours = report.byHour;
  if (hours.length === 0) {
    return (
      <ChartCard title={t.byHour.title} hint={t.byHour.hint}>
        <NoData height={180} />
      </ChartCard>
    );
  }
  const first = hours[0].hour;
  const last = hours[hours.length - 1].hour;
  const all = Array.from({ length: last - first + 1 }, (_, i) => {
    const hour = first + i;
    return hours.find((h) => h.hour === hour) ?? { hour, revenuePerDayCents: 0, tabsPerDay: 0 };
  });
  const peak = all.reduce((a, b) => (b.revenuePerDayCents > a.revenuePerDayCents ? b : a), all[0]);
  return (
    <ChartCard title={t.byHour.title} hint={t.byHour.hint}>
      <ColumnChart
        height={180}
        label={t.byHour.chart(peak.hour, formatCents(peak.revenuePerDayCents))}
        columns={all.map((h) => ({
          key: String(h.hour),
          label: `${h.hour}h`,
          value: h.revenuePerDayCents,
          tip: t.byHour.tip(h.hour, formatCents(h.revenuePerDayCents), decimal.format(h.tabsPerDay)),
        }))}
      />
    </ChartCard>
  );
}

/** A tela de Vendas, dentro da moldura dos relatórios. */
export function SalesScreen() {
  return <ReportsScreen title={t.sales}>{(_, filters) => <SalesBody filters={filters} />}</ReportsScreen>;
}
