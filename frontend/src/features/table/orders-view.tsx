"use client";

import Link from "next/link";
import { useState } from "react";
import { formatCents } from "@/lib/format";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";
import { TabClosed } from "./tab-closed";
import { timeOf, useTableOrders, type TableOrder, type TableOrderItem } from "./table-orders";

const t = tablePage.orders;
const focus = "focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none";

/** As cores de cada passo: na fila (neutro), em preparo e pronto (marca), entregue e cancelado (apagados). */
const BADGE: Record<TableOrderItem["status"], string> = {
  PENDING: "border-(--mesa-stroke)",
  PREPARING: "border-(--brand-text) text-(--brand-text)",
  READY: "border-(--brand) bg-(--brand) text-white",
  DELIVERED: "border-(--mesa-stroke) text-(--mesa-muted)",
  CANCELED: "border-(--mesa-stroke) text-(--mesa-muted)",
};
const STEPS: TableOrderItem["status"][] = ["PENDING", "PREPARING", "READY", "DELIVERED"];
/** O que "A caminho" mostra: o que ainda não chegou, e o cancelado, para o cliente ver o motivo. */
const ON_THE_WAY_VIEW: TableOrderItem["status"][] = ["PENDING", "PREPARING", "READY", "CANCELED"];

/**
 * "Meus pedidos" (desenhos Pedidos a caminho e Histórico): os pedidos da comanda aberta, de todos
 * os celulares da mesa, com o passo de cada item, o total parcial e o "Cancelar item" (só nos itens
 * deste celular que ainda estão na fila). "A caminho" é o histórico só com o que ainda não chegou
 * (especificação). O tempo real atualiza na hora; quando o garçom fecha a conta, a tela agradece.
 */
export function OrdersView({ token, tableLabel }: { token: string; tableLabel: string }) {
  const { data, busy, error, closed, cancel } = useTableOrders(token);
  const [view, setView] = useState<"onTheWay" | "history">("onTheWay");
  // Do mais novo para o mais antigo: o que acabou de pedir aparece primeiro.
  const all = data ? [...data.orders].reverse() : [];
  // "A caminho": só os pedidos com algum item que ainda não chegou (ou cancelado), e só esses itens.
  const orders =
    view === "history"
      ? all
      : all
          .map((o) => ({ ...o, items: o.items.filter((i) => ON_THE_WAY_VIEW.includes(i.status)) }))
          .filter((o) => o.items.length > 0);

  if (closed) return <TabClosed />;

  return (
    <>
      <header className="flex items-end justify-between gap-3">
        <div className="flex flex-col gap-0.5">
          <span className="text-sm text-(--mesa-muted)">
            {tablePage.where(tableLabel, "")}
            {all.length > 0 && ` · ${t.open}`}
          </span>
          <h1 className="text-3xl font-bold tracking-tight">{t.title}</h1>
        </div>
        {data && all.length > 0 && (
          <span className="flex flex-col text-right text-sm text-(--mesa-muted)">
            {t.partial}
            <span className="text-lg font-bold text-(--mesa-fg)">{formatCents(data.totalCents)}</span>
          </span>
        )}
      </header>

      {error && (
        <p role="alert" className="text-sm font-medium text-(--brand-text)">
          {error}
        </p>
      )}

      {data?.rodizio && (
        <p className="flex items-center justify-between gap-3 rounded-2xl border border-(--mesa-stroke) bg-(--mesa-glass) px-4 py-3 text-sm">
          <span className="font-semibold">{tablePage.rodizio.banner(data.rodizio.name, data.rodizio.people)}</span>
          <span className="font-bold">{formatCents(data.rodizio.totalCents)}</span>
        </p>
      )}

      {data && all.length > 0 && (
        <div role="group" aria-label={t.views} className="grid grid-cols-2 gap-1 rounded-2xl border border-(--mesa-stroke) bg-(--mesa-glass) p-1">
          {(["onTheWay", "history"] as const).map((v) => (
            <button
              key={v}
              type="button"
              aria-pressed={view === v}
              onClick={() => setView(v)}
              className={cn(
                "h-11 rounded-xl text-sm font-semibold text-(--mesa-muted)",
                focus,
                view === v && "bg-(--mesa-fg) text-(--mesa-bg)",
              )}
            >
              {t[v]}
            </button>
          ))}
        </div>
      )}

      {data === null ? (
        !error && <p className="text-(--mesa-muted)">{t.loading}</p>
      ) : all.length > 0 && orders.length === 0 ? (
        <p className="text-center text-(--mesa-muted)">{t.nothingOnTheWay}</p>
      ) : orders.length === 0 ? (
        <section className="flex flex-col items-center gap-3 pt-10 text-center">
          <p className="text-lg font-semibold">{t.empty}</p>
          <p className="text-(--mesa-muted)">{t.emptyHint}</p>
          <Link
            href={`/m/${token}/cardapio`}
            className="mt-4 flex h-14 w-full items-center justify-center rounded-2xl bg-(--brand) font-semibold text-white focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:ring-offset-2 focus-visible:ring-offset-(--mesa-bg) focus-visible:outline-none"
          >
            {t.seeMenu}
          </Link>
        </section>
      ) : (
        <>
          {orders.map((order) => (
            <Order key={order.id} order={order} busy={busy} onCancel={cancel} />
          ))}
          <p className="text-center text-sm text-(--mesa-muted)">{t.allPhones}</p>
        </>
      )}
    </>
  );
}

function Order({ order, busy, onCancel }: { order: TableOrder; busy: string | null; onCancel: (id: string) => void }) {
  const title = t.number(order.number, timeOf(order.createdAt));
  const waiting = order.status === "PENDING_APPROVAL";
  const rejected = order.status === "REJECTED";
  return (
    <section aria-label={title} className="flex flex-col gap-1 rounded-[22px] border border-(--mesa-stroke) bg-(--mesa-glass) p-3.5">
      <div className="flex justify-between gap-2 text-sm text-(--mesa-muted)">
        <h2>{title}</h2>
        <span>{order.mine ? t.mine : t.other}</span>
      </div>
      {(waiting || rejected) && (
        <span className="w-fit rounded-full border border-(--brand-text) px-2.5 py-0.5 text-xs font-semibold text-(--brand-text)">
          {t.orderStatus[order.status as "PENDING_APPROVAL" | "REJECTED"]}
        </span>
      )}
      <ul className="flex flex-col">
        {order.items.map((item) => {
          const canceled = item.status === "CANCELED" || rejected;
          const step = STEPS.indexOf(item.status);
          return (
            <li key={item.id} className="flex flex-col gap-1.5 border-b border-(--mesa-stroke) py-2.5 last:border-b-0">
              <div className="flex items-start justify-between gap-3">
                <span className={cn("flex min-w-0 flex-col", canceled && "text-(--mesa-muted) line-through")}>
                  <span className="font-semibold">
                    {item.quantity}× {item.productName}
                  </span>
                  {item.rodizio && (
                    <span className="text-xs font-semibold tracking-wide text-(--brand-text) uppercase">
                      {tablePage.rodizio.item}
                    </span>
                  )}
                  {item.note && <span className="text-sm text-(--mesa-muted)">{item.note}</span>}
                </span>
                {!waiting && !rejected && (
                  <span
                    className={cn(
                      "shrink-0 rounded-full border px-2.5 py-0.5 text-xs font-semibold",
                      BADGE[item.status],
                    )}
                  >
                    {t.status[item.status]}
                  </span>
                )}
              </div>
              {item.status === "CANCELED" && item.cancelReason && (
                <span className="text-sm text-(--mesa-muted)">{item.cancelReason}</span>
              )}
              {!canceled && !waiting && (
                <span aria-hidden className="flex gap-1">
                  {STEPS.map((s, i) => (
                    <span key={s} className={cn("h-1 flex-1 rounded-full bg-(--mesa-stroke)", i <= step && "bg-(--brand-text)")} />
                  ))}
                </span>
              )}
              {order.mine && item.status === "PENDING" && !rejected && (
                <button
                  type="button"
                  disabled={busy !== null}
                  aria-label={t.cancelNamed(item.productName)}
                  onClick={() => onCancel(item.id)}
                  className={cn("h-10 w-fit rounded-lg text-sm font-semibold text-(--brand-text) disabled:opacity-60", focus)}
                >
                  {busy === item.id ? t.canceling : t.cancel}
                </button>
              )}
            </li>
          );
        })}
      </ul>
    </section>
  );
}
