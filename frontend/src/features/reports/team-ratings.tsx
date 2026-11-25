"use client";

import { useInfiniteQuery } from "@tanstack/react-query";
import { Star } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { BarList, ChartCard, ColumnChart, KpiTile, NoData } from "./charts";
import { formatDayMonth } from "./period";
import { fetchTeamComments, type ReportParams, type TeamReport } from "./reports-api";

const m = messages.reports.team.ratings;
const PAGE = 10;

const onePlace = new Intl.NumberFormat("pt-BR", { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const share = new Intl.NumberFormat("pt-BR", { style: "percent", maximumFractionDigits: 0 });
const when = new Intl.DateTimeFormat("pt-BR", {
  weekday: "short",
  day: "2-digit",
  month: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
});

type Ratings = TeamReport["ratings"];
const ratio = (part: number, whole: number) => share.format(whole === 0 ? 0 : part / whole);
const note = (value: number | null | undefined) => (value == null ? null : onePlace.format(value));

/**
 * As avaliações do atendimento no período (desenho 14.16, REL-83 e 84): os números, a nota ao longo
 * do período, como as notas se dividem (e o almoço contra o jantar), os atalhos e os comentários.
 */
export function TeamRatings({ report, params }: { report: TeamReport; params: ReportParams }) {
  const r = report.ratings;
  return (
    <section aria-labelledby="ratings-title" className="flex flex-col gap-5">
      <div className="flex flex-wrap items-baseline gap-x-4 gap-y-1 pt-2">
        <h2 id="ratings-title" className="mr-auto text-[22px] leading-7 font-bold tracking-tight">
          {m.title}
        </h2>
        <span className="text-[13px] text-muted-foreground">{m.hint}</span>
      </div>
      {r.count === 0 ? (
        <ChartCard title={m.title}>
          <NoData text={m.empty} height={120} />
        </ChartCard>
      ) : (
        <>
          <Numbers r={r} />
          <div className="grid items-start gap-5 lg:grid-cols-2">
            <Trend r={r} />
            <Split r={r} />
          </div>
          <div className="grid items-start gap-5 lg:grid-cols-2">
            <Tags r={r} />
            <Comments params={params} />
          </div>
        </>
      )}
    </section>
  );
}

function Numbers({ r }: { r: Ratings }) {
  const average = r.average ?? 0;
  const delta =
    r.comparedAverage == null
      ? null
      : average > r.comparedAverage
        ? { text: m.better(onePlace.format(average - r.comparedAverage)), tone: "up" as const }
        : average < r.comparedAverage
          ? { text: m.worse(onePlace.format(r.comparedAverage - average)), tone: "down" as const }
          : { text: m.same, tone: "flat" as const };
  return (
    <div className="grid grid-cols-2 gap-3.5 lg:grid-cols-4">
      <KpiTile label={m.average} value={m.of5(onePlace.format(average))} delta={delta} hint={m.noCompare} />
      <KpiTile label={m.count} value={String(r.count)} hint={m.ofTabs(ratio(r.count, r.closedTabs))} />
      <KpiTile label={m.low} value={String(r.low)} hint={m.lowShare(ratio(r.low, r.count))} />
      <KpiTile
        label={m.google}
        value={String(r.googleClicks)}
        hint={m.googleOf(r.high, ratio(r.googleClicks, r.high))}
      />
    </div>
  );
}

/** A nota ao longo do período: uma coluna por dia (até 14 dias) ou por semana. */
function Trend({ r }: { r: Ratings }) {
  const points = r.trend;
  const weekly = points.some((p) => p.from !== p.to);
  const rated = points.filter((p) => p.average != null);
  const best = rated.reduce((a, b) => ((b.average ?? 0) > (a.average ?? 0) ? b : a), rated[0]);
  const worst = rated.reduce((a, b) => ((b.average ?? 5) < (a.average ?? 5) ? b : a), rated[0]);
  const label = (p: (typeof points)[number]) => formatDayMonth(p.from);
  return (
    <ChartCard title={m.trend} hint={weekly ? m.trendHintWeeks : m.trendHintDays}>
      <ColumnChart
        height={180}
        unit={1}
        top={5}
        tickFormat={(value) => String(value)}
        label={m.trendChart(note(best?.average) ?? "—", note(worst?.average) ?? "—")}
        columns={points.map((p) => ({
          key: p.from,
          label: label(p),
          value: p.average ?? 0,
          tip: m.trendTip(formatDayMonth(p.from), formatDayMonth(p.to), note(p.average), p.count),
        }))}
      />
    </ChartCard>
  );
}

/** Como as notas se dividem e o almoço contra o jantar. */
function Split({ r }: { r: Ratings }) {
  const [one, two, three, four, five] = r.byStars;
  const rows = [
    { key: "5", label: m.stars(5), value: five },
    { key: "4", label: m.stars(4), value: four },
    { key: "3", label: m.stars(3), value: three },
    { key: "low", label: m.lowStars, value: one + two },
  ];
  return (
    <ChartCard title={m.split}>
      <BarList format={(value) => m.starsValue(value, ratio(value, r.count))} rows={rows} />
      <div className="h-px bg-border" />
      <dl className="flex flex-col gap-2.5 text-sm">
        {[
          { label: m.lunch, group: r.lunch },
          { label: m.dinner, group: r.dinner },
        ].map(({ label, group }) => (
          <div key={label} className="flex items-baseline justify-between gap-3">
            <dt>{label}</dt>
            <dd className="font-mono text-[13px] tabular-nums">{m.shiftValue(note(group.average), group.count)}</dd>
          </div>
        ))}
      </dl>
    </ChartCard>
  );
}

/** Os atalhos: nos elogios (4 ou 5) e no que melhorar (1 a 3). */
function Tags({ r }: { r: Ratings }) {
  const list = (tags: Ratings["praisedTags"]) =>
    tags.filter((t) => t.times > 0).map((t) => ({ key: t.tag, label: m.tagNames[t.tag], value: t.times }));
  const praised = list(r.praisedTags);
  const improve = list(r.improveTags);
  return (
    <ChartCard title={m.tags}>
      <div className="grid gap-6 sm:grid-cols-2">
        {[
          { title: m.praised, rows: praised },
          { title: m.improve, rows: improve },
        ].map(({ title, rows }) => (
          <div key={title} className="flex flex-col gap-2.5">
            <h3 className="text-sm text-muted-foreground">{title}</h3>
            {rows.length === 0 ? (
              <p className="text-sm text-muted-foreground">{m.noTags}</p>
            ) : (
              <BarList format={(value) => String(value)} rows={rows} />
            )}
          </div>
        ))}
      </div>
    </ChartCard>
  );
}

/** Os comentários do período, do mais novo, com "Ver mais" e o filtro das notas baixas. */
function Comments({ params }: { params: ReportParams }) {
  const [onlyLow, setOnlyLow] = useState(false);
  const list = useInfiniteQuery({
    queryKey: ["reports", "team", "comments", params, onlyLow],
    queryFn: ({ pageParam }) => fetchTeamComments({ ...params, onlyLow, page: pageParam, size: PAGE }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
    retry: false,
  });
  const comments = list.data?.pages.flatMap((page) => page.content) ?? [];
  return (
    <ChartCard
      title={m.comments}
      note={m.commentsNote}
      action={
        <div
          role="group"
          aria-label={m.commentsFilter}
          className="flex gap-1 rounded-[12px] border border-border bg-background p-1"
        >
          {[false, true].map((low) => (
            <button
              key={String(low)}
              type="button"
              aria-pressed={onlyLow === low}
              onClick={() => setOnlyLow(low)}
              className={cn(
                "h-8 rounded-[8px] px-3 text-[13px] font-semibold text-muted-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                onlyLow === low && "bg-foreground text-background",
              )}
            >
              {low ? m.onlyLow : m.all}
            </button>
          ))}
        </div>
      }
    >
      {list.isError ? (
        <p role="alert" className="text-sm text-destructive">
          {m.commentsError}
        </p>
      ) : !list.data ? null : comments.length === 0 ? (
        <NoData text={onlyLow ? m.commentsEmptyLow : m.commentsEmpty} height={120} />
      ) : (
        <ul className="flex flex-col gap-2.5">
          {comments.map((c, i) => (
            <li
              key={`${c.createdAt}-${i}`}
              className={cn(
                "grid grid-cols-[auto_minmax(0,1fr)] gap-x-3.5 gap-y-1 rounded-[14px] border border-border bg-secondary/40 px-4 py-3",
                c.stars <= 2 && "border-destructive/45",
              )}
            >
              <span className="row-span-3 flex size-12 flex-col items-center justify-center rounded-xl bg-muted leading-none">
                <span className="text-[9px] font-semibold tracking-wide text-muted-foreground uppercase">
                  {m.table}
                </span>
                <span className="font-bold">{c.tableLabel}</span>
              </span>
              <span className="flex flex-wrap items-center gap-2 text-sm">
                <span role="img" aria-label={m.stars5(c.stars)} className="flex gap-0.5 text-amber-400">
                  {[1, 2, 3, 4, 5].map((n) => (
                    <Star key={n} aria-hidden className="size-3.5" fill={n <= c.stars ? "currentColor" : "none"} />
                  ))}
                </span>
                <span className="font-semibold">{c.waiterName ?? m.noWaiter}</span>
                {c.tags.map((tag) => (
                  <span
                    key={tag}
                    className="rounded-full bg-muted px-2 py-0.5 text-xs font-semibold text-muted-foreground"
                  >
                    {m.tagNames[tag]}
                  </span>
                ))}
              </span>
              <p className="text-sm text-muted-foreground">“{c.comment}”</p>
              <span className="text-xs text-muted-foreground first-letter:uppercase">
                {when.format(new Date(c.createdAt))}
              </span>
            </li>
          ))}
        </ul>
      )}
      {list.hasNextPage && (
        <Button
          variant="outline"
          className="self-center"
          disabled={list.isFetchingNextPage}
          onClick={() => void list.fetchNextPage()}
        >
          {m.more}
        </Button>
      )}
    </ChartCard>
  );
}
