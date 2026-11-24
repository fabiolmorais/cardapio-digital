import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { BarList, ChartCard, NoData, SplitBar } from "./charts";
import { formatDay } from "./period";
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

type Interest = Full["interest"];
type Seen = Interest["viewed"][number];

/** A nota dos blocos do celular: desde quando grava e, depois, o que é "visto" e "pediu". */
function interestNote(interest: Interest, periodFrom: string): string {
  if (interest.recordingSince && interest.recordingSince > periodFrom) {
    return `${m.interest.since(formatDay(interest.recordingSince))} ${m.interest.how}`;
  }
  return m.interest.how;
}

/** Cada produto: a barra de quem viu, com a parte de quem pediu, e os números. */
function SeenList({ rows }: { rows: Seen[] }) {
  const max = Math.max(...rows.map((r) => r.viewers));
  return (
    <div className="flex flex-col gap-3">
      <div aria-hidden className="flex gap-4 text-xs text-muted-foreground">
        <span className="flex items-center gap-1.5">
          <span className="size-2.5 rounded-sm bg-report-now" />
          {m.interest.ordered}
        </span>
        <span className="flex items-center gap-1.5">
          <span className="size-2.5 rounded-sm bg-report-prev" />
          {m.interest.onlyViewed}
        </span>
      </div>
      <ul className="flex flex-col gap-2.5">
        {rows.map((row) => {
          const conversion = percent.format(row.viewers === 0 ? 0 : row.ordered / row.viewers);
          return (
            <li
              key={row.name}
              title={m.interest.chart(row.name, row.viewers, row.ordered, conversion)}
              className="flex flex-col gap-1.5 text-sm"
            >
              <span className="flex items-baseline gap-3">
                <span className="min-w-0 flex-1 truncate">{row.name}</span>
                <span className="text-xs whitespace-nowrap text-muted-foreground tabular-nums">
                  <span aria-hidden>{m.interest.row(row.viewers, row.ordered, conversion)}</span>
                  <span className="sr-only">{m.interest.chart(row.name, row.viewers, row.ordered, conversion)}</span>
                </span>
              </span>
              <span aria-hidden className="flex h-2.5 gap-0.5">
                {row.ordered > 0 && (
                  <span className="h-2.5 rounded-l bg-report-now" style={{ width: `${(row.ordered / max) * 100}%` }} />
                )}
                {row.viewers > row.ordered && (
                  <span
                    className="h-2.5 rounded-r bg-report-prev"
                    style={{ width: `${((row.viewers - row.ordered) / max) * 100}%` }}
                  />
                )}
              </span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

/** Visto x pedido (REL-27): de quem abriu o prato pelo celular, quantos pediram. */
export function ViewedCard({ full, periodFrom, className }: { full: Full; periodFrom: string; className?: string }) {
  const interest = full.interest;
  return (
    <ChartCard
      title={m.viewed.title}
      hint={m.viewed.hint}
      className={className}
      note={interest.recordingSince ? interestNote(interest, periodFrom) : undefined}
    >
      {!interest.recordingSince ? (
        <NoData text={m.interest.notYet} height={120} />
      ) : interest.viewed.length === 0 ? (
        <NoData text={m.viewed.empty} height={120} />
      ) : (
        <SeenList rows={interest.viewed} />
      )}
    </ChartCard>
  );
}

/** Os reels mais vistos (REL-28) e quantos viraram pedido. */
export function ReelsCard({ full }: { full: Full }) {
  const interest = full.interest;
  return (
    <ChartCard title={m.reels.title} hint={m.reels.hint}>
      {!interest.recordingSince ? (
        <NoData text={m.interest.notYet} height={120} />
      ) : interest.reels.length === 0 ? (
        <NoData text={m.reels.empty} height={120} />
      ) : (
        <SeenList rows={interest.reels} />
      )}
    </ChartCard>
  );
}

/** Os carrinhos não enviados (REL-29): quantos, o valor e os itens que mais ficam. */
export function CartsCard({ full }: { full: Full }) {
  const interest = full.interest;
  return (
    <ChartCard title={m.carts.title} note={interest.cartSessions > 0 ? m.carts.leftHint : undefined}>
      {!interest.recordingSince ? (
        <NoData text={m.interest.notYet} height={120} />
      ) : interest.cartSessions === 0 ? (
        <NoData text={m.carts.empty} height={120} />
      ) : (
        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-0.5">
            <span className="flex items-baseline gap-2">
              <span className="font-mono text-3xl font-bold tabular-nums">{interest.cartsNotSent}</span>
              <span className="text-sm">{m.carts.notSent(interest.cartsNotSent)}</span>
            </span>
            <span className="text-xs text-muted-foreground">{m.carts.of(interest.cartSessions)}</span>
          </div>
          <p className="text-sm font-semibold">{m.carts.left(formatCents(interest.leftCents))}</p>
          {interest.mostLeft.length > 0 && (
            <div className="flex flex-col gap-2">
              <h3 className="text-xs font-semibold text-muted-foreground">{m.carts.mostLeft}</h3>
              <ul className="flex flex-col gap-2">
                {interest.mostLeft.map((item) => (
                  <li key={item.name} className="flex items-baseline gap-3 text-sm">
                    <span className="min-w-0 flex-1 truncate">{item.name}</span>
                    <span className="text-xs whitespace-nowrap text-muted-foreground tabular-nums">
                      {m.carts.item(item.sessions, item.units)}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </ChartCard>
  );
}
