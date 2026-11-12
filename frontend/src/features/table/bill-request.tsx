"use client";

import { Banknote, Check, ChevronLeft, CreditCard, QrCode, Receipt, Star } from "lucide-react";
import Link from "next/link";
import { useEffect, useState, type ReactNode } from "react";
import { formatCents } from "@/lib/format";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";
import { PresencePanel } from "./presence-panel";
import { TabClosed } from "./tab-closed";
import { cancelCall, requestCall, useTableCalls, type PaymentMethod } from "./table-calls";
import { listOrders, type TableOrders } from "./table-orders";

const t = tablePage.bill;
const METHODS: { id: PaymentMethod; icon: ReactNode }[] = [
  { id: "PIX", icon: <QrCode aria-hidden className="size-5" /> },
  { id: "CARD", icon: <CreditCard aria-hidden className="size-5" /> },
  { id: "CASH", icon: <Banknote aria-hidden className="size-5" /> },
];

/**
 * "Pedir a conta" (desenhos Conta e Conta solicitada): o total da comanda, a forma de pagamento e
 * o pedido. Com a conta já pedida, mostra quem está vindo e deixa trocar a forma de pagamento ou
 * cancelar. Se a mesa já pagou (com itens ainda a caminho), o que foi pago sai do total e, sem
 * nada a pagar, a tela diz que a conta está paga. Quando o garçom fecha a comanda, agradece.
 */
export function BillRequest({ token, reviewUrl }: { token: string; reviewUrl: string | null }) {
  const state = useTableCalls(token);
  const { calls, bill, busy, error, run } = state;
  const [chosen, setChosen] = useState<PaymentMethod>("PIX");
  const [changing, setChanging] = useState(false);
  const [orders, setOrders] = useState<TableOrders | null>(null);
  const home = `/m/${token}`;

  // O total vem dos pedidos da comanda. Busca de novo quando os chamados mudam (pedir, trocar,
  // o garçom aceitar): é quando o cliente está olhando esta tela.
  useEffect(() => {
    let cancelled = false;
    listOrders(token)
      .then((found) => {
        if (!cancelled) setOrders(found);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [token, calls]);

  const header = (
    <header className="flex items-center gap-3">
      <Link
        href={home}
        aria-label={t.back}
        className="flex size-11 items-center justify-center rounded-full border border-(--mesa-stroke) bg-(--mesa-glass) focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
      >
        <ChevronLeft aria-hidden className="size-5" />
      </Link>
      <h1 className="text-2xl font-bold tracking-tight">{t.title}</h1>
    </header>
  );
  const alert = error && (
    <p role="alert" className="text-sm font-medium text-(--mesa-danger)">
      {error}
    </p>
  );

  if (state.closed) {
    return (
      <>
        {header}
        <TabClosed />
      </>
    );
  }

  const onTheWay =
    orders?.orders
      .filter((o) => o.status !== "REJECTED")
      .flatMap((o) => o.items)
      .filter((i) => ["PENDING", "PREPARING", "READY"].includes(i.status))
      .reduce((sum, i) => sum + i.quantity, 0) ?? 0;
  const paid = (orders?.paidCents ?? 0) > 0;
  const total = orders && (
    <section className="flex flex-col gap-2 rounded-3xl border border-(--mesa-stroke) bg-(--mesa-glass) p-4.5">
      <span className="text-sm font-semibold tracking-wide text-(--brand-text) uppercase">
        {paid ? t.due : t.total}
      </span>
      <span className="text-4xl font-bold tracking-tight">
        {formatCents(paid ? orders.dueCents : orders.totalCents)}
      </span>
      {paid && (
        <span className="flex flex-wrap gap-2 text-sm">
          <span className="rounded-full border border-(--mesa-stroke) px-2.5 py-0.5 text-(--mesa-muted)">
            {t.consumed(formatCents(orders.totalCents))}
          </span>
          <span className="rounded-full bg-(--mesa-ok-soft) px-2.5 py-0.5 font-semibold text-(--mesa-ok)">
            {t.alreadyPaid(formatCents(orders.paidCents))}
          </span>
        </span>
      )}
      {orders.rodizio && (
        <span className="text-sm text-(--mesa-muted)">
          {tablePage.rodizio.billLine(orders.rodizio.name, orders.rodizio.people)} ·{" "}
          {formatCents(orders.rodizio.totalCents)}
        </span>
      )}
      {orders.orders.length === 0 ? (
        <span className="text-sm text-(--mesa-muted)">{t.noOrders}</span>
      ) : (
        <Link
          href={`/m/${token}/pedidos`}
          className="w-fit rounded text-sm font-semibold text-(--brand-text) focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
        >
          {t.seeItems}
        </Link>
      )}
      {onTheWay > 0 && <span className="text-sm text-(--mesa-muted)">{t.onTheWay(onTheWay)}</span>}
    </section>
  );

  if (calls === null) {
    return (
      <>
        {header}
        <p className="text-(--mesa-muted)">{t.loading}</p>
      </>
    );
  }

  if (!state.present) {
    return (
      <>
        {header}
        <PresencePanel token={token} state={state} />
      </>
    );
  }

  if (bill && !changing) {
    const method = bill.paymentMethod ?? "PIX";
    const coming = bill.status === "IN_PROGRESS";
    return (
      <>
        {header}
        <section role="status" className="flex flex-col items-center gap-5 pt-4 text-center">
          <span className="flex size-28 items-center justify-center rounded-full bg-(--mesa-glass)">
            <span className="flex size-18 items-center justify-center rounded-full bg-(--brand) text-white">
              <Receipt aria-hidden className="size-8" />
            </span>
          </span>
          <div className="flex flex-col gap-2">
            <h2 className="text-3xl font-bold tracking-tight">{t.requestedTitle}</h2>
            <p className="text-(--mesa-muted)">{t.requestedText[method]}</p>
          </div>
        </section>
        {total}
        <section className="flex flex-col gap-3 rounded-3xl border border-(--mesa-stroke) bg-(--mesa-glass) p-4.5">
          <div className="flex justify-between gap-3">
            <span className="text-(--mesa-muted)">{t.method}</span>
            <span className="font-semibold">{t.methods[method].name}</span>
          </div>
          <div className="h-px bg-(--mesa-stroke)" />
          <span className="w-fit rounded-full border border-(--mesa-stroke) px-3 py-1 text-sm font-semibold text-(--brand-text)">
            {coming ? t.coming(bill.waiterFirstName) : t.waiting}
          </span>
        </section>
        {alert}
        <div className="flex flex-col gap-2.5">
          <button
            type="button"
            disabled={busy !== null}
            onClick={() => {
              setChosen(method);
              setChanging(true);
            }}
            className="h-13 rounded-2xl border border-(--mesa-stroke) bg-(--mesa-glass) font-semibold focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none disabled:opacity-60"
          >
            {t.changeMethod}
          </button>
          <button
            type="button"
            disabled={busy !== null}
            onClick={() => run("cancel", () => cancelCall(token, bill.id))}
            className="h-12 rounded-2xl text-sm font-semibold text-(--mesa-muted) focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none disabled:opacity-60"
          >
            {t.cancel}
          </button>
          {reviewUrl && (
            <a
              href={reviewUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex h-12 items-center justify-center gap-2 text-sm font-semibold text-(--mesa-muted) focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
            >
              <Star aria-hidden className="size-4" />
              {tablePage.review}
              <span className="sr-only">{tablePage.newTab}</span>
            </a>
          )}
        </div>
      </>
    );
  }

  // Tudo pago (a mesa pagou com itens a caminho): não há conta a pedir até pedirem mais.
  if (orders && paid && orders.dueCents === 0 && !changing) {
    return (
      <>
        {header}
        <section role="status" className="flex flex-col items-center gap-5 pt-4 text-center">
          <span className="flex size-28 items-center justify-center rounded-full bg-(--mesa-ok-soft)">
            <span className="flex size-18 items-center justify-center rounded-full bg-(--mesa-ok) text-white">
              <Check aria-hidden className="size-8" />
            </span>
          </span>
          <div className="flex flex-col gap-2">
            <h2 className="text-3xl font-bold tracking-tight">{t.paidTitle}</h2>
            <p className="text-(--mesa-muted)">{t.paidText}</p>
          </div>
        </section>
        {total}
      </>
    );
  }

  return (
    <>
      {header}
      {total}
      <fieldset className="flex flex-col gap-2.5">
        <legend className="pb-2.5 text-lg font-semibold">{t.how}</legend>
        {METHODS.map(({ id, icon }) => (
          <label
            key={id}
            className={cn(
              "relative flex min-h-18 items-center gap-3.5 rounded-2xl border border-(--mesa-stroke) bg-(--mesa-glass) p-3.5 has-focus-visible:ring-3 has-focus-visible:ring-(--brand)",
              chosen === id && "border-(--brand)",
            )}
          >
            {/* Rádio de verdade, invisível por cima do cartão inteiro: o toque em qualquer ponto vale. */}
            <input
              type="radio"
              name="payment"
              value={id}
              checked={chosen === id}
              onChange={() => setChosen(id)}
              className="absolute inset-0 size-full cursor-pointer opacity-0"
            />
            <span className="flex size-11 shrink-0 items-center justify-center rounded-xl border border-(--mesa-stroke)">
              {icon}
            </span>
            <span className="flex flex-1 flex-col gap-0.5">
              <span className="font-semibold">{t.methods[id].name}</span>
              <span className="text-sm text-(--mesa-muted)">{t.methods[id].description}</span>
            </span>
            <span
              aria-hidden
              className={cn(
                "size-5 shrink-0 rounded-full border-2 border-(--mesa-stroke)",
                chosen === id && "border-6 border-(--brand)",
              )}
            />
          </label>
        ))}
      </fieldset>
      {alert}
      <div className="flex flex-col gap-2.5">
        <button
          type="button"
          disabled={busy !== null}
          onClick={() =>
            run("bill", async () => {
              await requestCall(token, { type: "BILL", paymentMethod: chosen });
              setChanging(false);
            })
          }
          className="h-14 rounded-2xl bg-(--brand) font-semibold text-white focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:ring-offset-2 focus-visible:ring-offset-(--mesa-bg) focus-visible:outline-none disabled:opacity-80"
        >
          {busy === "bill" ? t.sending : changing ? t.change : t.submit}
        </button>
        {changing && (
          <button
            type="button"
            onClick={() => setChanging(false)}
            className="h-12 rounded-2xl text-sm font-semibold text-(--mesa-muted) focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
          >
            {t.keep}
          </button>
        )}
        <p className="text-center text-sm text-(--mesa-muted)">{t.payAtTable}</p>
      </div>
    </>
  );
}
