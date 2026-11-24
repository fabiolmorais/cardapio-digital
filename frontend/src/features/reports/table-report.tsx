"use client";

import Link from "next/link";
import { useState } from "react";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { ChartCard, ColumnChart, KpiTile, NoData, type Delta } from "./charts";
import { formatDuration } from "./durations";
import { lengthOf } from "./period";
import { useTableReport, type TableReport } from "./reports-api";
import { BlockError, Loading, ReportsScreen, type ReportFilters } from "./reports-page";

const t = messages.reports;
const m = t.tables;

const onePlace = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1, minimumFractionDigits: 1 });
const share = new Intl.NumberFormat("pt-BR", { style: "percent", maximumFractionDigits: 0 });
/** Nos quadrados do mapa, em reais redondos ("R$ 18.720"), para caber no celular. */
const reais = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL", maximumFractionDigits: 0 });
const round = (cents: number) => reais.format(Math.round(cents / 100));

/** Mesas e ocupação (desenho 14.6): a tela inteira é do Business. */
export function TablesScreen() {
  return (
    <ReportsScreen exportAs="tables" title={m.title} businessOnly={m.businessText}>
      {(context, filters) => <TablesBody filters={filters} timeZone={context.timeZone} />}
    </ReportsScreen>
  );
}

function TablesBody({ filters, timeZone }: { filters: ReportFilters; timeZone: string }) {
  const report = useTableReport(filters.query);
  if (report.isError) return <BlockError onRetry={() => report.refetch()} />;
  if (!report.data) return <Loading inline />;
  const data = report.data;
  return (
    <>
      <Numbers report={data} />
      <FloorMap report={data} />
      <div className="grid items-start gap-5 lg:grid-cols-2">
        <OccupancyByHour report={data} />
        <div className="flex min-w-0 flex-col gap-5">
          <Forgotten report={data} timeZone={timeZone} />
          <Moves report={data} days={lengthOf(filters.period)} />
        </div>
      </div>
    </>
  );
}

/** A diferença de um número em que mais é melhor, já escrita ("▲ 0,2", "▼ R$ 5,00"). */
function absolute(
  current: number | null | undefined,
  previous: number | null | undefined,
  show: (v: number) => string,
) {
  if (current == null || previous == null || previous === 0) return null;
  const diff = current - previous;
  if (show(Math.abs(diff)) === show(0)) return { text: t.delta.same, tone: "flat" } satisfies Delta;
  return diff > 0
    ? ({ text: t.delta.up(show(diff)), tone: "up" } satisfies Delta)
    : ({ text: t.delta.down(show(-diff)), tone: "down" } satisfies Delta);
}

function Numbers({ report }: { report: TableReport }) {
  const now = report.current;
  const before = report.compared;
  const split =
    now.rodizioStaySeconds != null && now.aLaCarteStaySeconds != null
      ? m.staySplit(formatDuration(now.rodizioStaySeconds), formatDuration(now.aLaCarteStaySeconds))
      : null;
  const stayHint =
    now.staySeconds == null
      ? m.noTabs
      : (split ?? (before.staySeconds != null ? m.stayBefore(formatDuration(before.staySeconds)) : t.delta.none));
  const peak = report.peak;
  return (
    <section aria-label={m.title} className="grid grid-cols-2 gap-3.5 lg:grid-cols-4">
      <KpiTile label={m.stay} value={now.staySeconds == null ? "—" : formatDuration(now.staySeconds)} hint={stayHint} />
      <KpiTile
        label={m.turnover}
        value={now.turnover == null ? "—" : onePlace.format(now.turnover)}
        delta={absolute(now.turnover, before.turnover, (v) => onePlace.format(v))}
        hint={t.delta.none}
        detail={m.turnoverDetail}
      />
      <KpiTile
        label={m.peak}
        value={peak ? share.format(peak.share) : "—"}
        hint={peak ? m.peakAt(m.weekdays[peak.weekday], peak.hour) : m.noTabs}
      />
      <KpiTile
        label={m.perHour}
        value={now.perOccupiedHourCents == null ? "—" : formatCents(now.perOccupiedHourCents)}
        delta={absolute(now.perOccupiedHourCents, before.perOccupiedHourCents, formatCents)}
        hint={now.perOccupiedHourCents == null ? m.noTabs : t.delta.none}
      />
    </section>
  );
}

type Metric = "revenue" | "tabs" | "ticket";
type Table = TableReport["tables"][number];

const METRIC: Record<Metric, { value: (t: Table) => number; show: (t: Table) => string }> = {
  revenue: { value: (t) => t.revenueCents, show: (t) => round(t.revenueCents) },
  tabs: { value: (t) => t.tabs, show: (t) => m.tabsValue(t.tabs) },
  ticket: {
    value: (t) => t.ticketCents ?? 0,
    show: (t) => (t.ticketCents == null ? "—" : round(t.ticketCents)),
  },
};

/** Até 60% de cor: o texto por cima continua legível nos dois temas. */
const LEVELS = [0.08, 0.19, 0.31, 0.44, 0.6];
function level(ratio: number) {
  if (ratio <= 0) return 0;
  return ratio < 0.35
    ? LEVELS[0]
    : ratio < 0.5
      ? LEVELS[1]
      : ratio < 0.65
        ? LEVELS[2]
        : ratio < 0.85
          ? LEVELS[3]
          : LEVELS[4];
}

/** O mapa do salão (REL-59): cada mesa, por setor, com a cor pelo que ela vende. */
function FloorMap({ report }: { report: TableReport }) {
  const [metric, setMetric] = useState<Metric>("revenue");
  const tables = report.tables;
  const sectors = [...new Set(tables.map((table) => table.sector))];
  const count = (sector: string) => tables.filter((table) => table.sector === sector).length;
  const hint = m.mapHint(tables.length, sectors.length > 1 ? sectors.map((s) => `${s} ${count(s)}`).join(", ") : "");
  const { value, show } = METRIC[metric];
  const max = Math.max(0, ...tables.map(value));
  const ranked = [...tables].filter((table) => table.tabs > 0).sort((a, b) => value(b) - value(a));
  const note =
    ranked.length >= 5
      ? m.mapNote(
          m.list(ranked.slice(0, 3).map((table) => table.label)),
          m.list(
            ranked
              .slice(-2)
              .reverse()
              .map((table) => table.label),
          ),
        )
      : undefined;

  return (
    <ChartCard
      title={m.map}
      hint={hint}
      note={note}
      action={
        <div role="group" aria-label={m.metricLabel} className="flex rounded-xl border border-border p-1">
          {(Object.keys(METRIC) as Metric[]).map((key) => (
            <button
              key={key}
              type="button"
              aria-pressed={metric === key}
              onClick={() => setMetric(key)}
              className={cn(
                "min-h-9 rounded-lg px-3 text-sm font-semibold text-muted-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                metric === key && "bg-foreground text-background",
              )}
            >
              {m.metric[key]}
            </button>
          ))}
        </div>
      }
    >
      {tables.length === 0 ? (
        <NoData text={m.mapEmpty} height={140} />
      ) : (
        <div className="flex flex-col gap-4">
          <div aria-hidden className="flex items-center justify-end gap-2 text-xs text-muted-foreground">
            {m.less}
            <span className="flex gap-0.5">
              {LEVELS.map((o) => (
                <b key={o} className="h-2.5 w-[18px] rounded-sm bg-report-now" style={{ opacity: o }} />
              ))}
            </span>
            {m.more}
          </div>
          {sectors.map((sector) => (
            <section key={sector} aria-label={sector} className="flex flex-col gap-2">
              <h3 className="text-sm text-muted-foreground">{sector}</h3>
              <ul className="grid grid-cols-3 gap-2.5 sm:grid-cols-4 lg:grid-cols-6">
                {tables
                  .filter((table) => table.sector === sector)
                  .map((table) => {
                    const tip = m.tableTip(table.label, formatCents(table.revenueCents), table.tabs);
                    return (
                      <li
                        key={table.label}
                        title={tip}
                        className="relative flex h-[92px] flex-col justify-between overflow-hidden rounded-2xl border border-border bg-secondary/40 px-3 py-2.5"
                      >
                        <span
                          aria-hidden
                          className="absolute inset-0 bg-report-now"
                          style={{ opacity: max > 0 ? level(value(table) / max) : 0 }}
                        />
                        <span className="sr-only">{tip}</span>
                        <span aria-hidden className="relative text-xl font-bold">
                          {table.label}
                        </span>
                        <span aria-hidden className="relative text-sm font-semibold tabular-nums">
                          {show(table)}
                        </span>
                      </li>
                    );
                  })}
              </ul>
            </section>
          ))}
        </div>
      )}
    </ChartCard>
  );
}

/**
 * As horas na ordem do expediente: um restaurante que vai até a 1h começa às 11h e termina à 1h,
 * e não com a madrugada na frente. O dia começa depois do maior intervalo sem movimento.
 */
export function businessHours(hours: number[]) {
  if (hours.length === 0) return [];
  const sorted = [...new Set(hours)].sort((a, b) => a - b);
  let start = sorted[0];
  let end = sorted[sorted.length - 1];
  let gap = sorted[0] + 24 - end;
  for (let i = 1; i < sorted.length; i++) {
    const g = sorted[i] - sorted[i - 1];
    if (g > gap) {
      gap = g;
      start = sorted[i];
      end = sorted[i - 1];
    }
  }
  const length = ((end - start + 24) % 24) + 1;
  return Array.from({ length }, (_, i) => (start + i) % 24);
}

/** A ocupação média por hora (REL-58). */
function OccupancyByHour({ report }: { report: TableReport }) {
  const hours = report.occupancyByHour;
  if (hours.length === 0) {
    return (
      <ChartCard title={m.byHour}>
        <NoData text={m.byHourEmpty} height={180} />
      </ChartCard>
    );
  }
  const all = businessHours(hours.map((h) => h.hour)).map(
    (hour) => hours.find((h) => h.hour === hour) ?? { hour, share: 0 },
  );
  const top = all.reduce((a, b) => (b.share > a.share ? b : a), all[0]);
  return (
    <ChartCard title={m.byHour} hint={m.byHourHint}>
      <ColumnChart
        height={180}
        unit={1}
        tickFormat={(value) => `${value}%`}
        label={m.hourChart(top.hour, share.format(top.share))}
        columns={all.map((h) => ({
          key: String(h.hour),
          label: `${h.hour}h`,
          value: Math.round(h.share * 100),
          tip: m.hourTip(h.hour, share.format(h.share)),
        }))}
      />
    </ChartCard>
  );
}

/** "15/11, 23h40", no fuso do restaurante. */
function when(iso: string, timeZone: string) {
  const parts = new Intl.DateTimeFormat("pt-BR", {
    timeZone,
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(new Date(iso));
  const part = (type: Intl.DateTimeFormatPartTypes) => parts.find((p) => p.type === type)?.value ?? "";
  return `${part("day")}/${part("month")}, ${part("hour")}h${part("minute")}`;
}

/** As comandas esquecidas abertas (REL-61). */
function Forgotten({ report, timeZone }: { report: TableReport; timeZone: string }) {
  return (
    <ChartCard
      title={m.forgotten}
      hint={m.forgottenHint}
      note={report.forgottenCount > 0 ? m.forgottenNote(report.forgottenCount) : undefined}
    >
      {report.forgotten.length === 0 ? (
        <NoData text={m.forgottenEmpty} height={100} />
      ) : (
        <ul className="flex flex-col text-sm">
          {report.forgotten.map((tab) => (
            <li
              key={tab.tabId}
              className="flex flex-wrap items-center gap-x-3 gap-y-1 border-b border-border py-2.5 last:border-b-0"
            >
              <span className="mr-auto min-w-0">{m.forgottenLabel(tab.label, when(tab.openedAt, timeZone))}</span>
              {tab.closedAt ? (
                <span className="text-muted-foreground">{m.closedAt(when(tab.closedAt, timeZone))}</span>
              ) : (
                <span className="rounded-full bg-destructive/15 px-2.5 py-0.5 text-xs font-semibold text-destructive">
                  {m.stillOpen}
                </span>
              )}
              <Link
                href={tab.closedAt ? `/admin/comandas/${tab.tabId}` : `/painel/comandas/${tab.tabId}`}
                aria-label={m.seeLabel(tab.label)}
                className="inline-flex min-h-9 items-center font-semibold underline underline-offset-4"
              >
                {m.see}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </ChartCard>
  );
}

/** As mudanças de mesa (REL-62). */
function Moves({ report, days }: { report: TableReport; days: number }) {
  const moves = report.moves;
  const rows: [string, string][] = [
    [m.movesLabel, m.movesValue(moves.moves, onePlace.format(moves.moves / days))],
    [m.merges, String(moves.merges)],
    [m.topFrom, moves.topFrom ? m.topFromValue(moves.topFrom, moves.topFromMoves) : "—"],
  ];
  return (
    <ChartCard title={m.moves}>
      <dl className="flex flex-col gap-2.5 text-sm">
        {rows.map(([label, value]) => (
          <div key={label} className="flex items-baseline gap-3">
            <dt className="mr-auto min-w-0">{label}</dt>
            <dd className="font-mono whitespace-nowrap tabular-nums">{value}</dd>
          </div>
        ))}
      </dl>
    </ChartCard>
  );
}
