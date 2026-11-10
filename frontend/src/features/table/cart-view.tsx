"use client";

import { Check, ChevronLeft, Clock, MessageCircle, Minus, Plus, Trash2, UtensilsCrossed } from "lucide-react";
import Link from "next/link";
import { useEffect, useId, useState } from "react";
import { formatCents } from "@/lib/format";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";
import { clearCart, MAX_NOTE, MAX_QUANTITY, setNote, setQuantity, useCart, useHydrated, type CartLine } from "./cart";
import { listOrders, placeOrder, timeOf, type TableOrder } from "./table-orders";
import { MesaNotice } from "./mesa-notice";

const t = tablePage.cart;

const focus = "focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none";
const primary =
  "flex h-14 w-full items-center justify-center rounded-2xl bg-(--brand) font-semibold text-white focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:ring-offset-2 focus-visible:ring-offset-(--mesa-bg) focus-visible:outline-none disabled:opacity-70";
const secondary = cn(
  "flex h-12 w-full items-center justify-center rounded-2xl text-sm font-semibold text-(--mesa-muted)",
  focus,
);

/**
 * "Seu pedido" (desenho Carrinho): as linhas com quantidade e observação, o total e "Enviar pedido
 * para a cozinha". Enviado, vira "Pedido enviado!" (ou "aguardando o garçom", acima dos limites).
 */
export function CartView({ token, tableLabel }: { token: string; tableLabel: string }) {
  const { lines, count, totalCents } = useCart(token);
  const hydrated = useHydrated();
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState<TableOrder | null>(null);
  const [ordering, setOrdering] = useState(true);
  const menu = `/m/${token}/cardapio`;

  useEffect(() => {
    let cancelled = false;
    listOrders(token)
      .then((orders) => {
        if (!cancelled) setOrdering(orders.orderingEnabled);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [token]);

  async function send() {
    setSending(true);
    setError(null);
    try {
      const order = await placeOrder(token, lines);
      clearCart(token);
      setSent(order);
    } catch (e) {
      setError((e as Error).message);
    }
    setSending(false);
  }

  if (sent) return <Sent token={token} order={sent} />;
  // O carrinho mora no celular: até lê-lo, não mostra "vazio".
  if (!hydrated) return <p className="text-(--mesa-muted)">{tablePage.loading}</p>;

  return (
    <>
      <header className="flex items-center gap-3">
        <Link
          href={menu}
          aria-label={t.back}
          className={cn(
            "flex size-11 items-center justify-center rounded-full border border-(--mesa-stroke) bg-(--mesa-glass)",
            focus,
          )}
        >
          <ChevronLeft aria-hidden className="size-5" />
        </Link>
        <div className="flex flex-1 flex-col">
          <h1 className="text-2xl font-bold tracking-tight">{t.title}</h1>
          <span className="text-sm text-(--mesa-muted)">
            {tablePage.where(tableLabel, "")}
            {count > 0 && ` · ${tablePage.cartBar.items(count)}`}
          </span>
        </div>
        {lines.length > 0 && (
          <button
            type="button"
            onClick={() => clearCart(token)}
            className={cn("h-11 rounded-xl px-3 text-sm font-semibold text-(--mesa-muted)", focus)}
          >
            {t.clear}
          </button>
        )}
      </header>

      {lines.length === 0 ? (
        <section className="flex flex-col items-center gap-3 pt-10 text-center">
          <p className="text-lg font-semibold">{t.empty}</p>
          <p className="text-(--mesa-muted)">{t.emptyHint}</p>
          <Link href={menu} className={cn(primary, "mt-4")}>
            {t.seeMenu}
          </Link>
        </section>
      ) : (
        <>
          <ul className="flex flex-col gap-2.5">
            {lines.map((line) => (
              <Line key={line.key} token={token} line={line} />
            ))}
          </ul>

          <MesaNotice tone={ordering ? "info" : "warning"}>{ordering ? t.info : t.disabled}</MesaNotice>

          <section className="flex flex-col gap-3 rounded-3xl border border-(--mesa-stroke) bg-(--mesa-glass) p-4.5">
            <div className="flex justify-between text-sm text-(--mesa-muted)">
              <span>{t.subtotal(count)}</span>
              <span>{formatCents(totalCents)}</span>
            </div>
            <div className="flex items-baseline justify-between">
              <span className="font-semibold">{t.total}</span>
              <span className="text-2xl font-bold">{formatCents(totalCents)}</span>
            </div>
            {error && (
              <p role="alert" className="text-sm font-medium text-(--mesa-danger)">
                {error}
              </p>
            )}
            <button type="button" disabled={sending || !ordering} onClick={send} className={primary}>
              {sending ? t.sending : t.send}
            </button>
          </section>
        </>
      )}
    </>
  );
}

function Line({ token, line }: { token: string; line: CartLine }) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(line.note);
  const id = useId();
  const round = cn("flex size-10 items-center justify-center rounded-full disabled:opacity-40", focus);

  function saveNote() {
    setNote(token, line.key, draft);
    setEditing(false);
  }

  return (
    <li className="flex gap-3 rounded-[22px] border border-(--mesa-stroke) bg-(--mesa-glass) p-2.5">
      <span className="size-19 shrink-0 overflow-hidden rounded-2xl bg-(--mesa-stroke)">
        {line.thumb ? (
          // eslint-disable-next-line @next/next/no-img-element -- miniatura já no tamanho certo, vinda do R2
          <img src={line.thumb} alt="" width={76} height={76} className="size-full object-cover" />
        ) : (
          <span className="flex size-full items-center justify-center text-(--mesa-muted)">
            <UtensilsCrossed aria-hidden className="size-5" />
          </span>
        )}
      </span>
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <h2 className="font-semibold">{line.name}</h2>
        {editing ? (
          <div className="flex gap-2">
            <label htmlFor={`${id}note`} className="sr-only">
              {t.noteLabel(line.name)}
            </label>
            <input
              id={`${id}note`}
              value={draft}
              maxLength={MAX_NOTE}
              autoFocus
              autoComplete="off"
              placeholder={tablePage.product.notePlaceholder}
              onChange={(event) => setDraft(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") saveNote();
              }}
              className="h-10 min-w-0 flex-1 rounded-xl border border-(--mesa-stroke) bg-(--mesa-bg) px-3 text-base outline-none focus-visible:ring-3 focus-visible:ring-(--brand)"
            />
            <button
              type="button"
              onClick={saveNote}
              className={cn("h-10 rounded-xl px-3 text-sm font-semibold text-(--brand-text)", focus)}
            >
              {t.saveNote}
            </button>
          </div>
        ) : line.note ? (
          <button
            type="button"
            onClick={() => setEditing(true)}
            aria-label={`${t.note(line.note)}. ${t.editNote}`}
            className={cn("flex w-fit items-center gap-1.5 rounded text-left text-sm text-(--mesa-muted)", focus)}
          >
            <MessageCircle aria-hidden className="size-3.5 shrink-0" />
            {line.note}
          </button>
        ) : (
          <button
            type="button"
            onClick={() => setEditing(true)}
            className={cn("w-fit rounded text-left text-sm font-semibold text-(--brand-text)", focus)}
          >
            {t.addNote}
          </button>
        )}
        <span className="mt-auto flex items-center justify-between gap-2 pt-1">
          <span className="font-bold">{formatCents(line.unitPriceCents * line.quantity)}</span>
          <span className="flex items-center rounded-xl border border-(--mesa-stroke) p-0.5">
            <button
              type="button"
              aria-label={line.quantity === 1 ? t.remove(line.name) : t.decrease(line.name)}
              onClick={() => setQuantity(token, line.key, line.quantity - 1)}
              className={round}
            >
              {line.quantity === 1 ? (
                <Trash2 aria-hidden className="size-4" />
              ) : (
                <Minus aria-hidden className="size-4" />
              )}
            </button>
            <span className="w-7 text-center font-bold" aria-live="polite">
              {line.quantity}
            </span>
            <button
              type="button"
              aria-label={t.increase(line.name)}
              disabled={line.quantity >= MAX_QUANTITY}
              onClick={() => setQuantity(token, line.key, line.quantity + 1)}
              className={round}
            >
              <Plus aria-hidden className="size-4" />
            </button>
          </span>
        </span>
      </div>
    </li>
  );
}

/** "Pedido enviado!" (desenho Pedido enviado), ou "aguardando o garçom" acima dos limites. */
function Sent({ token, order }: { token: string; order: TableOrder }) {
  const waiting = order.status === "PENDING_APPROVAL";
  const count = order.items.reduce((sum, item) => sum + item.quantity, 0);
  return (
    <>
      <section role="status" className="flex flex-col items-center gap-5 pt-8 text-center">
        <span className="flex size-28 items-center justify-center rounded-full bg-(--mesa-glass)">
          <span className="flex size-18 items-center justify-center rounded-full bg-(--brand) text-white">
            {waiting ? <Clock aria-hidden className="size-8" /> : <Check aria-hidden className="size-9" />}
          </span>
        </span>
        <div className="flex flex-col gap-2">
          <span className="text-sm font-semibold tracking-wide text-(--brand-text) uppercase">
            {t.number(order.number, timeOf(order.createdAt))}
          </span>
          <h1 className="text-3xl font-bold tracking-tight">{waiting ? t.waitingTitle : t.sentTitle}</h1>
          <p className="text-(--mesa-muted)">{waiting ? t.waitingText : t.sentText(count)}</p>
        </div>
      </section>
      <ul className="flex flex-col rounded-3xl border border-(--mesa-stroke) bg-(--mesa-glass) p-1.5">
        {order.items.map((item) => (
          <li key={item.id} className="flex items-center gap-3 px-3 py-2.5 text-sm">
            <span className="flex-1">
              {item.quantity}× {item.productName}
            </span>
            <span className="rounded-full border border-(--mesa-stroke) px-2.5 py-0.5 text-xs font-semibold">
              {waiting ? tablePage.orders.orderStatus.PENDING_APPROVAL : tablePage.orders.status[item.status]}
            </span>
          </li>
        ))}
      </ul>
      <div className="flex flex-col gap-2">
        <Link href={`/m/${token}/pedidos`} className={primary}>
          {t.follow}
        </Link>
        <Link href={`/m/${token}/cardapio`} className={secondary}>
          {t.more}
        </Link>
      </div>
    </>
  );
}
