import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { BarList, ChartCard, NoData, SplitBar } from "./charts";
import type { MenuReport } from "./reports-api";

const m = messages.reports.menu;

type Full = NonNullable<MenuReport["full"]>;

const percent = new Intl.NumberFormat("pt-BR", { style: "percent", maximumFractionDigits: 0 });
const share = new Intl.NumberFormat("pt-BR", { style: "percent", maximumFractionDigits: 1 });
const onePlace = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 });

/** Os mais cancelados (REL-23), com o motivo que mais se repete. */
export function CanceledCard({ full }: { full: Full }) {
  return (
    <ChartCard title={m.canceled.title}>
      {full.mostCanceled.length === 0 ? (
        <NoData text={m.canceled.empty} height={120} />
      ) : (
        <ul className="flex flex-col gap-3">
          {full.mostCanceled.map((item) => (
            <li key={item.name} className="flex items-baseline gap-3 text-sm">
              <span className="flex min-w-0 flex-1 flex-col">
                <span className="truncate">{item.name}</span>
                {item.reason && item.reasonTimes != null && (
                  <span className="text-xs text-muted-foreground">
                    {m.canceled.reason(item.reasonTimes, item.times, item.reason)}
                  </span>
                )}
              </span>
              <span className="font-mono tabular-nums">{item.times}</span>
            </li>
          ))}
        </ul>
      )}
    </ChartCard>
  );
}

/** Os tamanhos mais escolhidos (REL-18). */
export function SizesCard({ full }: { full: Full }) {
  const colors = ["var(--report-now)", "var(--report-prev)", "var(--report-card)", "var(--report-cash)"];
  return (
    <ChartCard title={m.sizes.title}>
      {full.sizes.length === 0 ? (
        <NoData text={m.sizes.empty} height={120} />
      ) : (
        <div className="flex flex-col gap-4">
          {full.sizes.map((product) => {
            const total = product.sizes.reduce((sum, s) => sum + s.quantity, 0);
            const parts = product.sizes.map((s) => m.sizes.part(s.name, percent.format(s.quantity / total)));
            return (
              <div key={product.product} className="flex flex-col gap-1.5">
                <span className="text-sm">{product.product}</span>
                <SplitBar
                  label={m.sizes.chart(product.product, parts.join(", "))}
                  segments={product.sizes.map((s, i) => ({
                    key: s.name,
                    label: s.name,
                    value: s.quantity,
                    color: colors[i % colors.length],
                  }))}
                />
                <span className="text-xs text-muted-foreground">{parts.join(" · ")}</span>
              </div>
            );
          })}
        </div>
      )}
    </ChartCard>
  );
}

/** As opções e os adicionais mais escolhidos (REL-19). */
export function OptionsCard({ full, itemsRevenueCents }: { full: Full; itemsRevenueCents: number }) {
  return (
    <ChartCard
      title={m.options.title}
      note={
        full.paidOptionsCents > 0 && itemsRevenueCents > 0
          ? m.options.note(formatCents(full.paidOptionsCents), share.format(full.paidOptionsCents / itemsRevenueCents))
          : undefined
      }
    >
      {full.options.length === 0 ? (
        <NoData text={m.options.empty} height={120} />
      ) : (
        <ul className="flex flex-col gap-2.5">
          {full.options.map((option) => (
            <li key={`${option.group}-${option.name}`} className="flex items-baseline gap-3 text-sm">
              <span className="min-w-0 flex-1 truncate">{option.name}</span>
              <span className="text-xs text-muted-foreground tabular-nums">{m.options.times(option.times)}</span>
              <span className="w-24 text-right font-mono whitespace-nowrap tabular-nums">
                {option.chargedCents > 0
                  ? formatCents(option.chargedCents)
                  : option.priced
                    ? m.options.free
                    : m.options.noPrice}
              </span>
            </li>
          ))}
        </ul>
      )}
    </ChartCard>
  );
}

/** Os itens mais pedidos juntos (REL-21). */
export function PairsCard({ full }: { full: Full }) {
  return (
    <ChartCard title={m.pairs.title} hint={m.pairs.hint} note={full.pairs.length > 0 ? m.pairs.note : undefined}>
      {full.pairs.length === 0 ? (
        <NoData text={m.pairs.empty} height={120} />
      ) : (
        <ul className="flex flex-col gap-2.5">
          {full.pairs.map((pair) => (
            <li key={`${pair.first}-${pair.second}`} className="flex items-baseline gap-3 text-sm">
              <span className="min-w-0 flex-1">{m.pairs.pair(pair.first, pair.second)}</span>
              <span className="whitespace-nowrap tabular-nums">
                {m.pairs.share(percent.format(pair.firstTabs === 0 ? 0 : pair.together / pair.firstTabs))}
              </span>
            </li>
          ))}
        </ul>
      )}
    </ChartCard>
  );
}

/** Com vídeo x sem vídeo (REL-26): vendidos por produto ativo. */
export function VideoCard({ full }: { full: Full }) {
  const v = full.video;
  const withVideo = v.withVideo === 0 ? 0 : v.withVideoSold / v.withVideo;
  const without = v.withoutVideo === 0 ? 0 : v.withoutVideoSold / v.withoutVideo;
  const rows = [
    ...(v.withVideo > 0 ? [{ key: "with", label: m.video.with(v.withVideo), value: withVideo }] : []),
    ...(v.withoutVideo > 0 ? [{ key: "without", label: m.video.without(v.withoutVideo), value: without }] : []),
  ];
  return (
    <ChartCard title={m.video.title} note={rows.length > 0 ? m.video.note : undefined}>
      {rows.length === 0 ? (
        <NoData text={m.video.empty} height={120} />
      ) : (
        <BarList format={(value) => m.video.perItem(onePlace.format(value))} rows={rows} />
      )}
    </ChartCard>
  );
}
