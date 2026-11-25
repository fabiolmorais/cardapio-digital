"use client";

import { useState } from "react";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { axis, ChartCard, KpiTile, SplitBar, variation } from "./charts";
import { formatDuration } from "./durations";
import { formatDayMonth, formatMonth, formatWeekday } from "./period";
import { useUnitComparisonReport, type UnitComparison } from "./reports-api";
import { BlockError, Loading, ReportsScreen, type ReportFilters } from "./reports-page";

const t = messages.reports;
const u = t.units;

type Unit = UnitComparison["units"][number];
type Figures = Unit["figures"];
type Grouping = UnitComparison["grouping"];

/** Até 5 unidades com cor própria; as outras somam numa linha cinza. */
const MAX_LINES = 5;
const COLORS = [
  "var(--report-pix)",
  "var(--report-card)",
  "var(--report-cash)",
  "var(--report-unit-4)",
  "var(--report-unit-5)",
];
const OTHERS_COLOR = "var(--report-prev)";

const rating = new Intl.NumberFormat("pt-BR", { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const decimal = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 });
const compact = new Intl.NumberFormat("pt-BR", { notation: "compact", maximumFractionDigits: 1 });

/**
 * Unidades (desenho 14.17, REL-80): a rede no período, as unidades lado a lado (ordenáveis), a
 * evolução de cada uma e a parte de cada uma no faturamento. Só no Enterprise, para quem é dono de
 * todas as unidades (o back decide).
 */
export function UnitsScreen() {
  return (
    <ReportsScreen title={u.title} exportAs="units" units>
      {(_, filters) => <UnitsBody filters={filters} />}
    </ReportsScreen>
  );
}

function UnitsBody({ filters }: { filters: ReportFilters }) {
  const report = useUnitComparisonReport({ from: filters.query.from, to: filters.query.to });
  if (report.isError) return <BlockError onRetry={() => report.refetch()} />;
  if (!report.data) return <Loading inline />;
  const data = report.data;
  const colors = colorsOf(data.units);
  return (
    <>
      <p className="text-sm text-muted-foreground">{u.intro(data.units.length)}</p>
      <Network figures={data.network} />
      <SideBySide report={data} colors={colors} />
      <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
        <Evolution report={data} colors={colors} />
        <Share report={data} colors={colors} />
      </div>
    </>
  );
}

/**
 * A cor segue a unidade, não a posição: as que têm cor (até 5, as que mais faturaram) recebem a cor
 * pela ordem do nome, e trocar o período não repinta ninguém sem necessidade.
 */
function colorsOf(units: Unit[]) {
  const colored = units.slice(0, MAX_LINES).map((unit) => unit.name);
  const byName = [...colored].sort((a, b) => a.localeCompare(b, "pt-BR"));
  const map = new Map<string, string>();
  units.forEach((unit) => {
    const index = byName.indexOf(unit.name);
    map.set(unit.id, index >= 0 ? COLORS[index] : OTHERS_COLOR);
  });
  return map;
}

function Network({ figures }: { figures: Figures }) {
  const { current, compared } = figures;
  return (
    <section aria-label={u.network} className="grid grid-cols-2 gap-3.5 lg:grid-cols-4">
      <KpiTile
        label={u.kpi.revenue}
        value={formatCents(current.revenueCents)}
        current={current.revenueCents}
        previous={compared.revenueCents}
      />
      <KpiTile label={u.kpi.tabs} value={String(current.tabs)} current={current.tabs} previous={compared.tabs} />
      <KpiTile
        label={u.kpi.ticket}
        value={formatCents(current.averageTicketCents)}
        current={current.averageTicketCents}
        previous={compared.averageTicketCents}
      />
      <KpiTile
        label={u.kpi.rating}
        value={figures.ratingAverage == null ? "—" : `${rating.format(figures.ratingAverage)} ${u.kpi.ratingOf}`}
        hint={figures.ratingAverage == null ? u.kpi.fewRatings : u.kpi.ratings(figures.ratings)}
      />
    </section>
  );
}

type Column = keyof typeof u.table.columns;

/**
 * As colunas: o valor para ordenar e achar o melhor (null fica de fora), se menos é melhor (os tempos)
 * e como mostrar.
 */
const COLUMNS: {
  key: Column;
  value: (f: Figures) => number | null;
  lowerIsBetter?: boolean;
  show: (f: Figures) => string;
}[] = [
  { key: "revenue", value: (f) => f.current.revenueCents, show: (f) => formatCents(f.current.revenueCents) },
  { key: "tabs", value: (f) => f.current.tabs, show: (f) => String(f.current.tabs) },
  {
    key: "ticket",
    value: (f) => (f.current.tabs > 0 ? f.current.averageTicketCents : null),
    show: (f) => (f.current.tabs > 0 ? formatCents(f.current.averageTicketCents) : "—"),
  },
  {
    key: "prep",
    value: (f) => f.prepSeconds ?? null,
    lowerIsBetter: true,
    show: (f) => (f.prepSeconds == null ? "—" : formatDuration(f.prepSeconds)),
  },
  {
    key: "response",
    value: (f) => f.callResponseSeconds ?? null,
    lowerIsBetter: true,
    show: (f) => (f.callResponseSeconds == null ? "—" : formatDuration(f.callResponseSeconds)),
  },
  {
    key: "rating",
    value: (f) => f.ratingAverage ?? null,
    show: (f) => u.table.rating(f.ratingAverage == null ? null : rating.format(f.ratingAverage), f.ratings),
  },
];

/** A unidade com o melhor valor da coluna (com duas ou mais unidades com valor). */
function bestOf(units: Unit[], column: (typeof COLUMNS)[number]) {
  const values = units.map((unit) => column.value(unit.figures)).filter((v): v is number => v !== null);
  if (values.length < 2) return null;
  return column.lowerIsBetter ? Math.min(...values) : Math.max(...values);
}

/** As unidades lado a lado, ordenáveis por qualquer coluna; a posição é a do ranking escolhido. */
function SideBySide({ report, colors }: { report: UnitComparison; colors: Map<string, string> }) {
  const [sort, setSort] = useState<Column>("revenue");
  const column = COLUMNS.find((c) => c.key === sort)!;
  const sortValue = (unit: Unit) => {
    const value = column.value(unit.figures);
    if (value === null) return Number.NEGATIVE_INFINITY;
    return column.lowerIsBetter ? -value : value;
  };
  const rows = [...report.units].sort((a, b) => sortValue(b) - sortValue(a) || a.name.localeCompare(b.name));
  const best = new Map(COLUMNS.map((c) => [c.key, bestOf(report.units, c)]));
  const cell = "px-3 py-3 text-right font-mono whitespace-nowrap tabular-nums";
  return (
    <ChartCard title={u.table.title} hint={u.table.hint} note={u.table.note}>
      <div className="relative -mx-5 overflow-x-auto px-5">
        <table className="w-full min-w-[860px] border-collapse text-sm">
          <thead>
            <tr className="text-left text-xs font-semibold tracking-[.4px] text-muted-foreground uppercase">
              <th scope="col" className="border-b border-border py-3 pr-3 font-semibold">
                {u.table.unit}
              </th>
              {COLUMNS.map((c) => (
                <th
                  key={c.key}
                  scope="col"
                  aria-sort={sort === c.key ? (c.lowerIsBetter ? "ascending" : "descending") : "none"}
                  className="border-b border-border px-3 py-3 text-right font-semibold"
                >
                  <button
                    type="button"
                    aria-label={u.table.sortBy(u.table.columns[c.key])}
                    onClick={() => setSort(c.key)}
                    className={cn(
                      "min-h-9 uppercase focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                      sort === c.key && "text-foreground",
                    )}
                  >
                    {u.table.columns[c.key]}
                    {sort === c.key && <span aria-hidden> {c.lowerIsBetter ? "↑" : "↓"}</span>}
                  </button>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((unit, index) => {
              const delta = variation(unit.figures.current.revenueCents, unit.figures.compared.revenueCents);
              return (
                <tr key={unit.id} className="border-b border-border">
                  <th scope="row" className="py-3 pr-3 text-left font-medium whitespace-nowrap">
                    <span className="mr-2 inline-flex size-6 items-center justify-center rounded-full bg-secondary text-xs font-semibold">
                      {index + 1}
                    </span>
                    <i
                      aria-hidden
                      className="mr-2 inline-block size-2.5 rounded-[3px] align-middle"
                      style={{ background: colors.get(unit.id) }}
                    />
                    {unit.name}
                  </th>
                  {COLUMNS.map((c) => {
                    const value = c.value(unit.figures);
                    const isBest = value !== null && value === best.get(c.key);
                    const shown = c.show(unit.figures);
                    return (
                      <td key={c.key} className={cell}>
                        {isBest ? (
                          <span className="inline-flex items-center gap-1.5 rounded-full bg-secondary px-2 py-0.5 font-semibold">
                            <span aria-hidden>{shown}</span>
                            <span className="sr-only">{u.table.bestLabel(shown)}</span>
                            <span aria-hidden className="font-sans text-[11px] text-muted-foreground">
                              {u.table.best}
                            </span>
                          </span>
                        ) : (
                          shown
                        )}
                        {c.key === "revenue" && (
                          <span
                            className={cn(
                              "block font-sans text-xs font-semibold",
                              delta.tone === "up" && "text-success",
                              delta.tone === "down" && "text-destructive",
                              delta.tone === "flat" && "text-muted-foreground",
                            )}
                          >
                            {delta.text}
                            {delta.tone !== "flat" && <span className="sr-only"> {t.delta.vsPrevious}</span>}
                          </span>
                        )}
                      </td>
                    );
                  })}
                </tr>
              );
            })}
          </tbody>
          <tfoot>
            <tr className="font-semibold">
              <th scope="row" className="py-3 pr-3 text-left">
                {u.table.network}
              </th>
              {COLUMNS.map((c) => (
                <td key={c.key} className={cell}>
                  {c.show(report.network)}
                </td>
              ))}
            </tr>
          </tfoot>
        </table>
      </div>
    </ChartCard>
  );
}

type Metric = "revenue" | "tabs";
type Line = { key: string; name: string; color: string; values: number[] };

/** As linhas do gráfico: até 5 unidades e, com mais, "Outras unidades" somadas ponto a ponto. */
function linesOf(report: UnitComparison, colors: Map<string, string>, metric: Metric): Line[] {
  const value = (p: Unit["series"][number]) => (metric === "revenue" ? p.revenuePerDayCents : p.tabsPerDay);
  const lines: Line[] = report.units.slice(0, MAX_LINES).map((unit) => ({
    key: unit.id,
    name: unit.name,
    color: colors.get(unit.id)!,
    values: unit.series.map(value),
  }));
  const rest = report.units.slice(MAX_LINES);
  if (rest.length > 0) {
    lines.push({
      key: "others",
      name: u.evolution.others,
      color: OTHERS_COLOR,
      values: rest[0].series.map((_, i) =>
        rest.reduce((sum, unit) => sum + (unit.series[i] ? value(unit.series[i]) : 0), 0),
      ),
    });
  }
  return lines;
}

function pointLabel(start: string, grouping: Grouping, count: number) {
  if (grouping === "MONTH") return formatMonth(start);
  if (grouping === "WEEK") return formatDayMonth(start);
  return count <= 7 ? formatWeekday(start) : formatDayMonth(start);
}

/**
 * A evolução no período: uma linha por unidade, com o faturamento ou as comandas por dia. O gráfico
 * se diz em texto (de quanto a quanto cada unidade foi), e cada ponto mostra o valor ao passar o mouse.
 */
function Evolution({ report, colors }: { report: UnitComparison; colors: Map<string, string> }) {
  const e = u.evolution;
  const [metric, setMetric] = useState<Metric>("revenue");
  const lines = linesOf(report, colors, metric);
  const points = report.units[0]?.series ?? [];
  const format = (v: number) => (metric === "revenue" ? formatCents(Math.round(v)) : decimal.format(v));
  const { top, steps } = axis(Math.max(0, ...lines.flatMap((l) => l.values)));
  const x = (i: number) => (points.length <= 1 ? 50 : 2 + (i / (points.length - 1)) * 96);
  const y = (v: number) => 100 - (v / top) * 100;
  const tick = (v: number) => (metric === "revenue" ? compact.format(v / 100) : decimal.format(v));
  // Até 8 rótulos embaixo, espalhados, para não amontoar.
  const every = Math.max(1, Math.ceil(points.length / 8));
  const metricName = metric === "revenue" ? e.revenue : e.tabs;
  const summary = e.chart(
    metricName,
    lines.map((l) => e.from(l.name, format(l.values[0] ?? 0), format(l.values[l.values.length - 1] ?? 0))).join("; "),
  );
  return (
    <ChartCard
      title={e.title}
      note={[e.note[report.grouping], report.units.length > MAX_LINES ? e.many(report.units.length - MAX_LINES) : ""]
        .filter(Boolean)
        .join(" ")}
      action={
        <div role="group" aria-label={e.show} className="flex gap-1 rounded-[12px] border border-border p-1">
          {(["revenue", "tabs"] as const).map((option) => (
            <button
              key={option}
              type="button"
              aria-pressed={metric === option}
              onClick={() => setMetric(option)}
              className={cn(
                "h-9 rounded-[9px] px-3 text-sm font-semibold text-muted-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                metric === option && "bg-foreground text-background",
              )}
            >
              {option === "revenue" ? e.revenue : e.tabs}
            </button>
          ))}
        </div>
      }
    >
      <ul aria-label={u.table.unit} className="flex flex-wrap gap-x-4 gap-y-1.5 text-sm">
        {lines.map((l) => (
          <li key={l.key} className="inline-flex items-center gap-2">
            <i aria-hidden className="h-0.5 w-4 rounded-full" style={{ background: l.color }} />
            {l.name}
          </li>
        ))}
      </ul>
      <div role="img" aria-label={summary} className="flex gap-2">
        <div
          aria-hidden
          className="flex h-[220px] w-14 shrink-0 flex-col justify-between text-right text-xs text-muted-foreground"
        >
          {steps.map((s) => (
            <span key={s} className="-my-2 leading-4">
              {tick(s)}
            </span>
          ))}
        </div>
        <div className="relative h-[220px] min-w-0 flex-1">
          {steps.map((s) => (
            <i
              key={s}
              aria-hidden
              className="absolute inset-x-0 border-t border-border"
              style={{ top: `${(1 - s / top) * 100}%` }}
            />
          ))}
          <svg
            aria-hidden
            viewBox="0 0 100 100"
            preserveAspectRatio="none"
            className="absolute inset-0 size-full overflow-visible"
          >
            {lines.map((l) => (
              <polyline
                key={l.key}
                points={l.values.map((v, i) => `${x(i)},${y(v)}`).join(" ")}
                fill="none"
                stroke={l.color}
                strokeWidth={2}
                strokeLinejoin="round"
                strokeLinecap="round"
                vectorEffect="non-scaling-stroke"
              />
            ))}
          </svg>
          {lines.map((l) =>
            l.values.map((v, i) => (
              <span
                key={`${l.key}-${i}`}
                aria-hidden
                title={e.tip(l.name, pointLabel(points[i].start, report.grouping, points.length), e.perDay(format(v)))}
                className="absolute size-2.5 -translate-x-1/2 -translate-y-1/2 rounded-full ring-2 ring-card"
                style={{ left: `${x(i)}%`, top: `${y(v)}%`, background: l.color }}
              />
            )),
          )}
        </div>
      </div>
      <div aria-hidden className="relative ml-16 h-4 text-xs text-muted-foreground">
        {points.map((p, i) =>
          i % every === 0 ? (
            <span key={p.start} className="absolute -translate-x-1/2 whitespace-nowrap" style={{ left: `${x(i)}%` }}>
              {pointLabel(p.start, report.grouping, points.length)}
            </span>
          ) : null,
        )}
      </div>
    </ChartCard>
  );
}

/** A parte de cada unidade no faturamento do período. */
function Share({ report, colors }: { report: UnitComparison; colors: Map<string, string> }) {
  const shown = report.units.slice(0, MAX_LINES);
  const rest = report.units.slice(MAX_LINES);
  const segments = [
    ...shown.map((unit) => ({
      key: unit.id,
      label: unit.name,
      value: unit.figures.current.revenueCents,
      color: colors.get(unit.id)!,
    })),
    ...(rest.length > 0
      ? [
          {
            key: "others",
            label: u.evolution.others,
            value: rest.reduce((sum, unit) => sum + unit.figures.current.revenueCents, 0),
            color: OTHERS_COLOR,
          },
        ]
      : []),
  ];
  const total = segments.reduce((sum, s) => sum + s.value, 0);
  const parts = segments.map((s) => `${s.label} ${total === 0 ? 0 : Math.round((s.value / total) * 100)}%`).join(", ");
  return (
    <ChartCard title={u.share.title}>
      <SplitBar segments={segments} label={u.share.chart(parts)} />
    </ChartCard>
  );
}
