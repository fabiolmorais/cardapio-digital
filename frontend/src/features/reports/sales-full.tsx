import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { BarList, ChartCard, KpiTile, NoData, variation, type Delta } from "./charts";
import type { SalesReport } from "./reports-api";

const t = messages.reports;
const f = t.salesFull;

type Full = NonNullable<SalesReport["full"]>;

const share = new Intl.NumberFormat("pt-BR", { style: "percent", maximumFractionDigits: 1 });
const onePlace = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 });

/**
 * A variação da parte cancelada do faturamento, em pontos percentuais: menos é melhor ("▼ 0,4 ponto,
 * menos", em verde).
 */
function canceledDelta(full: Full, revenue: number, comparedRevenue: number): Delta | null {
  if (revenue === 0 || comparedRevenue === 0) return null;
  const now = (full.canceledCents / revenue) * 100;
  const before = (full.comparedCanceledCents / comparedRevenue) * 100;
  const diff = Math.round((now - before) * 10) / 10;
  if (diff === 0) return { text: t.delta.same, tone: "flat" };
  const points = f.points(onePlace.format(Math.abs(diff)));
  return diff < 0 ? { text: f.canceledLess(points), tone: "up" } : { text: f.canceledMore(points), tone: "down" };
}

/** Os 2 números do Business (REL-11, 12), depois dos 6 básicos. */
export function SalesFullTiles({ report, full }: { report: SalesReport; full: Full }) {
  const revenue = report.current.revenueCents;
  return (
    <>
      <KpiTile label={f.advance} value={formatCents(full.advanceCents)} hint={f.advanceHint(full.advanceTabs)} />
      <KpiTile
        label={f.canceled}
        value={formatCents(full.canceledCents)}
        delta={canceledDelta(full, revenue, report.compared.revenueCents)}
        hint={revenue > 0 ? share.format(full.canceledCents / revenue) : t.delta.none}
      />
    </>
  );
}

/** Por setor (REL-06): o faturamento e quanto cada mesa vende por dia. */
export function SectorsCard({ full }: { full: Full }) {
  const sold = full.bySector.filter((s) => s.revenueCents > 0);
  return (
    <ChartCard title={f.bySector}>
      {sold.length === 0 ? (
        <NoData text={f.noSectors} height={140} />
      ) : (
        <>
          <BarList
            rows={sold.map((s) => ({
              key: s.sectorId,
              label: f.sectorLabel(s.name, s.tables),
              value: s.revenueCents,
            }))}
          />
          <dl className="flex flex-col gap-2 border-t border-border pt-3.5 text-sm">
            {sold.map((s) => (
              <div key={s.sectorId} className="flex items-center gap-3">
                <dt className="mr-auto text-muted-foreground">{f.perTable(s.name)}</dt>
                <dd className="font-mono tabular-nums">{formatCents(s.perTablePerDayCents)}</dd>
              </div>
            ))}
          </dl>
        </>
      )}
    </ChartCard>
  );
}

/** As mesas que mais vendem (REL-07). */
export function TablesCard({ full }: { full: Full }) {
  const c = f.tables.columns;
  return (
    <ChartCard title={f.tables.title}>
      {full.topTables.length === 0 ? (
        <NoData text={f.tables.empty} height={140} />
      ) : (
        <div className="relative -mx-5 overflow-x-auto px-5">
          <table className="w-full min-w-[480px] border-collapse text-sm">
            <thead>
              <tr className="text-left text-xs font-semibold tracking-[.4px] text-muted-foreground uppercase">
                <th scope="col" className="border-b border-border py-3 pr-3 font-semibold">
                  {c.table}
                </th>
                <th scope="col" className="border-b border-border px-3 py-3 font-semibold">
                  {c.sector}
                </th>
                <th scope="col" className="border-b border-border px-3 py-3 text-right font-semibold">
                  {c.tabs}
                </th>
                <th scope="col" className="border-b border-border px-3 py-3 text-right font-semibold">
                  {c.ticket}
                </th>
                <th scope="col" className="border-b border-border py-3 pl-3 text-right font-semibold">
                  {c.revenue}
                </th>
              </tr>
            </thead>
            <tbody>
              {full.topTables.map((table, index) => (
                <tr key={table.tableId} className="border-b border-border last:border-b-0">
                  <td className="py-3 pr-3 whitespace-nowrap">
                    <span className="mr-2 inline-flex size-6 items-center justify-center rounded-full bg-secondary text-xs font-semibold">
                      {index + 1}
                    </span>
                    {f.tables.table(table.label)}
                  </td>
                  <td className="px-3 py-3 text-muted-foreground">{table.sector}</td>
                  <td className="px-3 py-3 text-right font-mono tabular-nums">{table.tabs}</td>
                  <td className="px-3 py-3 text-right font-mono whitespace-nowrap tabular-nums">
                    {formatCents(table.averageTicketCents)}
                  </td>
                  <td className="py-3 pl-3 text-right font-mono whitespace-nowrap tabular-nums">
                    {formatCents(table.revenueCents)}
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

function TicketRow({ label, now, before }: { label: string; now?: number | null; before?: number | null }) {
  const delta = now != null && before != null ? variation(now, before) : null;
  return (
    <div className="flex items-center gap-3 text-sm">
      <dt className="mr-auto min-w-0">{label}</dt>
      <dd
        className={cn(
          "text-[13px] font-semibold whitespace-nowrap",
          delta?.tone === "up" && "text-success",
          delta?.tone === "down" && "text-destructive",
          (!delta || delta.tone === "flat") && "text-muted-foreground",
        )}
      >
        {now == null ? f.ticket.noData : delta ? delta.text : t.delta.none}
      </dd>
      <dd className="w-24 shrink-0 text-right font-mono whitespace-nowrap tabular-nums sm:w-28">
        {now == null ? "—" : formatCents(now)}
      </dd>
    </div>
  );
}

/** O tíquete por comanda e por pessoa, no rodízio e fora dele (REL-03, 04). */
export function TicketCard({ report, full }: { report: SalesReport; full: Full }) {
  return (
    <ChartCard title={f.ticket.title} note={f.ticket.note}>
      <dl className="flex flex-col gap-2.5">
        <TicketRow
          label={f.ticket.perTab}
          now={report.current.tabs > 0 ? report.current.averageTicketCents : null}
          before={report.compared.tabs > 0 ? report.compared.averageTicketCents : null}
        />
        <TicketRow
          label={f.ticket.rodizio}
          now={full.rodizio.averageCents}
          before={full.comparedRodizio.averageCents}
        />
        <TicketRow label={f.ticket.people} now={full.people.averageCents} before={full.comparedPeople.averageCents} />
      </dl>
    </ChartCard>
  );
}

/** Pagamentos adiantados e cancelamentos por motivo (REL-11, 12). */
export function PaymentsCard({ report, full }: { report: SalesReport; full: Full }) {
  const p = f.payments;
  const revenue = report.current.revenueCents;
  return (
    <ChartCard title={p.title}>
      <dl className="flex flex-col gap-2 text-sm">
        <div className="flex items-center gap-3">
          <dt className="mr-auto min-w-0">{p.advance}</dt>
          <dd className="shrink-0 font-mono whitespace-nowrap tabular-nums">
            {p.advanceValue(full.advanceTabs, formatCents(full.advanceCents))}
          </dd>
        </div>
        <div className="flex items-center gap-3">
          <dt className="mr-auto min-w-0">{p.canceled}</dt>
          <dd className="shrink-0 font-mono whitespace-nowrap tabular-nums">
            {revenue > 0
              ? p.canceledValue(formatCents(full.canceledCents), share.format(full.canceledCents / revenue))
              : formatCents(full.canceledCents)}
          </dd>
        </div>
      </dl>
      {full.reasons.length > 0 ? (
        <BarList
          rows={full.reasons.map((r, i) => ({
            key: r.reason ?? `outros-${i}`,
            label: r.reason ?? p.others,
            value: r.amountCents,
          }))}
        />
      ) : (
        <NoData text={p.noCanceled} height={80} />
      )}
    </ChartCard>
  );
}
