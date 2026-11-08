"use client";

import { ChevronRight, Flame } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { formatCents } from "@/lib/format";
import { tablePage } from "@/messages/table.pt-BR";
import { useTableCalls } from "./table-calls";
import { listRodizios, useTableOrders, type RodizioQuote } from "./table-orders";

const t = tablePage.rodizio;
const card =
  "flex min-h-16 items-center gap-3.5 rounded-3xl p-4 focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none";

/**
 * O rodízio na tela inicial: com ele ativo, a faixa "Rodízio Japonês · 4 pessoas" que leva às
 * rodadas; sem ele, o "Quero o rodízio" (só se o restaurante tem algum à la minute).
 */
export function RodizioEntry({ token, photo }: { token: string; photo?: string }) {
  const { data } = useTableOrders(token);
  const { rodizio: request } = useTableCalls(token);
  const [offered, setOffered] = useState<RodizioQuote[]>([]);

  useEffect(() => {
    let cancelled = false;
    listRodizios(token)
      .then((list) => !cancelled && setOffered(list))
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
          <span className="text-sm opacity-90">
            {active.type === "PASSAGEM" ? t.card.short[active.status] : t.rounds(rounds)}
          </span>
        </span>
        <ChevronRight aria-hidden className="size-5" />
      </Link>
    );
  }
  if (offered.length === 0) return null;
  // O banner (desenho 2.1): o nome (um rodízio só) e o menor preço por pessoa, o da faixa mais cara
  // de cada rodízio (a de adulto), sobre a foto de um prato.
  const name = offered.length === 1 ? offered[0].name : t.eyebrow;
  const prices = offered.map((r) => Math.max(...r.tiers.map((tier) => tier.priceCents))).filter(Number.isFinite);
  const price = prices.length ? Math.min(...prices) : null;
  return (
    <section
      aria-label={t.eyebrow}
      className="relative flex h-[156px] overflow-hidden rounded-3xl border border-(--mesa-stroke) bg-(--mesa-glass)"
    >
      {photo && (
        // eslint-disable-next-line @next/next/no-img-element -- foto de um prato, já vinda do R2
        <img
          src={photo}
          alt=""
          decoding="async"
          className="absolute top-0 -right-10 h-full w-[260px] object-cover [mask-image:linear-gradient(90deg,transparent_0%,#000_45%)]"
        />
      )}
      <div className="relative flex w-[210px] flex-col justify-between p-4">
        <span className="flex items-center gap-1.5 text-[11px] font-semibold tracking-[.3px] text-(--brand-text) uppercase">
          <Flame aria-hidden className="size-3.5" />
          {name}
        </span>
        <span className="text-xl leading-[26px] font-semibold tracking-[-0.3px]">
          {request ? (
            <span className="text-base">{t.requestedSub}</span>
          ) : (
            <>
              {price !== null ? t.allYouCan : t.wantSub}
              {price !== null && (
                <>
                  <br />
                  <span className="text-(--brand-text)">{formatCents(price)}</span>
                </>
              )}
            </>
          )}
        </span>
        <Link
          href={`/m/${token}/rodizio`}
          className="flex h-10 items-center self-start rounded-xl bg-(--brand) px-3.5 text-sm font-semibold text-white focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:outline-none"
        >
          {t.want}
        </Link>
      </div>
    </section>
  );
}
