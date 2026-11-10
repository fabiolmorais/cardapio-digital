"use client";

import { ArrowLeft, Minus, Play, Plus, UtensilsCrossed } from "lucide-react";
import Link from "next/link";
import { useEffect, useId, useRef, useState } from "react";
import { formatCents } from "@/lib/format";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";
import { addToCart, MAX_NOTE, MAX_QUANTITY } from "./cart";
import { discountPercent } from "./discount";
import type { MenuItem } from "./table-data";

const t = tablePage.product;

/** O preço que vale agora: o promocional, se houver. O back confere de novo ao receber o pedido. */
export function currentPriceCents(item: MenuItem) {
  return item.promoPriceCents ?? item.priceCents;
}

/**
 * O detalhe do prato em tela cheia (desenho 3.2): as fotos no alto (tocar nelas abre o Reels do
 * prato); os selos, o nome e o preço, a descrição e a observação num card de vidro sobre a foto;
 * e, num card flutuante fixo embaixo, a quantidade e "Adicionar · R$". Um <dialog> do navegador:
 * prende o foco e fecha no Esc, sem biblioteca (o JavaScript da mesa precisa ser pequeno).
 *
 * `reelsHref`: as fotos levam ao Reels (no cardápio). `onPhoto`: no próprio Reels, tocar na foto só
 * fecha o detalhe e volta ao vídeo.
 */
export function ProductSheet({
  token,
  item,
  onClose,
  reelsHref,
  onPhoto,
}: {
  token: string;
  item: MenuItem | null;
  onClose: () => void;
  reelsHref?: (item: MenuItem) => string | undefined;
  onPhoto?: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const id = useId();

  useEffect(() => {
    const element = dialog.current;
    if (item && element && !element.open) element.showModal();
  }, [item]);

  function close() {
    dialog.current?.close();
  }

  return (
    <dialog
      ref={dialog}
      aria-labelledby={`${id}name`}
      onClose={onClose}
      className="m-0 h-dvh max-h-none w-full max-w-none overflow-hidden bg-(--mesa-bg) p-0 text-(--mesa-fg) backdrop:bg-black/70 sm:mx-auto sm:max-w-md"
    >
      {item && (
        <SheetBody
          key={item.id}
          token={token}
          item={item}
          id={id}
          onDone={close}
          reelsHref={reelsHref?.(item)}
          onPhoto={
            onPhoto
              ? () => {
                  close();
                  onPhoto();
                }
              : undefined
          }
        />
      )}
    </dialog>
  );
}

function SheetBody({
  token,
  item,
  id,
  onDone,
  reelsHref,
  onPhoto,
}: {
  token: string;
  item: MenuItem;
  id: string;
  onDone: () => void;
  reelsHref?: string;
  onPhoto?: () => void;
}) {
  const [quantity, setQuantity] = useState(1);
  const [note, setNote] = useState("");
  const [full, setFull] = useState(false);
  const [shown, setShown] = useState(0);
  const price = currentPriceCents(item);
  const off = discountPercent(item);
  const photos = item.media.flatMap((m) => (m.image ? [m.image.large] : []));
  if (photos.length === 0 && item.photo) photos.push(item.photo.medium);
  const video = item.media.find((m) => m.video)?.video;

  function add() {
    const added = addToCart(token, {
      productId: item.id,
      name: item.name,
      unitPriceCents: price,
      thumb: item.photo?.thumb ?? null,
      quantity,
      note,
    });
    if (added) onDone();
    else setFull(true);
  }

  const round =
    "flex size-11 items-center justify-center rounded-full focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none disabled:opacity-40";
  const overPhoto =
    "flex h-11 items-center gap-2 rounded-full border border-white/20 bg-black/45 px-3.5 text-sm font-semibold text-white backdrop-blur-md focus-visible:ring-3 focus-visible:ring-white focus-visible:outline-none";

  // Tocar na foto: o Reels do prato (link no cardápio; no próprio Reels, só fecha o detalhe).
  const gallery = (
    <div
      onScroll={(event) => {
        const el = event.currentTarget;
        setShown(Math.round(el.scrollLeft / el.clientWidth));
      }}
      className="flex size-full snap-x snap-mandatory overflow-x-auto [scrollbar-width:none]"
    >
      {photos.map((src, i) => (
        // eslint-disable-next-line @next/next/no-img-element -- foto grande, vinda do R2
        <img
          key={src}
          src={src}
          alt=""
          decoding="async"
          loading={i === 0 ? "eager" : "lazy"}
          className="size-full shrink-0 snap-center object-cover"
        />
      ))}
    </div>
  );

  return (
    <>
      <div className="h-full overflow-y-auto overscroll-contain pb-28">
        <div className="relative h-[51dvh] overflow-hidden bg-(--mesa-stroke)">
          {photos.length === 0 ? (
            <span className="flex size-full items-center justify-center pb-14 text-(--mesa-muted)">
              <UtensilsCrossed aria-hidden className="size-10" />
            </span>
          ) : reelsHref ? (
            <Link
              href={reelsHref}
              prefetch={false}
              aria-label={t.openReels(item.name)}
              className="block size-full"
            >
              {gallery}
            </Link>
          ) : onPhoto ? (
            <button
              type="button"
              onClick={onPhoto}
              aria-label={t.backToReels}
              className="block size-full"
            >
              {gallery}
            </button>
          ) : (
            gallery
          )}
          <div
            aria-hidden
            className="pointer-events-none absolute inset-x-0 top-0 h-1/3 bg-linear-to-b from-black/35 to-transparent"
          />
          <button
            type="button"
            onClick={onDone}
            aria-label={t.close}
            className={cn(
              overPhoto,
              "absolute top-6 left-5 w-11 justify-center px-0",
            )}
          >
            <ArrowLeft aria-hidden className="size-5" />
          </button>
          {video &&
            (reelsHref || onPhoto) &&
            (reelsHref ? (
              <Link
                href={reelsHref}
                prefetch={false}
                className={cn(overPhoto, "absolute top-6 right-5")}
              >
                <Play aria-hidden className="size-4 fill-current" />
                {t.seeVideo(video.durationSeconds ?? null)}
              </Link>
            ) : (
              <button
                type="button"
                onClick={onPhoto}
                className={cn(overPhoto, "absolute top-6 right-5")}
              >
                <Play aria-hidden className="size-4 fill-current" />
                {t.seeVideo(video.durationSeconds ?? null)}
              </button>
            ))}
          {photos.length > 1 && (
            <span
              aria-hidden
              className="absolute inset-x-0 bottom-[72px] flex justify-center gap-1.5"
            >
              {photos.map((src, i) => (
                <span
                  key={src}
                  className={cn(
                    "h-1.5 rounded-full bg-white/50 transition-all",
                    i === shown ? "w-5 bg-white" : "w-1.5",
                  )}
                />
              ))}
            </span>
          )}
        </div>

        {/* O texto num card de vidro que sobe sobre a foto (desenho 3.2). */}
        <section className="relative mx-3 -mt-14 flex flex-col gap-3.5 rounded-[28px] border border-(--mesa-stroke) bg-(--mesa-bar) p-5 shadow-[0_20px_50px_rgba(0,0,0,.35)] backdrop-blur-xl">
          {(item.featured || off !== null) && (
            <div className="flex flex-wrap gap-2">
              {item.featured && (
                <span className="rounded-full bg-(--brand)/20 px-2.5 py-1 text-xs font-semibold text-(--brand-text)">
                  {t.featured}
                </span>
              )}
              {off !== null && (
                <span className="rounded-full bg-(--brand) px-2.5 py-1 text-xs font-bold text-white">
                  {t.off(off)}
                </span>
              )}
            </div>
          )}
          <div className="flex items-start justify-between gap-3">
            <h2
              id={`${id}name`}
              className="text-[26px] leading-[31px] font-bold tracking-tight"
            >
              {item.name}
            </h2>
            <span className="flex flex-col items-end pt-1">
              {item.promoPriceCents != null && (
                <s className="text-xs text-(--mesa-muted)">
                  {formatCents(item.priceCents)}
                </s>
              )}
              <span className="text-xl font-bold whitespace-nowrap text-(--brand-text)">
                {formatCents(price)}
              </span>
            </span>
          </div>
          {item.description && (
            <p className="text-[15px] leading-[22px] text-(--mesa-muted)">
              {item.description}
            </p>
          )}
          <div aria-hidden className="h-px bg-(--mesa-stroke)" />
          <div className="flex flex-col gap-2">
            <label htmlFor={`${id}note`} className="font-semibold">
              {t.note}
            </label>
            <input
              id={`${id}note`}
              value={note}
              maxLength={MAX_NOTE}
              onChange={(event) => setNote(event.target.value)}
              placeholder={t.notePlaceholder}
              autoComplete="off"
              className="h-12 w-full rounded-2xl border border-(--mesa-stroke) bg-(--mesa-glass) px-4 text-base text-(--mesa-fg) outline-none placeholder:text-(--mesa-muted) focus-visible:ring-3 focus-visible:ring-(--brand)"
            />
          </div>
          {full && (
            <p role="alert" className="text-sm font-medium text-(--brand-text)">
              {t.full}
            </p>
          )}
        </section>
      </div>

      {/* A quantidade e o "Adicionar · R$" num card flutuante, fixo embaixo (desenho 3.2). */}
      <div className="absolute inset-x-3 bottom-[max(env(safe-area-inset-bottom),20px)] flex items-center gap-2.5 rounded-3xl border border-(--mesa-stroke) bg-(--mesa-bar) p-2.5 shadow-[0_20px_50px_rgba(0,0,0,.35)] backdrop-blur-xl">
        <div className="flex items-center">
          <button
            type="button"
            aria-label={t.decrease}
            disabled={quantity <= 1}
            onClick={() => setQuantity((q) => Math.max(1, q - 1))}
            className={round}
          >
            <Minus aria-hidden className="size-4" />
          </button>
          <span aria-live="polite" className="w-7 text-center font-bold">
            <span className="sr-only">{t.quantity(quantity)}</span>
            <span aria-hidden>{quantity}</span>
          </span>
          <button
            type="button"
            aria-label={t.increase}
            disabled={quantity >= MAX_QUANTITY}
            onClick={() => setQuantity((q) => Math.min(MAX_QUANTITY, q + 1))}
            className={round}
          >
            <Plus aria-hidden className="size-4" />
          </button>
        </div>
        <button
          type="button"
          onClick={add}
          className="flex h-13 flex-1 items-center justify-between gap-3 rounded-2xl bg-(--brand) px-5 font-semibold text-white focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:ring-offset-2 focus-visible:ring-offset-(--mesa-bg) focus-visible:outline-none"
        >
          <span>{t.add}</span>
          <span aria-hidden className="h-5 w-px bg-white/35" />
          <span>{formatCents(price * quantity)}</span>
        </button>
      </div>
    </>
  );
}
