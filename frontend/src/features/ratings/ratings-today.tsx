"use client";

import { useInfiniteQuery, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { subscribeStaffEvents } from "@/lib/realtime/shared-staff-events";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { RatingStars } from "./rating-stars";
import { fetchTodayRatings, listTodayRatings, RATINGS_KEY, type Rating } from "./ratings-api";

const t = messages.panel.ratings;
const PAGE = 20;
const average = new Intl.NumberFormat("pt-BR", { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const time = new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit" });

/**
 * As avaliações de hoje (RF40, desenho 7.6), para dono e gerente: a nota média, quantas, as notas
 * baixas e quem foi ao Google; depois a lista, da mais nova, com o filtro "Notas baixas". Atualiza
 * sozinha quando chega uma nota baixa.
 */
export function RatingsToday() {
  const queryClient = useQueryClient();
  const { status } = useRequiredSession();
  const ready = status === "authenticated";
  const [onlyLow, setOnlyLow] = useState(false);
  const summary = useQuery({
    queryKey: [...RATINGS_KEY, "today"],
    queryFn: fetchTodayRatings,
    retry: false,
    enabled: ready,
  });
  const list = useInfiniteQuery({
    queryKey: [...RATINGS_KEY, "list", onlyLow],
    queryFn: ({ pageParam }) => listTodayRatings(onlyLow, pageParam, PAGE),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
    retry: false,
    enabled: ready,
  });
  useEffect(
    () =>
      subscribeStaffEvents(
        (event) => {
          if (event.name === "rating") void queryClient.invalidateQueries({ queryKey: RATINGS_KEY });
        },
        () => {},
      ),
    [queryClient],
  );

  const s = summary.data;
  const ratings = list.data?.pages.flatMap((page) => page.content) ?? [];

  return (
    <>
      <header className="flex flex-wrap items-center gap-3 border-b border-border px-4 py-4 sm:px-7">
        <div className="mr-auto flex flex-col gap-0.5">
          <h1 className="text-[28px] leading-[34px] font-bold tracking-tight">{t.title}</h1>
          <span className="text-[13px] text-muted-foreground">{t.subtitle}</span>
        </div>
        <div
          role="group"
          aria-label={t.filter}
          className="flex gap-1.5 rounded-[14px] border border-border bg-card p-1"
        >
          {[false, true].map((low) => (
            <button
              key={String(low)}
              type="button"
              aria-pressed={onlyLow === low}
              onClick={() => setOnlyLow(low)}
              className={cn(
                "h-9 rounded-[10px] px-3 text-[13px] font-semibold text-muted-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                onlyLow === low && "bg-foreground text-background",
              )}
            >
              {low ? t.onlyLow(s?.low ?? 0) : t.all(s?.count ?? 0)}
            </button>
          ))}
        </div>
      </header>

      <div className="flex flex-col gap-5 px-4 py-6 sm:px-7">
        {summary.isError || list.isError ? (
          <div className="flex flex-col items-start gap-3">
            <p role="alert" className="text-destructive">
              {t.error}
            </p>
            <Button variant="outline" onClick={() => void queryClient.invalidateQueries({ queryKey: RATINGS_KEY })}>
              {t.retry}
            </Button>
          </div>
        ) : (
          <>
            <section aria-label={t.title} className="grid grid-cols-2 gap-3 lg:grid-cols-4">
              <Kpi label={t.average}>
                {s?.average != null ? average.format(s.average) : "–"}{" "}
                <span className="text-sm font-normal text-muted-foreground">{t.of5}</span>
              </Kpi>
              <Kpi label={t.count}>{s ? s.count : "–"}</Kpi>
              <Kpi label={t.low} className={s && s.low > 0 ? "text-destructive" : undefined}>
                {s ? s.low : "–"}
              </Kpi>
              <Kpi label={t.google} hint={s && s.high > 0 ? t.googleOf(s.high) : undefined}>
                {s ? s.googleClicks : "–"}
              </Kpi>
            </section>

            <section aria-label={t.count} className="flex flex-col gap-2.5">
              {!list.data ? (
                <p className="text-muted-foreground">{t.loading}</p>
              ) : ratings.length === 0 ? (
                <p className="rounded-[20px] border border-border bg-card px-5 py-8 text-center text-muted-foreground">
                  {onlyLow ? t.emptyLow : t.empty}
                </p>
              ) : (
                ratings.map((rating) => <RatingRow key={rating.id} rating={rating} />)
              )}
              {list.hasNextPage && (
                <Button
                  variant="outline"
                  className="self-center"
                  disabled={list.isFetchingNextPage}
                  onClick={() => void list.fetchNextPage()}
                >
                  {t.more}
                </Button>
              )}
            </section>
            <p className="text-[13px] text-muted-foreground">{t.privacy}</p>
          </>
        )}
      </div>
    </>
  );
}

function RatingRow({ rating }: { rating: Rating }) {
  const low = rating.stars <= 2;
  return (
    <article
      className={cn(
        "grid grid-cols-[auto_minmax(0,1fr)] gap-x-3.5 gap-y-1 rounded-2xl border border-border bg-card px-4.5 py-4 sm:grid-cols-[auto_minmax(0,1fr)_auto]",
        low && "border-destructive/45",
      )}
    >
      <span className="row-span-2 flex size-14 flex-col items-center justify-center rounded-xl bg-muted leading-none">
        <span className="text-[10px] font-semibold tracking-wide text-muted-foreground uppercase">{t.table}</span>
        <span className="text-lg font-bold">{rating.tableLabel}</span>
      </span>
      <div className="flex min-w-0 flex-wrap items-center gap-2.5">
        <RatingStars stars={rating.stars} />
        <span className="font-semibold">{rating.waiterName ?? t.noWaiter}</span>
        {rating.tags.map((tag) => (
          <span key={tag} className="rounded-full bg-muted px-2.5 py-0.5 text-xs font-semibold text-muted-foreground">
            {t.tags[tag]}
          </span>
        ))}
        {low && (
          <span className="rounded-full bg-destructive/15 px-2.5 py-0.5 text-xs font-semibold text-destructive">
            {rating.resolvedAt ? t.resolvedBadge : t.lowBadge}
          </span>
        )}
        {rating.google && (
          <span className="rounded-full bg-emerald-700/20 px-2.5 py-0.5 text-xs font-semibold text-emerald-400">
            {t.googleBadge}
          </span>
        )}
      </div>
      <span className="text-[13px] text-muted-foreground sm:row-span-2 sm:text-right">
        {time.format(new Date(rating.createdAt))}
      </span>
      {rating.comment && <p className="col-start-2 text-sm text-muted-foreground">“{rating.comment}”</p>}
    </article>
  );
}

function Kpi({
  label,
  hint,
  className,
  children,
}: {
  label: string;
  hint?: string;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-w-0 flex-col gap-1.5 rounded-[20px] border border-border bg-card px-[18px] py-4">
      <span className="text-[13px] text-muted-foreground">{label}</span>
      <span className={cn("text-[28px] leading-[34px] font-bold tracking-tight", className)}>{children}</span>
      {hint && <span className="text-[13px] text-muted-foreground/80">{hint}</span>}
    </div>
  );
}
