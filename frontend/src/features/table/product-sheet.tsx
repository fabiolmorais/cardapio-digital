"use client";

import { Minus, Plus, UtensilsCrossed, X } from "lucide-react";
import { useEffect, useId, useRef, useState } from "react";
import { formatCents } from "@/lib/format";
import { tablePage } from "@/messages/table.pt-BR";
import { addToCart, MAX_NOTE, MAX_QUANTITY } from "./cart";
import type { MenuItem } from "./table-data";

const t = tablePage.product;

/** O preço que vale agora: o promocional, se houver. O back confere de novo ao receber o pedido. */
export function currentPriceCents(item: MenuItem) {
  return item.promoPriceCents ?? item.priceCents;
}

/**
 * O prato aberto por baixo (desenho Detalhe do prato): a foto, a descrição, a observação e a
 * quantidade, e "Adicionar · R$". Um <dialog> do navegador: prende o foco, fecha no Esc e no
 * toque fora, sem biblioteca (o JavaScript da mesa precisa ser pequeno).
 */
export function ProductSheet({ token, item, onClose }: { token: string; item: MenuItem | null; onClose: () => void }) {
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
      onClick={(event) => {
        // Tocar fora (no fundo escuro) fecha.
        if (event.target === dialog.current) close();
      }}
      className="m-0 mt-auto w-full max-w-none bg-transparent p-0 text-(--mesa-fg) backdrop:bg-black/60 sm:mx-auto sm:max-w-md"
    >
      {item && <SheetBody key={item.id} token={token} item={item} id={id} onDone={close} />}
    </dialog>
  );
}

function SheetBody({ token, item, id, onDone }: { token: string; item: MenuItem; id: string; onDone: () => void }) {
  const [quantity, setQuantity] = useState(1);
  const [note, setNote] = useState("");
  const [full, setFull] = useState(false);
  const price = currentPriceCents(item);

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

  return (
    <div className="flex max-h-[90dvh] flex-col gap-4 overflow-y-auto rounded-t-[28px] border border-(--mesa-stroke) bg-(--mesa-bg) p-5 pb-6">
      <div className="relative -mx-5 -mt-5 h-52 shrink-0 overflow-hidden rounded-t-[28px] bg-(--mesa-stroke)">
        {item.photo ? (
          // eslint-disable-next-line @next/next/no-img-element -- foto já no tamanho certo, vinda do R2
          <img src={item.photo.medium} alt="" className="size-full object-cover" decoding="async" />
        ) : (
          <span className="flex size-full items-center justify-center text-(--mesa-muted)">
            <UtensilsCrossed aria-hidden className="size-8" />
          </span>
        )}
        <button
          type="button"
          onClick={onDone}
          aria-label={t.close}
          className="absolute top-3 right-3 flex size-11 items-center justify-center rounded-full bg-black/55 text-white focus-visible:ring-3 focus-visible:ring-white focus-visible:outline-none"
        >
          <X aria-hidden className="size-5" />
        </button>
      </div>

      <div className="flex items-start justify-between gap-3">
        <h2 id={`${id}name`} className="text-2xl font-bold tracking-tight">
          {item.name}
        </h2>
        <span className="pt-1 text-lg font-bold whitespace-nowrap text-(--brand-text)">{formatCents(price)}</span>
      </div>
      {item.description && <p className="text-(--mesa-muted)">{item.description}</p>}

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

      <div className="flex items-center gap-3">
        <div className="flex items-center rounded-full border border-(--mesa-stroke) bg-(--mesa-glass) p-1">
          <button
            type="button"
            aria-label={t.decrease}
            disabled={quantity <= 1}
            onClick={() => setQuantity((q) => Math.max(1, q - 1))}
            className={round}
          >
            <Minus aria-hidden className="size-4" />
          </button>
          <span aria-live="polite" className="w-8 text-center font-bold">
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
          className="flex h-13 flex-1 items-center justify-between rounded-2xl bg-(--brand) px-5 font-semibold text-white focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:ring-offset-2 focus-visible:ring-offset-(--mesa-bg) focus-visible:outline-none"
        >
          <span>{t.add}</span>
          <span>{formatCents(price * quantity)}</span>
        </button>
      </div>
    </div>
  );
}
