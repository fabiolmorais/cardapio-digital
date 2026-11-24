"use client";

import { useQuery } from "@tanstack/react-query";
import { Star } from "lucide-react";
import { messages } from "@/messages/pt-BR";
import { fetchMyRating } from "./ratings-api";

const t = messages.panel.ratings;
const average = new Intl.NumberFormat("pt-BR", { minimumFractionDigits: 1, maximumFractionDigits: 1 });

/** A nota do garçom nos últimos 30 dias, no "Ao vivo", quando o restaurante mostra (RF40). */
export function MyRating() {
  const mine = useQuery({ queryKey: ["my-rating"], queryFn: fetchMyRating, retry: false, staleTime: 60_000 });
  if (!mine.data?.visible) return null;
  const summary = mine.data.summary;
  return (
    <p className="flex items-center gap-2 rounded-[14px] border border-border bg-card px-4 py-3 text-sm">
      <Star aria-hidden className="size-4 shrink-0 text-amber-400" fill="currentColor" />
      {summary && summary.count > 0 && summary.average != null
        ? t.mine(average.format(summary.average), summary.count)
        : t.mineEmpty}
    </p>
  );
}
