"use client";

import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { BarList, ChartCard, ColumnChart, Heatmap, KpiTile, NoData, SplitBar } from "./charts";
import { formatDayMonth, formatWeekday } from "./period";
import { useOverviewReport, type OverviewReport } from "./reports-api";
import { BlockError, Loading, ReportsScreen, type ReportFilters } from "./reports-page";

const t = messages.reports;

const METHOD_COLOR = { PIX: "var(--report-pix)", CARD: "var(--report-card)", CASH: "var(--report-cash)" } as const;

/** A Visão geral (desenho 14.1), com os blocos básicos (REL-01, 02, 03, 05, 08, 13). */
export function OverviewBody({ filters }: { filters: ReportFilters }) {
  const report = useOverviewReport(filters.query);
  if (report.isError) return <BlockError onRetry={() => report.refetch()} />;
  if (!report.data) return <Loading inline />;
  return <Overview report={report.data} />;
}

function Overview({ report }: { report: OverviewReport }) {
  const { current, compared } = report;
  return (
    <>
      <section aria-label={t.overview} className="grid grid-cols-2 gap-3.5 lg:grid-cols-3">
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
      </section>
      <div className="grid items-start gap-5 lg:grid-cols-2">
        <RevenueByDay report={report} />
        <Payments report={report} />
      </div>
      <div className="grid items-start gap-5 lg:grid-cols-[3fr_2fr]">
        <ChartCard title={t.heatmap.title} hint={t.heatmap.hint}>
          {report.heatmap.length > 0 ? <Heatmap cells={report.heatmap} /> : <NoData />}
        </ChartCard>
        <ChartCard title={t.topItems.title} note={report.topItems.length > 0 ? t.topItems.note : undefined}>
          {report.topItems.length > 0 ? (
            <BarList
              rows={report.topItems.map((item) => ({
                key: item.productId,
                label: item.name,
                value: item.revenueCents,
                detail: t.topItems.sold(item.quantity),
              }))}
            />
          ) : (
            <NoData />
          )}
        </ChartCard>
      </div>
    </>
  );
}

function RevenueByDay({ report }: { report: OverviewReport }) {
  const days = report.byDay;
  const best = days.reduce((a, b) => (b.revenueCents > a.revenueCents ? b : a), days[0]);
  const label = (day: string) => `${formatWeekday(day)} ${formatDayMonth(day)}`;
  return (
    <ChartCard title={t.byDay.title}>
      {days.some((d) => d.revenueCents > 0 || d.comparedRevenueCents > 0) ? (
        <ColumnChart
          label={t.byDay.chart(`${label(best.day)}, ${formatCents(best.revenueCents)}`)}
          legend={{ now: t.byDay.now, before: t.byDay.before }}
          columns={days.map((d) => ({
            key: d.day,
            label: days.length <= 7 ? formatWeekday(d.day) : String(Number(d.day.slice(8))),
            value: d.revenueCents,
            previous: d.comparedRevenueCents,
            tip: t.byDay.tip(
              label(d.day),
              formatCents(d.revenueCents),
              d.comparedDay ? label(d.comparedDay) : t.byDay.before,
              formatCents(d.comparedRevenueCents),
            ),
          }))}
        />
      ) : (
        <NoData height={220} />
      )}
    </ChartCard>
  );
}

function Payments({ report }: { report: OverviewReport }) {
  const methods = (["PIX", "CARD", "CASH"] as const).map((method) => ({
    key: method,
    label: t.payments.method[method],
    value: report.payments.find((p) => p.method === method)?.amountCents ?? 0,
    color: METHOD_COLOR[method],
  }));
  const total = methods.reduce((sum, m) => sum + m.value, 0);
  const parts = methods
    .filter((m) => m.value > 0)
    .map((m) => `${m.label} ${Math.round((m.value / total) * 100)}%`)
    .join(", ");
  return (
    <ChartCard title={t.payments.title} hint={t.payments.hint} note={total > 0 ? t.payments.note : undefined}>
      {total > 0 ? <SplitBar segments={methods} label={t.payments.chart(parts)} /> : <NoData />}
    </ChartCard>
  );
}

/** A tela da Visão geral, dentro da moldura dos relatórios. */
export function OverviewScreen() {
  return <ReportsScreen title={t.overview}>{(_, filters) => <OverviewBody filters={filters} />}</ReportsScreen>;
}
