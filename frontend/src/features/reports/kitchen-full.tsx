import { messages } from "@/messages/pt-BR";
import { BarList, ChartCard, ColumnChart, KpiTile, NoData } from "./charts";
import { formatDuration, timeVariation } from "./durations";
import { formatDayMonth, formatWeekday } from "./period";
import type { KitchenReport } from "./reports-api";

const k = messages.reports.kitchen;
const f = k.full;
const t = messages.reports;

type Full = NonNullable<KitchenReport["full"]>;

const onePlace = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 });

const capitalize = (text: string) => text.charAt(0).toUpperCase() + text.slice(1);

/** Os 3 números do Business (REL-31, 32, 35), depois do preparo médio. */
export function KitchenFullTiles({ full }: { full: Full }) {
  const now = full.current;
  const before = full.compared;
  const time = (seconds: number | null | undefined) => (seconds == null ? "—" : formatDuration(seconds));
  return (
    <>
      <KpiTile
        label={f.passe}
        value={time(now.passeSeconds)}
        delta={
          now.passeSeconds != null && before.passeSeconds != null
            ? timeVariation(now.passeSeconds, before.passeSeconds)
            : null
        }
        hint={now.passeSeconds == null ? f.noDeliveries : t.delta.none}
      />
      <KpiTile
        label={f.toTable}
        value={time(now.toTableSeconds)}
        delta={
          now.toTableSeconds != null && before.toTableSeconds != null
            ? timeVariation(now.toTableSeconds, before.toTableSeconds)
            : null
        }
        hint={now.toTableSeconds == null ? f.noDeliveries : t.delta.none}
      />
      <KpiTile label={f.confirmation} value={f.orders(now.lateConfirmations)} hint={f.confirmationHint} />
    </>
  );
}

/** Do pedido à mesa, por hora (REL-32): o preparo e, em cima, a espera no passe. */
export function ByHourCard({ full }: { full: Full }) {
  const hours = full.byHour;
  if (hours.length === 0) {
    return (
      <ChartCard title={f.byHour}>
        <NoData text={f.byHourEmpty} height={220} />
      </ChartCard>
    );
  }
  const first = hours[0].hour;
  const last = hours[hours.length - 1].hour;
  const all = Array.from({ length: last - first + 1 }, (_, i) => {
    const hour = first + i;
    return hours.find((h) => h.hour === hour) ?? { hour, prepSeconds: 0, passeSeconds: 0, items: 0 };
  });
  const worst = all.reduce((a, b) => (b.prepSeconds + b.passeSeconds > a.prepSeconds + a.passeSeconds ? b : a), all[0]);
  return (
    <ChartCard title={f.byHour} hint={f.byHourHint}>
      <ColumnChart
        stacked
        unit={60}
        tickFormat={(minutes) => onePlace.format(minutes)}
        legend={{ now: f.legend.prep, before: f.legend.passe }}
        label={f.hourChart(
          worst.hour,
          formatDuration(worst.prepSeconds + worst.passeSeconds),
          formatDuration(worst.passeSeconds),
        )}
        columns={all.map((h) => ({
          key: String(h.hour),
          label: `${h.hour}h`,
          value: h.prepSeconds,
          previous: h.passeSeconds,
          tip: f.hourTip(
            h.hour,
            formatDuration(h.prepSeconds + h.passeSeconds),
            formatDuration(h.prepSeconds),
            formatDuration(h.passeSeconds),
          ),
        }))}
      />
    </ChartCard>
  );
}

/** Itens em preparo ao mesmo tempo (REL-34): a média do pico de cada hora. */
export function PeaksCard({ full }: { full: Full }) {
  const peaks = full.peaks;
  if (peaks.length === 0) {
    return (
      <ChartCard title={f.peaks} hint={f.peaksHint}>
        <NoData text={k.noPreparations} height={180} />
      </ChartCard>
    );
  }
  const first = peaks[0].hour;
  const last = peaks[peaks.length - 1].hour;
  const all = Array.from({ length: last - first + 1 }, (_, i) => {
    const hour = first + i;
    return peaks.find((p) => p.hour === hour) ?? { hour, average: 0 };
  });
  const top = all.reduce((a, b) => (b.average > a.average ? b : a), all[0]);
  return (
    <ChartCard title={f.peaks} hint={f.peaksHint} note={f.peaksNote}>
      <ColumnChart
        height={180}
        unit={1}
        tickFormat={(value) => onePlace.format(value)}
        label={f.peakChart(top.hour, onePlace.format(top.average))}
        columns={all.map((p) => ({
          key: String(p.hour),
          label: `${p.hour}h`,
          value: p.average,
          tip: f.peakTip(p.hour, onePlace.format(p.average)),
        }))}
      />
    </ChartCard>
  );
}

/** Preparo por categoria (REL-36). */
export function CategoryPrepCard({ full }: { full: Full }) {
  return (
    <ChartCard title={f.byCategory}>
      {full.byCategory.length === 0 ? (
        <NoData text={k.noPreparations} height={140} />
      ) : (
        <BarList
          format={formatDuration}
          rows={full.byCategory.map((c) => ({ key: c.name, label: c.name, value: c.averageSeconds }))}
        />
      )}
    </ChartCard>
  );
}

/** Prontos que demoraram a chegar à mesa (REL-37): as horas com mais itens parados no passe. */
export function DelayedCard({ full }: { full: Full }) {
  return (
    <ChartCard title={f.delayed} hint={f.delayedHint}>
      {full.delayed.length === 0 ? (
        <NoData text={f.delayedEmpty} height={140} />
      ) : (
        <ul className="flex flex-col gap-2.5">
          {full.delayed.map((slot) => (
            <li key={`${slot.day}-${slot.hour}`} className="flex items-baseline gap-3 text-sm">
              <span className="min-w-0 flex-1">
                {capitalize(f.delayedDay(formatWeekday(slot.day), formatDayMonth(slot.day), slot.hour))}
              </span>
              <span className="text-xs text-muted-foreground">{f.delayedItems(slot.items)}</span>
              <span className="font-mono whitespace-nowrap tabular-nums">
                {f.delayedWait(formatDuration(slot.averageWaitSeconds))}
              </span>
            </li>
          ))}
        </ul>
      )}
    </ChartCard>
  );
}
