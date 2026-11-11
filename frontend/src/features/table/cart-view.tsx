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
import { ItemThumb, StatusBadge } from "./order-status";

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
  // O pedido enviado, com a miniatura de cada item (o carrinho tem a foto; a resposta do envio, não).
  const [sent, setSent] = useState<{ order: TableOrder; thumbs: Map<string, string | null> } | null>(null);
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
      const thumbs = new Map(lines.map((line) => [line.name, line.thumb]));
      clearCart(token);
      setSent({ order, thumbs });
    } catch (e) {
      setError((e as Error).message);
    }
    setSending(false);
  }

  // "Pedido enviado!" só enquanto o carrinho está vazio: com itens novos (depois do "Pedir mais
  // alguma coisa"), a tela volta a ser o carrinho, com o resumo do pedido novo.
  if (sent && lines.length === 0) {
    return <Sent token={token} order={sent.order} thumbs={sent.thumbs} onMore={() => setSent(null)} />;
  }
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

/**
 * "Pedido enviado!" (desenho 4.2): o check verde pulsando, o número e a hora, e cada item com a
 * foto e a tag do passo. Acima dos limites, "aguardando o garçom", em amarelo.
 */
function Sent({
  token,
  order,
  thumbs,
  onMore,
}: {
  token: string;
  order: TableOrder;
  thumbs: Map<string, string | null>;
  onMore: () => void;
}) {
  const waiting = order.status === "PENDING_APPROVAL";
  const count = order.items.reduce((sum, item) => sum + item.quantity, 0);
  return (
    <>
      <section role="status" className="flex flex-col items-center gap-6 pt-8 text-center">
        <span
          className={cn(
            "relative flex size-32 items-center justify-center rounded-full",
            waiting ? "bg-(--mesa-warn-soft)" : "bg-(--mesa-ok-soft)",
          )}
        >
          <span
            aria-hidden
            className={cn(
              "absolute inset-0 animate-ping rounded-full opacity-30 motion-reduce:hidden",
              waiting ? "bg-(--mesa-warn)" : "bg-(--mesa-ok)",
            )}
          />
          <span
            className={cn(
              "relative flex size-20 items-center justify-center rounded-full text-[#0F0D0C]",
              waiting ? "bg-(--mesa-warn)" : "bg-(--mesa-ok) shadow-[0_16px_36px_rgba(74,222,128,.30)]",
            )}
          >
            {waiting ? (
              <Clock aria-hidden className="size-9" strokeWidth={2.4} />
            ) : (
              <Check aria-hidden className="size-10" strokeWidth={2.6} />
            )}
          </span>
        </span>
        <div className="flex flex-col gap-2.5">
          <span className="text-[13px] text-(--mesa-muted)">{t.number(order.number, timeOf(order.createdAt))}</span>
          <h1 className="text-[32px] leading-[38px] font-bold tracking-tight">
            {waiting ? t.waitingTitle : t.sentTitle}
          </h1>
          <p className="text-[15px] text-(--mesa-muted)">{waiting ? t.waitingText : t.sentText(count)}</p>
        </div>
      </section>
      <ul className="flex flex-col rounded-[22px] border border-(--mesa-stroke) bg-(--mesa-glass) p-1.5">
        {order.items.map((item) => (
          <li key={item.id} className="flex items-center gap-3 p-2.5 text-sm">
            <ItemThumb src={thumbs.get(item.productName)} size="sm" />
            <span className="flex-1 text-left">
              {item.quantity}× {item.productName}
            </span>
            {waiting ? (
              <StatusBadge tone="PENDING">{tablePage.orders.orderStatus.PENDING_APPROVAL}</StatusBadge>
            ) : (
              <StatusBadge tone={item.status}>{tablePage.orders.status[item.status]}</StatusBadge>
            )}
          </li>
        ))}
      </ul>
      <div className="flex flex-col gap-2">
        <Link href={`/m/${token}/pedidos`} className={primary}>
          {t.follow}
        </Link>
        <Link href={`/m/${token}/cardapio`} onClick={onMore} className={secondary}>
          {t.more}
        </Link>
      </div>
    </>
  );
}
