"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { TriangleAlert } from "lucide-react";
import Link from "next/link";
import { useEffect } from "react";
import { Button, buttonVariants } from "@/components/ui/button";
import { subscribeStaffEvents } from "@/lib/realtime/shared-staff-events";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { fetchRatingAlerts, RATING_ALERTS_KEY, RATINGS_KEY, resolveRating, type Rating } from "./ratings-api";

const t = messages.panel.ratings;

/**
 * O alerta de nota baixa no "Ao vivo" (RF40, desenho 7.5), só para dono e gerente: cada nota 1 ou 2
 * de hoje que ninguém resolveu, com o comentário, o garçom e "Já resolvi". Chega na hora pelo tempo
 * real (o aviso "rating").
 */
export function LowRatingAlerts() {
  const queryClient = useQueryClient();
  const alerts = useQuery({ queryKey: RATING_ALERTS_KEY, queryFn: fetchRatingAlerts, retry: false });
  useEffect(
    () =>
      subscribeStaffEvents(
        (event) => {
          if (event.name === "rating") void queryClient.invalidateQueries({ queryKey: RATING_ALERTS_KEY });
        },
        () => {},
      ),
    [queryClient],
  );
  const resolve = useMutation({
    mutationFn: resolveRating,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: RATING_ALERTS_KEY });
      void queryClient.invalidateQueries({ queryKey: RATINGS_KEY });
    },
  });

  const open = alerts.data ?? [];
  if (open.length === 0) return null;
  return (
    <section aria-label={t.alertsLabel} className="flex flex-col gap-3">
      {open.map((rating) => (
        <Alert
          key={rating.id}
          rating={rating}
          busy={resolve.isPending && resolve.variables === rating.id}
          onResolve={() => resolve.mutate(rating.id)}
        />
      ))}
    </section>
  );
}

function Alert({ rating, busy, onResolve }: { rating: Rating; busy: boolean; onResolve: () => void }) {
  const tags = rating.tags.map((tag) => t.tags[tag]).join(", ");
  return (
    <article
      role="alert"
      className="flex flex-wrap items-center gap-4 rounded-[20px] border border-destructive/45 bg-destructive/10 px-5 py-4"
    >
      <span className="flex size-12 shrink-0 items-center justify-center rounded-[14px] bg-destructive/15 text-destructive">
        <TriangleAlert aria-hidden className="size-6" />
      </span>
      <div className="flex min-w-0 flex-[1_1_320px] flex-col gap-1">
        <span className="font-semibold">{t.alertTitle(rating.stars, rating.tableLabel, rating.waiterName)}</span>
        {(rating.comment || tags) && (
          <span className="text-sm text-muted-foreground">
            {rating.comment ? `“${rating.comment}”` : ""}
            {rating.comment && tags ? " · " : ""}
            {tags ? t.alertTags(tags) : ""}
          </span>
        )}
      </div>
      <div className="flex flex-wrap gap-2">
        <Link href="/painel/mesas" className={cn(buttonVariants(), "h-11")}>
          {t.seeTable(rating.tableLabel)}
        </Link>
        <Button variant="ghost" className="h-11" disabled={busy} onClick={onResolve}>
          {busy ? t.resolving : t.resolve}
        </Button>
      </div>
    </article>
  );
}
