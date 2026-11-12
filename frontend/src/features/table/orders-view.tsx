"use client";

import { CircleAlert } from "lucide-react";
import Link from "next/link";
import { useEffect, useState, type ReactNode } from "react";
import { formatCents } from "@/lib/format";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";
import { ItemThumb, StatusBadge } from "./order-status";
import { TabClosed } from "./tab-closed";
import { timeOf, useTableOrders, type TableOrder, type TableOrderItem } from "./table-orders";

const t = tablePage.orders;
const focus = "focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none";

const STEPS: TableOrderItem["status"][] = ["PENDING", "PREPARING", "READY", "DELIVERED"];
/** O que ainda não chegou à mesa: é o que "A caminho" mostra. */
const ON_THE_WAY: TableOrderItem["status"][] = ["PENDING", "PREPARING", "READY"];

/** Um item com o pedido de onde veio (a hora, de quem é, se espera o garçom). */
type Entry = { item: TableOrderItem; order: TableOrder };

/** O relógio da tela, de 30 em 30 segundos: os minutos de cada item andam sozinhos. */
function useNow() {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 30_000);
    return () => window.clearInterval(timer);
  }, []);
  return now;
}

/**
 * "Meus pedidos" (desenhos 4.3 e 4.4), com os pedidos da visita atual de todos os celulares da mesa.
 * - A caminho: um card por item que ainda não chegou, com a foto, há quanto tempo foi pedido, a tag
 *   do passo e a barra de progresso; e o aviso de cada prato que a equipe cancelou.
 * - Histórico: os pedidos, um card por pedido, com todos os itens (entregues e cancelados também) e
 *   o total parcial.
 * O tempo real atualiza na hora; quando o garçom fecha a conta, a tela agradece.
 */
export function OrdersView({ token, tableLabel }: { token: string; tableLabel: string }) {
  const { data, busy, error, closed, cancel } = useTableOrders(token);
  const [view, setView] = useState<"onTheWay" | "history">("onTheWay");
  const now = useNow();

  // Do mais novo para o mais antigo: o que acabou de pedir aparece primeiro.
  const orders = data ? [...data.orders].reverse() : [];
  const entries: Entry[] = orders.flatMap((order) => order.items.map((item) => ({ item, order })));
  const onTheWay = entries.filter(({ item, order }) => order.status !== "REJECTED" && ON_THE_WAY.includes(item.status));
  // O aviso de cada prato cancelado: pela equipe (com o motivo) ou por quem fez o pedido. Os do
  // pedido recusado pelo garçom têm o aviso do pedido.
  const canceled = entries.filter(({ item, order }) => item.status === "CANCELED" && order.status !== "REJECTED");
  const rejected = orders.filter((order) => order.status === "REJECTED");
  // Quanto cada celular pediu (os itens que entram na conta), do 1 (este) em diante.
  const byPhone = new Map<number, number>();
  for (const order of data?.orders ?? []) {
    if (order.status === "REJECTED") continue;
    const sum = order.items
      .filter((i) => i.status !== "CANCELED")
      .reduce((acc, i) => acc + i.unitPriceCents * i.quantity, 0);
    byPhone.set(order.phone, (byPhone.get(order.phone) ?? 0) + sum);
  }
  const phones = [...byPhone.entries()].sort(([a], [b]) => a - b);
  const mineCents = byPhone.get(1) ?? 0;

  if (closed) return <TabClosed />;

  return (
    <>
      <header className="flex items-end justify-between gap-3">
        <div className="flex flex-col gap-0.5">
          <span className="text-sm text-(--mesa-muted)">
            {tablePage.where(tableLabel, "")}
            {orders.length > 0 && ` · ${t.open}`}
          </span>
          <h1 className="text-3xl font-bold tracking-tight">{t.title}</h1>
        </div>
        {data && orders.length > 0 && (
          <span className="flex flex-col text-right text-sm text-(--mesa-muted)">
            {data.paidCents > 0 ? t.due : t.partial}
            <span className="text-lg font-bold text-(--mesa-fg)">
              {formatCents(data.paidCents > 0 ? data.dueCents : data.totalCents)}
            </span>
            {phones.length > 1 && (
              <span className="text-xs">
                {t.thisPhone} · {formatCents(mineCents)}
              </span>
            )}
          </span>
        )}
      </header>

      {error && (
        <p role="alert" className="text-sm font-medium text-(--mesa-danger)">
          {error}
        </p>
      )}

      {data?.rodizio && (
        <p className="flex items-center justify-between gap-3 rounded-2xl border border-(--mesa-stroke) bg-(--mesa-glass) px-4 py-3 text-sm">
          <span className="font-semibold">{tablePage.rodizio.banner(data.rodizio.name, data.rodizio.people)}</span>
          <span className="font-bold">{formatCents(data.rodizio.totalCents)}</span>
        </p>
      )}

      {data && orders.length > 0 && (
        <div
          role="group"
          aria-label={t.views}
          className="grid grid-cols-2 gap-1 rounded-2xl border border-(--mesa-stroke) bg-(--mesa-glass) p-1"
        >
          {(["onTheWay", "history"] as const).map((v) => (
            <button
              key={v}
              type="button"
              aria-pressed={view === v}
              onClick={() => setView(v)}
              className={cn(
                "h-10 rounded-xl text-sm font-semibold text-(--mesa-muted)",
                focus,
                view === v && "bg-(--mesa-fg) text-(--mesa-bg)",
              )}
            >
              {v === "onTheWay" && onTheWay.length > 0 ? t.onTheWayCount(onTheWay.length) : t[v]}
            </button>
          ))}
        </div>
      )}

      {data === null ? (
        !error && <p className="text-(--mesa-muted)">{t.loading}</p>
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
      ) : view === "onTheWay" ? (
        <section aria-label={t.onTheWay} className="flex flex-col gap-2.5">
          {onTheWay.length === 0 && <p className="text-center text-(--mesa-muted)">{t.nothingOnTheWay}</p>}
          {onTheWay.map((entry) => (
            <OnTheWayItem key={entry.item.id} entry={entry} now={now} busy={busy} onCancel={cancel} />
          ))}
          {rejected.map((order) => (
            <Alert key={order.id}>
              <strong>{t.rejectedNotice(order.number)}</strong> {t.rejectedNoCharge}
            </Alert>
          ))}
          {canceled.map(({ item, order }) => {
            const name = item.quantity > 1 ? `${item.quantity}× ${item.productName}` : item.productName;
            return (
              <Alert key={item.id}>
                {item.canceledByCustomer ? (
                  <strong>{order.mine ? t.canceledByYou(name) : t.canceledByOwner(name)}</strong>
                ) : (
                  <>
                    <strong>{t.canceledByTeam(name)}</strong> {item.cancelReason}.
                  </>
                )}{" "}
                {t.canceledNoCharge}
              </Alert>
            );
          })}
          <p className="pt-1 text-center text-sm text-(--mesa-muted)">{t.allPhones}</p>
        </section>
      ) : (
        <section aria-label={t.history} className="flex flex-col gap-3.5">
          {orders.map((order) => (
            <HistoryOrder key={order.id} order={order} />
          ))}
          {data && (
            <div className="flex flex-col gap-2 rounded-[22px] border border-(--mesa-stroke) bg-(--mesa-glass) p-3.5">
              <div className="flex items-center justify-between gap-3">
                <span className="flex flex-col">
                  <span className="font-semibold">{t.partial}</span>
                  <span className="text-sm text-(--mesa-muted)">
                    {phones.length > 1 ? t.phones(phones.length) : t.historyTotalHint}
                  </span>
                </span>
                <span className="text-2xl font-bold">{formatCents(data.totalCents)}</span>
              </div>
              {(phones.length > 1 || data.rodizio) && (
                <ul aria-label={t.byPhone} className="flex flex-col gap-1 border-t border-(--mesa-stroke) pt-2 text-sm">
                  {phones.map(([phone, cents]) => (
                    <li key={phone} className="flex items-center justify-between gap-3">
                      <span className={phone === 1 ? "font-semibold" : "text-(--mesa-muted)"}>
                        {phone === 1 ? t.thisPhone : t.phone(phone)}
                      </span>
                      <span className={phone === 1 ? "font-semibold" : undefined}>{formatCents(cents)}</span>
                    </li>
                  ))}
                  {data.rodizio && (
                    <li className="flex items-center justify-between gap-3">
                      <span className="text-(--mesa-muted)">{t.rodizioShare(data.rodizio.people)}</span>
                      <span>{formatCents(data.rodizio.totalCents)}</span>
                    </li>
                  )}
                </ul>
              )}
              {data.paidCents > 0 && (
                <>
                  <div className="flex items-center justify-between gap-3 border-t border-(--mesa-stroke) pt-2 text-sm">
                    <span className="text-(--mesa-muted)">{t.paid}</span>
                    <span className="font-semibold text-(--mesa-ok)">− {formatCents(data.paidCents)}</span>
                  </div>
                  <div className="flex items-center justify-between gap-3 text-sm">
                    <span className="font-semibold">{t.due}</span>
                    <span className="font-bold">{formatCents(data.dueCents)}</span>
                  </div>
                </>
              )}
            </div>
          )}
        </section>
      )}
    </>
  );
}

/**
 * Um item a caminho (desenho 4.3): a foto, "1× Prato" com os minutos ao lado, a observação, a tag
 * do passo (e o "Cancelar item", se é deste celular e ainda está na fila) e a barra dos passos.
 */
function OnTheWayItem({
  entry: { item, order },
  now,
  busy,
  onCancel,
}: {
  entry: Entry;
  now: number;
  busy: string | null;
  onCancel: (id: string) => void;
}) {
  const waiting = order.status === "PENDING_APPROVAL";
  const step = STEPS.indexOf(item.status);
  const minutes = Math.max(0, Math.floor((now - Date.parse(order.createdAt)) / 60_000));
  const label = waiting
    ? t.orderStatus.PENDING_APPROVAL
    : item.status === "READY"
      ? t.readyOnTheWay
      : t.status[item.status];
  return (
    <article
      aria-label={`${item.quantity}× ${item.productName}`}
      className="flex gap-3 rounded-[22px] border border-(--mesa-stroke) bg-(--mesa-glass) p-3"
    >
      <ItemThumb src={item.thumbUrl} size="md" />
      <div className="flex min-w-0 flex-1 flex-col">
        <div className="flex items-start justify-between gap-2">
          <span className="font-semibold">
            {item.quantity}× {item.productName}
          </span>
          <span className="shrink-0 pt-0.5 text-sm text-(--mesa-muted)">{t.minutes(minutes)}</span>
        </div>
        {item.rodizio && (
          <span className="text-xs font-semibold tracking-wide text-(--brand-text) uppercase">
            {tablePage.rodizio.item}
          </span>
        )}
        {item.note && <span className="text-sm text-(--mesa-muted)">{item.note}</span>}
        <div className="mt-2 flex items-center justify-between gap-2">
          <StatusBadge tone={waiting ? "PENDING" : item.status}>{label}</StatusBadge>
          {order.mine && item.status === "PENDING" && !waiting && (
            <button
              type="button"
              disabled={busy !== null}
              aria-label={t.cancelNamed(item.productName)}
              onClick={() => onCancel(item.id)}
              className={cn(
                "-my-2 h-10 rounded-lg px-1 text-[13px] font-semibold text-(--mesa-danger) disabled:opacity-60",
                focus,
              )}
            >
              {busy === item.id ? t.canceling : t.cancel}
            </button>
          )}
        </div>
        {!waiting && (
          <span aria-hidden className="mt-2 flex gap-1">
            {STEPS.map((s, i) => (
              <span
                key={s}
                className={cn("h-1 flex-1 rounded-full bg-(--mesa-stroke)", i <= step && "bg-(--brand-text)")}
              />
            ))}
          </span>
        )}
      </div>
    </article>
  );
}

/** O aviso vermelho de "A caminho" (desenho 4.3): prato cancelado, pedido recusado. */
function Alert({ children }: { children: ReactNode }) {
  return (
    <p className="flex items-start gap-2.5 rounded-[14px] bg-(--mesa-danger-soft) px-3.5 py-3 text-[13px] leading-[18px]">
      <CircleAlert aria-hidden className="mt-px size-4.5 shrink-0 text-(--mesa-danger)" />
      <span>{children}</span>
    </p>
  );
}

/** Um pedido no histórico (desenho 4.4): o número, a hora, de qual celular, e cada item com a tag. */
function HistoryOrder({ order }: { order: TableOrder }) {
  const title = t.number(order.number, timeOf(order.createdAt));
  const waiting = order.status === "PENDING_APPROVAL";
  const rejected = order.status === "REJECTED";
  return (
    <section
      aria-label={title}
      className="flex flex-col rounded-[22px] border border-(--mesa-stroke) bg-(--mesa-glass) px-3.5 py-3"
    >
      <div className="flex justify-between gap-2 text-sm text-(--mesa-muted)">
        <h2>{title}</h2>
        <span>{order.mine ? t.mine : t.phone(order.phone)}</span>
      </div>
      <ul className="flex flex-col">
        {order.items.map((item) => {
          const canceled = item.status === "CANCELED" || rejected;
          return (
            <li key={item.id} className="flex items-center gap-3 py-2">
              <ItemThumb src={item.thumbUrl} size="sm" />
              <span className={cn("min-w-0 flex-1 text-sm", canceled && "text-(--mesa-muted) line-through")}>
                {item.quantity}× {item.productName}
              </span>
              {rejected ? (
                <StatusBadge tone="CANCELED">{t.orderStatus.REJECTED}</StatusBadge>
              ) : waiting ? (
                <StatusBadge tone="PENDING">{t.orderStatus.PENDING_APPROVAL}</StatusBadge>
              ) : (
                <StatusBadge tone={item.status}>{t.status[item.status]}</StatusBadge>
              )}
            </li>
          );
        })}
      </ul>
    </section>
  );
}
