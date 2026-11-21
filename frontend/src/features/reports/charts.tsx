"use client";

import { useState, type ReactNode } from "react";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";

const t = messages.reports;

/**
 * As peças dos relatórios (desenhos 14.1 e 14.2), em CSS, sem biblioteca de gráficos: o número com a
 * variação, as colunas com a comparação, a barra dividida, o mapa de calor e a lista de barras.
 * Toda peça diz em texto o que mostra (leitor de tela) e mostra o valor exato ao tocar ou passar o
 * mouse. As cores vêm do tema (--report-*): a cor marca a série; o texto fica nas cores de texto.
 */

/** Um bloco do relatório: título, um aviso curto ao lado e, embaixo, uma nota. */
export function ChartCard({
  title,
  hint,
  action,
  note,
  className,
  children,
}: {
  title: string;
  hint?: string;
  action?: ReactNode;
  note?: string;
  className?: string;
  children: ReactNode;
}) {
  return (
    <section
      aria-label={title}
      className={cn(
        "flex min-w-0 flex-col gap-3.5 rounded-[20px] border border-border bg-card px-5 py-[18px]",
        className,
      )}
    >
      <div className="flex flex-wrap items-baseline gap-x-4 gap-y-1.5">
        <h2 className="mr-auto text-base leading-[22px] font-semibold">{title}</h2>
        {hint && <span className="text-xs font-semibold text-muted-foreground">{hint}</span>}
        {action}
      </div>
      {children}
      {note && <p className="text-xs leading-4 text-muted-foreground">{note}</p>}
    </section>
  );
}

/** O espaço de um bloco sem dados: o título continua, para o dono saber o que vai aparecer ali. */
export function NoData({ text = t.noSales, height = 160 }: { text?: string; height?: number }) {
  return (
    <div
      style={{ height }}
      className="flex items-center justify-center rounded-[14px] border border-dashed border-input px-4 text-center text-sm text-muted-foreground"
    >
      {text}
    </div>
  );
}

const percent = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1, minimumFractionDigits: 1 });

/** A variação contra o período anterior: ▲ verde (subiu), ▼ vermelho (caiu), — igual. */
export function variation(current: number, previous: number) {
  if (previous === 0) return { text: t.delta.none, tone: "flat" as const };
  const change = ((current - previous) / previous) * 100;
  if (Math.abs(change) < 0.05) return { text: t.delta.same, tone: "flat" as const };
  const value = `${percent.format(Math.abs(change))}%`;
  return change > 0
    ? { text: t.delta.up(value), tone: "up" as const }
    : { text: t.delta.down(value), tone: "down" as const };
}

export type Delta = ReturnType<typeof variation>;

/**
 * Um número do período. Com `previous`, a variação percentual embaixo; com `delta`, uma variação já
 * pronta (os tempos, em que menos é melhor); senão, `hint`.
 */
export function KpiTile({
  label,
  value,
  current,
  previous,
  delta: given,
  hint,
}: {
  label: string;
  value: string;
  current?: number;
  previous?: number;
  delta?: Delta | null;
  hint?: string;
}) {
  const delta = given ?? (current !== undefined && previous !== undefined ? variation(current, previous) : null);
  return (
    <div className="@container flex min-w-0 flex-col gap-1.5 rounded-[20px] border border-border bg-card px-5 py-[18px]">
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className="text-[length:clamp(20px,14cqi,30px)] leading-9 font-bold tracking-tight whitespace-nowrap tabular-nums">
        {value}
      </span>
      {delta ? (
        <span
          className={cn(
            "text-[13px] font-semibold",
            delta.tone === "up" && "text-success",
            delta.tone === "down" && "text-destructive",
            delta.tone === "flat" && "text-muted-foreground",
          )}
        >
          {delta.text}
          {delta.tone !== "flat" && <span className="sr-only"> {t.delta.vsPrevious}</span>}
        </span>
      ) : (
        hint && <span className="text-[13px] text-muted-foreground">{hint}</span>
      )}
    </div>
  );
}

const compact = new Intl.NumberFormat("pt-BR", { notation: "compact", maximumFractionDigits: 1 });

/**
 * O eixo: um passo redondo (1, 2, 2,5 ou 5 × 10ⁿ) e quantos passos cabem até passar do maior valor,
 * de 3 a 5 linhas. Assim os rótulos ficam redondos ("3 mil", "2 mil"), e não "1,9 mil" ou "625".
 */
function axis(max: number) {
  if (max <= 0) return { top: 1, steps: [1, 0] };
  const rough = max / 4;
  const magnitude = 10 ** Math.floor(Math.log10(rough));
  const step = ([1, 2, 2.5, 5, 10].find((s) => s * magnitude >= rough) ?? 10) * magnitude;
  const count = Math.ceil(max / step);
  return { top: count * step, steps: Array.from({ length: count + 1 }, (_, i) => (count - i) * step) };
}

export type Column = {
  key: string;
  /** O rótulo embaixo da coluna (vazio para não amontoar). */
  label: string;
  /** Em centavos. */
  value: number;
  /** A comparação, em centavos (a segunda coluna, cinza). */
  previous?: number;
  /** O valor exato, dito ao tocar, passar o mouse ou pelo leitor de tela. */
  tip: string;
};

/**
 * Colunas no tempo (dias, semanas, horas), com a comparação ao lado quando vier. Cada coluna é um
 * botão: tocar, passar o mouse ou chegar pelo teclado mostra o valor na linha embaixo.
 */
export function ColumnChart({
  columns,
  label,
  legend,
  height = 220,
}: {
  columns: Column[];
  label: string;
  legend?: { now: string; before: string };
  height?: number;
}) {
  const [selected, setSelected] = useState<number | null>(null);
  // Em reais (os valores vêm em centavos).
  const { top: topReais, steps } = axis(Math.max(...columns.map((c) => Math.max(c.value, c.previous ?? 0))) / 100);
  const top = topReais * 100;
  const ticks = steps.map((v) => compact.format(v));
  const every = columns.length > 16 ? Math.ceil(columns.length / 10) : 1;
  const paired = legend !== undefined;
  const pick = (i: number) => setSelected(i);

  return (
    <div className="flex flex-col gap-2">
      {legend && (
        <div className="flex flex-wrap gap-4 text-[13px] text-muted-foreground">
          <span className="inline-flex items-center gap-1.5">
            <i aria-hidden className="size-2.5 rounded-[3px] bg-report-now" />
            {legend.now}
          </span>
          <span className="inline-flex items-center gap-1.5">
            <i aria-hidden className="size-2.5 rounded-[3px] bg-report-prev" />
            {legend.before}
          </span>
        </div>
      )}
      <div role="group" aria-label={label} className="relative flex" style={{ height }}>
        <div
          aria-hidden
          className="flex w-11 shrink-0 -translate-y-[7px] flex-col justify-between pr-2 text-right text-[11px] text-muted-foreground"
        >
          {ticks.map((tick, i) => (
            <span key={i}>{tick}</span>
          ))}
        </div>
        <div className="relative min-w-0 flex-1 border-b border-input">
          <div aria-hidden className="pointer-events-none absolute inset-0 flex flex-col justify-between">
            {ticks.map((_, i) => (
              <i key={i} className="block border-t border-border" />
            ))}
          </div>
          <div
            className="absolute inset-0 grid items-end"
            style={{
              gridTemplateColumns: `repeat(${columns.length}, minmax(0, 1fr))`,
              gap: columns.length > 20 ? 3 : 8,
            }}
          >
            {columns.map((column, i) => (
              <button
                key={column.key}
                type="button"
                aria-label={column.tip}
                aria-pressed={selected === i}
                title={column.tip}
                onClick={() => pick(i)}
                onMouseEnter={() => pick(i)}
                onFocus={() => pick(i)}
                className={cn(
                  "group flex h-full min-w-0 items-end justify-center gap-0.5 rounded-t-md focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                  selected === i && "bg-foreground/5",
                )}
              >
                <span
                  className={cn(
                    "w-full rounded-t bg-report-now group-hover:brightness-110",
                    paired ? "max-w-[22px]" : "max-w-[26px]",
                  )}
                  style={{ height: `${(column.value / top) * 100}%`, minHeight: column.value > 0 ? 2 : 0 }}
                />
                {paired && (
                  <span
                    className="w-full max-w-[22px] rounded-t bg-report-prev"
                    style={{ height: `${((column.previous ?? 0) / top) * 100}%`, minHeight: column.previous ? 2 : 0 }}
                  />
                )}
              </button>
            ))}
          </div>
        </div>
      </div>
      <div
        aria-hidden
        className="ml-11 grid text-center text-xs text-muted-foreground"
        style={{ gridTemplateColumns: `repeat(${columns.length}, minmax(0, 1fr))`, gap: columns.length > 20 ? 3 : 8 }}
      >
        {columns.map((column, i) => (
          <span key={column.key} className="flex justify-center overflow-visible whitespace-nowrap">
            {i % every === 0 ? column.label : ""}
          </span>
        ))}
      </div>
      <p role="status" className="min-h-4 text-xs leading-4 text-muted-foreground">
        {selected !== null && columns[selected] ? columns[selected].tip : t.tapHint}
      </p>
    </div>
  );
}

export type Segment = { key: string; label: string; value: number; color: string };

/** Uma barra dividida em partes (forma de pagamento), com a legenda em linhas: cor, nome, % e valor. */
export function SplitBar({ segments, label }: { segments: Segment[]; label: string }) {
  const total = segments.reduce((sum, s) => sum + s.value, 0);
  const share = (value: number) => (total === 0 ? 0 : Math.round((value / total) * 100));
  return (
    <div className="flex flex-col gap-3.5">
      <div role="img" aria-label={label} className="flex h-4 gap-0.5">
        {segments
          .filter((s) => s.value > 0)
          .map((s) => (
            <i
              key={s.key}
              title={`${s.label} · ${formatCents(s.value)} · ${share(s.value)}%`}
              className="block h-full first:rounded-l last:rounded-r"
              style={{ width: `${(s.value / total) * 100}%`, background: s.color }}
            />
          ))}
      </div>
      <ul className="flex flex-col gap-2.5">
        {segments.map((s) => (
          <li key={s.key} className="grid grid-cols-[minmax(0,1fr)_auto_auto] items-center gap-3 text-sm">
            <span className="inline-flex items-center gap-2">
              <i aria-hidden className="size-2.5 shrink-0 rounded-[3px]" style={{ background: s.color }} />
              {s.label}
            </span>
            <span className="text-muted-foreground tabular-nums">{share(s.value)}%</span>
            <span className="min-w-24 text-right font-mono text-[13px] tabular-nums">{formatCents(s.value)}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

export type HeatCell = { weekday: number; hour: number; averageCents: number };

/** A força da cor em cinco degraus, pela fração do maior valor (como a escala "menos … mais"). */
const STEPS = [0.14, 0.32, 0.52, 0.74, 1];
function strength(value: number, max: number) {
  if (value <= 0 || max <= 0) return 0;
  return STEPS[Math.min(4, Math.floor((value / max) * 5 - 1e-9))];
}

/** O mapa de calor: dias da semana × horas em que houve venda, com a escala "menos … mais". */
export function Heatmap({ cells }: { cells: HeatCell[] }) {
  const h = t.heatmap;
  const hours = cells.map((c) => c.hour);
  const first = Math.min(...hours);
  const last = Math.max(...hours);
  const range = Array.from({ length: last - first + 1 }, (_, i) => first + i);
  const max = Math.max(...cells.map((c) => c.averageCents));
  const value = (weekday: number, hour: number) =>
    cells.find((c) => c.weekday === weekday && c.hour === hour)?.averageCents ?? 0;
  const peaks = [...cells]
    .sort((a, b) => b.averageCents - a.averageCents)
    .slice(0, 3)
    .map((c) => `${h.weekdayNames[c.weekday - 1]} às ${c.hour}h`)
    .join(", ");
  const every = range.length > 8 ? 2 : 1;

  return (
    <div className="flex flex-col gap-3">
      <div aria-hidden className="flex items-center justify-end gap-2 text-xs text-muted-foreground">
        {h.less}
        <span className="flex gap-0.5">
          {STEPS.map((o) => (
            <b key={o} className="h-2.5 w-[18px] rounded-sm bg-report-now" style={{ opacity: o }} />
          ))}
        </span>
        {h.more}
      </div>
      <div
        role="img"
        aria-label={h.chart(peaks)}
        className="grid gap-[3px] text-[11px] text-muted-foreground"
        style={{ gridTemplateColumns: `36px repeat(${range.length}, minmax(0, 1fr))` }}
      >
        <span />
        {range.map((hour, i) => (
          <span key={hour} className="pb-0.5 text-center">
            {i % every === 0 ? `${hour}h` : ""}
          </span>
        ))}
        {h.weekdays.map((day, w) => (
          <Row key={day} day={day} weekday={w + 1} range={range} value={value} max={max} />
        ))}
      </div>
    </div>
  );
}

function Row({
  day,
  weekday,
  range,
  value,
  max,
}: {
  day: string;
  weekday: number;
  range: number[];
  value: (weekday: number, hour: number) => number;
  max: number;
}) {
  return (
    <>
      <span className="flex items-center">{day}</span>
      {range.map((hour) => {
        const v = value(weekday, hour);
        return (
          <span
            key={hour}
            title={t.heatmap.cell(t.heatmap.weekdayNames[weekday - 1], hour, formatCents(v))}
            className={cn("h-7 rounded", v > 0 ? "bg-report-now" : "bg-foreground/5")}
            style={v > 0 ? { opacity: strength(v, max) } : undefined}
          />
        );
      })}
    </>
  );
}

export type BarRow = { key: string; label: string; value: number; detail?: string };

/** Barras deitadas, do maior para o menor (itens mais vendidos): nome, barra e valor. */
export function BarList({ rows }: { rows: BarRow[] }) {
  const max = Math.max(...rows.map((r) => r.value));
  return (
    <ul className="flex flex-col gap-2.5">
      {rows.map((row) => (
        <li
          key={row.key}
          title={row.detail ? `${row.label}: ${formatCents(row.value)} · ${row.detail}` : undefined}
          className="grid grid-cols-[minmax(110px,1.2fr)_minmax(0,2fr)_auto] items-center gap-3 text-sm max-sm:grid-cols-[minmax(0,1fr)_auto]"
        >
          <span className="truncate">{row.label}</span>
          <span aria-hidden className="flex h-3 max-sm:order-last max-sm:col-span-2">
            <span className="h-3 rounded-r bg-report-now" style={{ width: `${(row.value / max) * 100}%` }} />
          </span>
          <span className="text-right font-mono text-[13px] tabular-nums">
            {formatCents(row.value)}
            {row.detail && <span className="sr-only"> · {row.detail}</span>}
          </span>
        </li>
      ))}
    </ul>
  );
}
