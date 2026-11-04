"use client";

import { ChevronRight, Flame } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { tablePage } from "@/messages/table.pt-BR";
import { useTableCalls } from "./table-calls";
import { listRodizios, useTableOrders } from "./table-orders";

const t = tablePage.rodizio;
const card =
  "flex min-h-16 items-center gap-3.5 rounded-3xl p-4 focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none";

/**
 * O rodízio na tela inicial: com ele ativo, a faixa "Rodízio Japonês · 4 pessoas" que leva às
 * rodadas; sem ele, o "Quero o rodízio" (só se o restaurante tem algum à la minute).
 */
export function RodizioEntry({ token }: { token: string }) {
  const { data } = useTableOrders(token);
  const { rodizio: request } = useTableCalls(token);
  const [offered, setOffered] = useState(false);

  useEffect(() => {
    let cancelled = false;
    listRodizios(token)
      .then((list) => !cancelled && setOffered(list.length > 0))
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [token]);

  const active = data?.rodizio;
  if (active) {
    const rounds = data.orders.filter((o) => o.items.some((i) => i.rodizio)).length;
    return (
      <Link href={`/m/${token}/cardapio#rodizio`} className={`${card} bg-(--brand) text-white`}>
        <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-white/20">
          <Flame aria-hidden className="size-5" />
        </span>
        <span className="flex flex-1 flex-col">
          <span className="font-semibold">{t.banner(active.name, active.people)}</span>
          <span className="text-sm opacity-90">{t.rounds(rounds)}</span>
        </span>
        <ChevronRight aria-hidden className="size-5" />
      </Link>
    );
  }
  if (!offered) return null;
  return (
    <Link
      href={`/m/${token}/rodizio`}
      className={`${card} border border-(--mesa-stroke) bg-(--mesa-glass)`}
    >
      <span className="flex size-10 shrink-0 items-center justify-center rounded-xl border border-(--mesa-stroke) text-(--brand-text)">
        <Flame aria-hidden className="size-5" />
      </span>
      <span className="flex flex-1 flex-col">
        <span className="font-semibold">{t.want}</span>
        <span className="text-sm text-(--mesa-muted)">{request ? t.requestedSub : t.wantSub}</span>
      </span>
      <ChevronRight aria-hidden className="size-5" />
    </Link>
  );
}
