"use client";

import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { BarList, ChartCard, ColumnChart, KpiTile, NoData, SplitBar } from "./charts";
import { formatDuration } from "./durations";
import { formatDayMonth, formatMonth, formatRange, formatWeekday } from "./period";
import { usePhoneRodizioReport, type PhoneRodizioReport } from "./reports-api";
import { BlockError, Loading, ReportsScreen, type ReportFilters } from "./reports-page";

const t = messages.reports;
const m = t.phone;

const onePlace = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1, minimumFractionDigits: 1 });
const share = new Intl.NumberFormat("pt-BR", { style: "percent", maximumFractionDigits: 0 });
const whole = new Intl.NumberFormat("pt-BR");
const ratio = (part: number, whole: number) => (whole === 0 ? 0 : part / whole);

/** Pedido pelo celular e rodízio (desenho 14.7): a tela inteira é do Business. */
export function PhoneRodizioScreen() {
  return (
    <ReportsScreen title={m.title} businessOnly={m.businessText}>
      {(_, filters) => <PhoneRodizioBody filters={filters} />}
    </ReportsScreen>
  );
}

function PhoneRodizioBody({ filters }: { filters: ReportFilters }) {
  const report = usePhoneRodizioReport(filters.query);
  if (report.isError) return <BlockError onRetry={() => report.refetch()} />;
  if (!report.data) return <Loading inline />;
  const data = report.data;
  return (
    <>
      <h2 className="pt-1 text-xl font-semibold">{m.phoneSection}</h2>
      <div className="grid items-start gap-5 lg:grid-cols-2">
        <Origin report={data} />
        <Funnel report={data} />
      </div>
      <Presence report={data} />
      <h2 className="pt-3 text-xl font-semibold">{m.rodizioSection}</h2>
      <RodizioNumbers report={data} />
      <div className="grid items-start gap-5 lg:grid-cols-2">
        <TopItems report={data} />
        <Passagem report={data} />
      </div>
    </>
  );
}

type Point = PhoneRodizioReport["origin"][number];

function pointLabel(point: Point, grouping: PhoneRodizioReport["grouping"], count: number) {
  if (grouping === "MONTH") return formatMonth(point.start);
  if (grouping === "WEEK") return formatDayMonth(point.start);
  return count <= 7 ? formatWeekday(point.start) : String(Number(point.start.slice(8)));
}

/** Quem lança os pedidos (REL-63): a parte do celular e a do garçom em cada dia, semana ou mês. */
function Origin({ report }: { report: PhoneRodizioReport }) {
  const points = report.origin;
  const phone = points.reduce((sum, p) => sum + p.phone, 0);
  const total = points.reduce((sum, p) => sum + p.phone + p.staff, 0);
  const overall = share.format(ratio(phone, total));
  return (
    <ChartCard title={m.origin} note={total > 0 ? m.originNote(overall) : undefined}>
      {total === 0 ? (
        <NoData text={m.originEmpty} height={200} />
      ) : (
        <ColumnChart
          stacked
          height={200}
          unit={1}
          tickFormat={(value) => `${value}%`}
          legend={{ now: m.phoneKey, before: m.staffKey }}
          label={m.originChart(overall)}
          columns={points.map((p) => {
            const all = p.phone + p.staff;
            const phoneShare = Math.round(ratio(p.phone, all) * 100);
            return {
              key: p.start,
              label: pointLabel(p, report.grouping, points.length),
              value: all === 0 ? 0 : phoneShare,
              previous: all === 0 ? 0 : 100 - phoneShare,
              tip: m.originTip(
                formatRange({ from: p.start, to: p.end }, t.range),
                share.format(ratio(p.phone, all)),
                p.phone,
                share.format(ratio(p.staff, all)),
                p.staff,
              ),
            };
          })}
        />
      )}
    </ChartCard>
  );
}

/** Do QR ao pedido (REL-64, 65, 67, 68, 69). */
function Funnel({ report }: { report: PhoneRodizioReport }) {
  const { funnel, phones } = report;
  const steps = [
    { key: "tabs", label: m.funnelSteps.tabs, value: funnel.tabs },
    { key: "scanned", label: m.funnelSteps.scanned, value: funnel.scanned },
    { key: "phoneOrdered", label: m.funnelSteps.phoneOrdered, value: funnel.phoneOrdered },
  ];
  const rows: [string, string][] = [
    [
      m.phonesPerTab,
      phones.phonesPerTab == null
        ? "—"
        : m.phonesPerTabValue(onePlace.format(phones.phonesPerTab), onePlace.format(phones.orderingPerTab ?? 0)),
    ],
    [m.scanToOrder, phones.scanToOrderSeconds == null ? "—" : formatDuration(phones.scanToOrderSeconds)],
    [m.scannedNoOrder, m.tablesValue(phones.scannedNoOrder)],
    [m.rejected, String(phones.rejected)],
    [m.blocked, String(phones.blocked)],
  ];
  return (
    <ChartCard title={m.funnel} note={m.funnelNote}>
      {funnel.tabs === 0 ? (
        <NoData text={m.funnelEmpty} height={120} />
      ) : (
        <ul
          role="img"
          aria-label={m.funnelChart(funnel.tabs, funnel.scanned, funnel.phoneOrdered)}
          className="flex flex-col gap-2.5"
        >
          {steps.map((step) => (
            <li
              key={step.key}
              className="grid grid-cols-[minmax(0,1fr)_minmax(0,1.6fr)_3.5rem] items-center gap-3 text-sm max-sm:grid-cols-[minmax(0,1fr)_3.5rem]"
            >
              <span>{step.label}</span>
              <span aria-hidden className="flex h-7 max-sm:order-last max-sm:col-span-2">
                <span
                  className="flex h-7 items-center rounded-r-md bg-report-now pl-2.5 text-[13px] font-bold text-[#1A0E0A] tabular-nums"
                  style={{ width: `${Math.max(ratio(step.value, funnel.tabs) * 100, 8)}%` }}
                >
                  {whole.format(step.value)}
                </span>
              </span>
              <span className="text-right font-mono text-[13px] tabular-nums">
                {share.format(ratio(step.value, funnel.tabs))}
              </span>
            </li>
          ))}
        </ul>
      )}
      <dl className="flex flex-col gap-2.5 border-t border-border pt-3.5 text-sm">
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

const PRESENCE_COLOR = { WIFI: "var(--report-pix)", GPS: "var(--report-card)", STAFF: "var(--report-cash)" } as const;

/** Como a presença foi confirmada (REL-66). */
function Presence({ report }: { report: PhoneRodizioReport }) {
  const segments = (["WIFI", "GPS", "STAFF"] as const).map((method) => ({
    key: method,
    label: m.presenceMethod[method],
    value: report.presence.find((p) => p.method === method)?.sessions ?? 0,
    color: PRESENCE_COLOR[method],
  }));
  const total = segments.reduce((sum, s) => sum + s.value, 0);
  const parts = segments
    .filter((s) => s.value > 0)
    .map((s) => `${s.label} ${share.format(ratio(s.value, total))}`)
    .join(", ");
  return (
    <ChartCard title={m.presence} hint={m.presenceHint}>
      {total === 0 ? (
        <NoData text={m.presenceEmpty} height={100} />
      ) : (
        <SplitBar segments={segments} label={m.presenceChart(parts)} format={m.phones} />
      )}
    </ChartCard>
  );
}

/** Os números do rodízio (REL-70, 71, 73, 75). */
function RodizioNumbers({ report }: { report: PhoneRodizioReport }) {
  const r = report.rodizio;
  const none = r.tabs === 0;
  const tiers = report.tiers.map((tier) => m.tier(tier.people, tier.tier)).join(" · ");
  return (
    <section aria-label={m.rodizioSection} className="grid grid-cols-2 gap-3.5 lg:grid-cols-4">
      <KpiTile label={m.people} value={whole.format(r.people)} hint={none ? m.noRodizio : tiers} />
      <KpiTile
        label={m.revenue}
        value={formatCents(r.revenueCents)}
        hint={none ? m.noRodizio : m.extra(formatCents(r.extraCents))}
      />
      <KpiTile
        label={m.rounds}
        value={none ? "—" : onePlace.format(r.rounds / r.tabs)}
        hint={none ? m.noRodizio : m.itemsPerPerson(onePlace.format(ratio(r.items, r.people)))}
      />
      <KpiTile
        label={m.time}
        value={r.timeSeconds == null ? "—" : formatDuration(r.timeSeconds)}
        hint={
          none
            ? m.noRodizio
            : r.withLimit === 0
              ? m.noLimit
              : m.reached(share.format(ratio(r.reachedLimit, r.withLimit)))
        }
      />
    </section>
  );
}

/** Os itens mais pedidos no rodízio (REL-72). */
function TopItems({ report }: { report: PhoneRodizioReport }) {
  return (
    <ChartCard title={m.topItems} hint={m.topItemsHint}>
      {report.topItems.length === 0 ? (
        <NoData text={m.topItemsEmpty} height={140} />
      ) : (
        <BarList
          format={(value) => whole.format(value)}
          rows={report.topItems.map((item) => ({
            key: item.name,
            label: item.name,
            value: item.quantity,
            detail: m.units(item.quantity),
          }))}
        />
      )}
    </ChartCard>
  );
}

/** A passagem (REL-74): o tempo do cartão em cada cor e os cortes mais pedidos. */
function Passagem({ report }: { report: PhoneRodizioReport }) {
  const { servingSeconds, pausedSeconds } = report.card;
  const total = servingSeconds + pausedSeconds;
  const serving = share.format(ratio(servingSeconds, total));
  const paused = share.format(ratio(pausedSeconds, total));
  return (
    <ChartCard title={m.passagem} note={m.passagemNote}>
      {total === 0 && report.topCuts.length === 0 ? (
        <NoData text={m.passagemEmpty} height={140} />
      ) : (
        <>
          {total > 0 && (
            <SplitBar
              label={m.cardChart(serving, paused)}
              format={(seconds) => formatDuration(seconds)}
              segments={[
                { key: "serving", label: m.serving, value: servingSeconds, color: "var(--report-cash)" },
                { key: "paused", label: m.paused, value: pausedSeconds, color: "var(--destructive)" },
              ]}
            />
          )}
          {report.topCuts.length > 0 && (
            <div className="flex flex-col gap-2.5 border-t border-border pt-3.5 text-sm">
              <h3 className="font-semibold text-muted-foreground">{m.cuts}</h3>
              <dl className="flex flex-col gap-2.5">
                {report.topCuts.map((cut) => (
                  <div key={cut.name} className="flex items-baseline gap-3">
                    <dt className="mr-auto min-w-0">{cut.name}</dt>
                    <dd className="font-mono whitespace-nowrap tabular-nums">{m.orders(cut.quantity)}</dd>
                  </div>
                ))}
              </dl>
            </div>
          )}
        </>
      )}
    </ChartCard>
  );
}
