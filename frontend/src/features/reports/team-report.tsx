"use client";

import { useState } from "react";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { BarList, ChartCard, ColumnChart, KpiTile, NoData } from "./charts";
import { formatDuration, timeVariation } from "./durations";
import { formatDayMonth, formatWeekday, lengthOf } from "./period";
import { useTeamReport, type TeamReport } from "./reports-api";
import { BlockError, Loading, ReportsScreen, type ReportFilters } from "./reports-page";

const t = messages.reports;
const m = t.team;

const onePlace = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 });
const share = new Intl.NumberFormat("pt-BR", { style: "percent", maximumFractionDigits: 0 });

const capitalize = (text: string) => text.charAt(0).toUpperCase() + text.slice(1);

/** Atendimento e equipe (desenho 14.5): a tela inteira é do Business. */
export function TeamScreen() {
  return (
    <ReportsScreen title={m.title} businessOnly={m.businessText}>
      {(_, filters) => <TeamBody filters={filters} />}
    </ReportsScreen>
  );
}

function TeamBody({ filters }: { filters: ReportFilters }) {
  const report = useTeamReport(filters.query);
  if (report.isError) return <BlockError onRetry={() => report.refetch()} />;
  if (!report.data) return <Loading inline />;
  const data = report.data;
  return (
    <>
      <Numbers report={data} days={lengthOf(filters.period)} />
      <Waiters report={data} />
      <div className="grid items-start gap-5 lg:grid-cols-2">
        <ResponseByHour report={data} />
        <ByType report={data} />
      </div>
      <div className="grid items-start gap-5 lg:grid-cols-3">
        <Tables report={data} />
        <Warnings report={data} />
        <Shifts report={data} />
      </div>
    </>
  );
}

function Numbers({ report, days }: { report: TeamReport; days: number }) {
  const now = report.current;
  const before = report.compared;
  const time = (seconds: number | null | undefined) => (seconds == null ? "—" : formatDuration(seconds));
  const delta = (a: number | null | undefined, b: number | null | undefined) =>
    a != null && b != null ? timeVariation(a, b) : null;
  return (
    <section aria-label={m.title} className="grid grid-cols-2 gap-3.5 lg:grid-cols-4">
      <KpiTile label={m.calls} value={String(now.calls)} hint={m.perDay(onePlace.format(now.calls / days))} />
      <KpiTile
        label={m.response}
        value={time(now.responseSeconds)}
        delta={delta(now.responseSeconds, before.responseSeconds)}
        hint={now.responseSeconds == null ? m.noCalls : t.delta.none}
      />
      <KpiTile
        label={m.done}
        value={time(now.doneSeconds)}
        delta={delta(now.doneSeconds, before.doneSeconds)}
        hint={now.doneSeconds == null ? m.noCalls : t.delta.none}
      />
      <KpiTile
        label={m.billToClose}
        value={time(now.billToCloseSeconds)}
        delta={delta(now.billToCloseSeconds, before.billToCloseSeconds)}
        hint={now.billToCloseSeconds == null ? m.noCalls : t.delta.none}
      />
    </section>
  );
}

type Column = "calls" | "response" | "delivered" | "closedTabs" | "revenue" | "staffOrders" | "tips";
type Waiter = TeamReport["waiters"][number];

const COLUMNS: { key: Column; value: (w: Waiter) => number; show: (w: Waiter) => string }[] = [
  { key: "calls", value: (w) => w.calls, show: (w) => String(w.calls) },
  {
    key: "response",
    // Resposta: quem responde mais rápido vem primeiro; quem não atendeu chamado, por último.
    value: (w) => -(w.responseSeconds ?? Number.MAX_SAFE_INTEGER),
    show: (w) => (w.responseSeconds == null ? "—" : formatDuration(w.responseSeconds)),
  },
  { key: "delivered", value: (w) => w.delivered, show: (w) => String(w.delivered) },
  { key: "closedTabs", value: (w) => w.closedTabs, show: (w) => String(w.closedTabs) },
  { key: "revenue", value: (w) => w.revenueCents, show: (w) => formatCents(w.revenueCents) },
  { key: "staffOrders", value: (w) => w.staffOrders, show: (w) => String(w.staffOrders) },
  { key: "tips", value: (w) => w.tipsCents, show: (w) => (w.tipsCents > 0 ? formatCents(w.tipsCents) : "—") },
];

/** A tabela de garçons (REL-46 a 49, 54), ordenável por qualquer coluna. */
function Waiters({ report }: { report: TeamReport }) {
  const [sort, setSort] = useState<Column>("calls");
  const column = COLUMNS.find((c) => c.key === sort)!;
  const rows = [...report.waiters].sort((a, b) => column.value(b) - column.value(a) || a.name.localeCompare(b.name));
  return (
    <ChartCard title={m.waiters} hint={m.waitersHint} note={rows.length > 0 ? m.waitersNote : undefined}>
      {rows.length === 0 ? (
        <NoData text={m.waitersEmpty} height={140} />
      ) : (
        <div className="relative -mx-5 overflow-x-auto px-5">
          <table className="w-full min-w-[860px] border-collapse text-sm">
            <thead>
              <tr className="text-left text-xs font-semibold tracking-[.4px] text-muted-foreground uppercase">
                <th scope="col" className="border-b border-border py-3 pr-3 font-semibold">
                  {m.columns.name}
                </th>
                {COLUMNS.map((c) => (
                  <th
                    key={c.key}
                    scope="col"
                    aria-sort={sort === c.key ? "descending" : "none"}
                    className="border-b border-border px-3 py-3 text-right font-semibold"
                  >
                    <button
                      type="button"
                      aria-label={m.sortBy(m.columns[c.key])}
                      onClick={() => setSort(c.key)}
                      className={cn(
                        "min-h-9 uppercase focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                        sort === c.key && "text-foreground",
                      )}
                    >
                      {m.columns[c.key]}
                      {sort === c.key && <span aria-hidden> {c.key === "response" ? "↑" : "↓"}</span>}
                    </button>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((w, index) => (
                <tr key={w.userId} className="border-b border-border last:border-b-0">
                  <td className="py-3 pr-3 whitespace-nowrap">
                    <span className="mr-2 inline-flex size-6 items-center justify-center rounded-full bg-secondary text-xs font-semibold">
                      {index + 1}
                    </span>
                    {w.name}
                  </td>
                  {COLUMNS.map((c) => (
                    <td key={c.key} className="px-3 py-3 text-right font-mono whitespace-nowrap tabular-nums">
                      {c.show(w)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </ChartCard>
  );
}

/** A resposta ao chamado por hora (REL-38). */
function ResponseByHour({ report }: { report: TeamReport }) {
  const hours = report.responseByHour;
  if (hours.length === 0) {
    return (
      <ChartCard title={m.byHour}>
        <NoData text={m.byHourEmpty} height={180} />
      </ChartCard>
    );
  }
  const first = hours[0].hour;
  const last = hours[hours.length - 1].hour;
  const all = Array.from({ length: last - first + 1 }, (_, i) => {
    const hour = first + i;
    return hours.find((h) => h.hour === hour) ?? { hour, responseSeconds: 0, calls: 0 };
  });
  const worst = all.reduce((a, b) => (b.responseSeconds > a.responseSeconds ? b : a), all[0]);
  return (
    <ChartCard title={m.byHour} hint={m.byHourHint}>
      <ColumnChart
        height={180}
        unit={60}
        tickFormat={(minutes) => onePlace.format(minutes)}
        label={m.hourChart(worst.hour, formatDuration(worst.responseSeconds))}
        columns={all.map((h) => ({
          key: String(h.hour),
          label: `${h.hour}h`,
          value: h.responseSeconds,
          tip: m.hourTip(h.hour, formatDuration(h.responseSeconds), h.calls),
        }))}
      />
    </ChartCard>
  );
}

/** Os chamados por tipo e os cancelados pelo cliente (REL-41, 42). */
function ByType({ report }: { report: TeamReport }) {
  const total = report.byType.reduce((sum, c) => sum + c.calls, 0);
  const canceled = report.byType.reduce((sum, c) => sum + c.canceled, 0);
  return (
    <ChartCard title={m.byType}>
      {total === 0 ? (
        <NoData text={m.noCalls} height={140} />
      ) : (
        <>
          <BarList
            format={(value) => String(value)}
            rows={report.byType.map((c) => ({ key: c.type, label: m.type[c.type], value: c.calls }))}
          />
          <div className="flex items-center gap-3 border-t border-border pt-3.5 text-sm">
            <span className="mr-auto">{m.canceled}</span>
            <span className="font-mono whitespace-nowrap tabular-nums">
              {m.canceledValue(canceled, share.format(canceled / total))}
            </span>
          </div>
        </>
      )}
    </ChartCard>
  );
}

/** As mesas que mais chamam por comanda (REL-44). */
function Tables({ report }: { report: TeamReport }) {
  return (
    <ChartCard title={m.tables} note={report.topTables.length > 0 ? m.tablesNote : undefined}>
      {report.topTables.length === 0 ? (
        <NoData text={m.tablesEmpty} height={120} />
      ) : (
        <dl className="flex flex-col gap-2.5 text-sm">
          {report.topTables.map((table) => (
            <div key={`${table.sector}-${table.label}`} className="flex items-baseline gap-3">
              <dt className="mr-auto min-w-0">{m.tableLabel(table.label, table.sector)}</dt>
              <dd className="font-mono whitespace-nowrap tabular-nums">
                {m.perTab(onePlace.format(table.calls / table.tabs))}
              </dd>
            </div>
          ))}
          {report.tableAverage != null && (
            <div className="flex items-baseline gap-3 text-muted-foreground">
              <dt className="mr-auto">{m.average}</dt>
              <dd className="font-mono tabular-nums">{onePlace.format(report.tableAverage)}</dd>
            </div>
          )}
        </dl>
      )}
    </ChartCard>
  );
}

/** A trava de pedido repetido (REL-50): mostrados, confirmados e tirados. */
function Warnings({ report }: { report: TeamReport }) {
  const rows: [string, number][] = [
    [m.shown, report.warningsShown],
    [m.confirmed, report.warningsConfirmed],
    [m.removed, report.warningsShown - report.warningsConfirmed],
  ];
  return (
    <ChartCard title={m.warnings} note={m.warningsNote}>
      <dl className="flex flex-col gap-2.5 text-sm">
        {rows.map(([label, value]) => (
          <div key={label} className="flex items-baseline gap-3">
            <dt className="mr-auto min-w-0">{label}</dt>
            <dd className="font-mono tabular-nums">{value}</dd>
          </div>
        ))}
      </dl>
    </ChartCard>
  );
}

/** Garçons ativos x chamados (REL-53): as horas com mais chamados por pessoa no turno. */
function Shifts({ report }: { report: TeamReport }) {
  return (
    <ChartCard title={m.shifts} note={m.shiftsNote}>
      {report.busiestShifts.length === 0 ? (
        <NoData text={m.shiftsEmpty} height={120} />
      ) : (
        <dl className="flex flex-col gap-2.5 text-sm">
          {report.busiestShifts.map((load) => (
            <div key={`${load.day}-${load.hour}`} className="flex items-baseline gap-3">
              <dt className="mr-auto min-w-0">
                {capitalize(m.shiftLabel(formatWeekday(load.day), formatDayMonth(load.day), load.hour, load.waiters))}
              </dt>
              <dd className="whitespace-nowrap tabular-nums">
                {m.perWaiter(onePlace.format(load.calls / load.waiters))}
              </dd>
            </div>
          ))}
        </dl>
      )}
    </ChartCard>
  );
}
