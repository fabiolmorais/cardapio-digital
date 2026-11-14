"use client";

import { Plus, ShoppingBag, Volume2, VolumeX, X } from "lucide-react";
import Link from "next/link";
import { BrandLogo } from "@/components/brand-logo";
import { useEffect, useMemo, useState } from "react";
import { formatCents } from "@/lib/format";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";
import { useCart } from "./cart";
import { aLaCarte } from "./carte";
import { groupsById } from "./choices";
import { currentPriceCents, ProductSheet } from "./product-sheet";
import { ReelFeed, type ReelEntry } from "./reel-feed";
import type { MenuItem, PublicMenu } from "./table-data";
import { listOrders } from "./table-orders";

const t = tablePage.reels;

/** Um prato do cardápio como uma tela do Reels: os selos, o nome, a descrição e o preço. */
function itemEntry(item: MenuItem): ReelEntry {
  return {
    id: item.id,
    name: item.name,
    description: item.description,
    media: item.media,
    badges: [
      ...(item.featured ? [{ label: t.featured }] : []),
      ...(!item.available ? [{ label: t.soldOut, dark: true }] : []),
    ],
    price: {
      cents: item.fromPriceCents ?? currentPriceCents(item),
      from: item.fromPriceCents != null,
      strikeCents: item.promoPriceCents != null ? item.priceCents : null,
    },
  };
}

/**
 * O Reels do cardápio (desenho Reels): os pratos com foto ou vídeo, um por tela, deslizando
 * para cima; as mídias de cada prato, deslizando para o lado ("1 de 3"). O motor (a rolagem, os
 * vídeos e a barra de progresso) é o {@link ReelFeed}, o mesmo do Reels dos sabores.
 */
export function Reels({
  token,
  menu,
  startId,
  establishmentName,
  logo,
  brandColor,
  backHref,
  serving,
}: {
  token: string;
  menu: PublicMenu;
  startId: string | null;
  establishmentName: string;
  /** A logo pequena do restaurante; sem ela, as iniciais na cor da marca. */
  logo?: string;
  brandColor: string;
  /** Para onde o "Fechar" volta: o cardápio ou, vindo de um destaque, o início da mesa. */
  backHref: string;
  /** Falso com a assinatura suspensa: o Reels continua, sem o "Adicionar". */
  serving: boolean;
}) {
  // O Reels é do cardápio de sempre: o que é só do rodízio não tem preço para mostrar nem para adicionar.
  const items = useMemo(
    () =>
      aLaCarte(menu)
        .flatMap((c) => c.products)
        .filter((p) => p.media.length > 0),
    [menu],
  );
  const startIndex = Math.max(
    0,
    items.findIndex((p) => p.id === startId),
  );
  const [muted, setMuted] = useState(true);
  const [ordering, setOrdering] = useState(serving);
  const [adding, setAdding] = useState<MenuItem | null>(null);
  const groups = useMemo(() => groupsById(menu.optionGroups, menu.optionMedia), [menu.optionGroups, menu.optionMedia]);
  const entries = useMemo(() => items.map(itemEntry), [items]);
  const byId = new Map(items.map((item) => [item.id, item]));
  const { count, totalCents } = useCart(token);

  // O restaurante desligou o pedido pelo celular (ou a assinatura está suspensa)? O "Adicionar"
  // some (o Reels continua).
  useEffect(() => {
    let cancelled = false;
    listOrders(token)
      .then((orders) => {
        if (!cancelled) setOrdering(serving && orders.orderingEnabled);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [token, serving]);

  const round =
    "flex size-11 items-center justify-center rounded-full border border-white/20 bg-black/40 text-white backdrop-blur-md focus-visible:ring-3 focus-visible:ring-white focus-visible:outline-none";

  return (
    <div className="fixed inset-0 bg-black text-white">
      {items.length === 0 && <p className="p-6 text-white/80">{t.empty}</p>}
      <ReelFeed
        entries={entries}
        startIndex={startIndex}
        muted={muted}
        swipeHint={t.swipeHint}
        action={(entry) => {
          const item = byId.get(entry.id)!;
          return (
            ordering &&
            item.available && (
              <button
                type="button"
                onClick={() => setAdding(item)}
                aria-label={`${t.add} ${item.name}`}
                className="flex h-12 items-center gap-1.5 rounded-2xl bg-(--brand) px-5 font-semibold text-white focus-visible:ring-3 focus-visible:ring-white focus-visible:outline-none"
              >
                <Plus aria-hidden className="size-4" />
                {t.add}
              </button>
            )
          );
        }}
      />

      {/* Por cima de tudo: a marca, o fechar, o som e o pedido. */}
      <div className="pointer-events-none fixed inset-x-0 top-0 flex items-center justify-between gap-3 px-4 pt-9">
        <span className="flex min-w-0 items-center gap-2.5">
          <BrandLogo logo={logo} name={establishmentName} color={brandColor} alt="" className="size-8 text-xs" />
          <span className="truncate text-sm font-semibold drop-shadow">{establishmentName}</span>
        </span>
        <Link href={backHref} aria-label={t.close} className={cn(round, "pointer-events-auto")}>
          <X aria-hidden className="size-5" />
        </Link>
      </div>
      <div className="fixed right-3.5 bottom-64 flex flex-col items-center gap-1 text-[11px] font-semibold">
        <button
          type="button"
          aria-label={muted ? t.soundOn : t.soundOff}
          aria-pressed={!muted}
          onClick={() => setMuted((m) => !m)}
          className={round}
        >
          {muted ? <VolumeX aria-hidden className="size-5" /> : <Volume2 aria-hidden className="size-5" />}
        </button>
        <span aria-hidden>{t.sound}</span>
      </div>
      {count > 0 && (
        <Link
          href={`/m/${token}/carrinho`}
          className="fixed top-22 right-4 flex h-10 items-center gap-2 rounded-full bg-(--brand) px-3.5 text-sm font-semibold text-white shadow-lg focus-visible:ring-3 focus-visible:ring-white focus-visible:outline-none"
        >
          <ShoppingBag aria-hidden className="size-4" />
          {t.seeCart} · {formatCents(totalCents)}
        </Link>
      )}

      {/* O "Adicionar" abre o detalhe do prato (3.2); tocar na foto dele volta ao vídeo. */}
      <ProductSheet token={token} item={adding} onClose={() => setAdding(null)} onPhoto={() => {}} groups={groups} />
    </div>
  );
}
