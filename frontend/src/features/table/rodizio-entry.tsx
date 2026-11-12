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
  // O banner (desenho 2.1): um por rodízio, com o nome e o preço por pessoa (o da faixa mais cara,
  // a de adulto), sobre a foto de um prato. Com mais de um, ficam lado a lado e a mesa desliza
  // entre eles. Com o rodízio já pedido ao garçom, um banner só, dizendo que ele está vindo.
  if (request || offered.length === 1) {
    return <Banner token={token} photo={photo} rodizio={request ? null : offered[0]} requested={Boolean(request)} />;
  }
  return (
    <section aria-label={t.eyebrow} className="-mx-5 flex flex-col gap-2">
      <ul
        aria-label={t.options(offered.length)}
        className="flex snap-x snap-mandatory scroll-px-5 gap-3 overflow-x-auto px-5 [scrollbar-width:none]"
      >
        {offered.map((rodizio) => (
          <li key={rodizio.id} className="w-[86%] max-w-[340px] shrink-0 snap-start">
            <Banner token={token} photo={photo} rodizio={rodizio} requested={false} />
          </li>
        ))}
      </ul>
      <span className="px-5 text-xs text-(--mesa-muted)">{t.swipe(offered.length)}</span>
    </section>
  );
}

/** O preço por pessoa do rodízio: o da faixa mais cara (a de adulto). */
function adultPrice(rodizio: RodizioQuote): number | null {
  const prices = rodizio.tiers.map((tier) => tier.priceCents);
  return prices.length ? Math.max(...prices) : null;
}

/** Um banner de rodízio; sem `rodizio`, o banner do pedido já feito ao garçom. */
function Banner({
  token,
  photo,
  rodizio,
  requested,
}: {
  token: string;
  photo?: string;
  rodizio: RodizioQuote | null;
  requested: boolean;
}) {
  const price = rodizio ? adultPrice(rodizio) : null;
  return (
    <article
      aria-label={rodizio?.name ?? t.eyebrow}
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
          {rodizio?.name ?? t.eyebrow}
        </span>
        <span className="text-xl leading-[26px] font-semibold tracking-[-0.3px]">
          {requested ? (
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
          aria-label={rodizio ? t.wantNamed(rodizio.name) : undefined}
          className="flex h-10 items-center self-start rounded-xl bg-(--brand) px-3.5 text-sm font-semibold text-white focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:outline-none"
        >
          {t.want}
        </Link>
      </div>
    </article>
  );
}
