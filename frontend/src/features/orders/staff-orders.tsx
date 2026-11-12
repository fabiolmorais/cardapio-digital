"use client";

import { useQueryClient } from "@tanstack/react-query";
import { ChefHat, ChevronRight, Flame } from "lucide-react";
import Link from "next/link";
import { useCallback, useState } from "react";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { Button } from "@/components/ui/button";
import { playChime } from "@/features/calls/alert-sound";
import { GREEN, useNow } from "@/features/calls/call-queue";
import { Badge, ColumnHead, TableBox } from "@/features/live/live-ui";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import {
  fetchKitchenQueue,
  fetchWaitingOrders,
  reviewOrder,
  stepItem,
  type KitchenItem,
  type WaitingOrder,
} from "./orders-api";
import { useLiveOrders } from "./use-live-orders";

const t = messages.panel;

/** O atalho para a tela da cozinha (para a cozinha, é o principal do painel). */
export function KitchenLink() {
  return (
    <Link
      href="/painel/cozinha"
      className="flex min-h-16 items-center gap-4 rounded-2xl border border-border bg-card p-5 transition-colors hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
    >
      <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-primary/15 text-brand-text">
        <ChefHat aria-hidden className="size-6" />
      </span>
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="text-lg font-semibold">{t.kitchen.open}</span>
        <span className="text-sm text-muted-foreground">{t.kitchen.openHint}</span>
      </span>
      <ChevronRight aria-hidden className="size-5 text-muted-foreground" />
    </Link>
  );
}

/** O atalho para a tela dos passadores (rodízio de passagem). */
export function PassagemLink() {
  return (
    <Link
      href="/painel/passadores"
      className="flex min-h-16 items-center gap-4 rounded-2xl border border-border bg-card p-5 transition-colors hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
    >
      <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-primary/15 text-brand-text">
        <Flame aria-hidden className="size-6" />
      </span>
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="text-lg font-semibold">{messages.panel.passagem.open}</span>
        <span className="text-sm text-muted-foreground">{messages.panel.passagem.openHint}</span>
      </span>
      <ChevronRight aria-hidden className="size-5 text-muted-foreground" />
    </Link>
  );
}

/** A fila inteira da cozinha (na fila, em preparo e prontos): a mesma da tela da cozinha. */
export const KITCHEN_KEY = ["kitchen-queue"] as const;
export const WAITING_KEY = ["waiting-orders"] as const;

/** Um pedido na cozinha, juntando os itens dele (a fila vem item por item). */
type KitchenOrder = { id: string; number: number; tableLabel: string; sectorName: string; items: KitchenItem[] };

function byOrder(items: KitchenItem[]): KitchenOrder[] {
  const orders = new Map<string, KitchenOrder>();
  for (const item of items) {
    const order = orders.get(item.orderId) ?? {
      id: item.orderId,
      number: item.orderNumber,
      tableLabel: item.tableLabel,
      sectorName: item.sectorName,
      items: [],
    };
    order.items.push(item);
    orders.set(item.orderId, order);
  }
  return [...orders.values()];
}

/**
 * A coluna "Pedidos" do Ao vivo (desenho 7.1): primeiro os pedidos parados na confirmação do
 * garçom (com os itens, os valores, "Confirmar e enviar" e "Recusar"), depois os que estão na
 * cozinha, um cartão por pedido. Com `sector`, só daquele setor. Toca o alerta a cada pedido
 * esperando confirmação.
 */
export function OrdersColumn({ sector = null, canReview }: { sector?: string | null; canReview: boolean }) {
  const queryClient = useQueryClient();
  const waiting = useLiveOrders(WAITING_KEY, fetchWaitingOrders, useCallback(() => playChime(), []), canReview);
  const kitchen = useLiveOrders(KITCHEN_KEY, () => fetchKitchenQueue());
  const now = useNow();
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [rejecting, setRejecting] = useState<WaitingOrder | null>(null);
  const tt = t.approvals;
  const tl = t.live;
  const inSector = (s: string) => sector === null || s === sector;

  async function approve(id: string) {
    setBusy(id);
    setError(null);
    try {
      await reviewOrder(id, true);
    } catch (e) {
      setError((e as Error).message);
    }
    await queryClient.invalidateQueries({ queryKey: WAITING_KEY });
    setBusy(null);
  }

  const toReview = (waiting.list.data ?? []).filter((o) => inSector(o.sectorName));
  const cooking = byOrder(
    (kitchen.list.data ?? []).filter((i) => i.status !== "READY" && inSector(i.sectorName)),
  );

  return (
    <section aria-labelledby="pedidos" className="flex min-w-0 flex-col rounded-[20px] border border-border bg-card">
      <ColumnHead id="pedidos" title={tl.orders} count={toReview.length + cooking.length} />
      <div className="flex flex-col gap-2.5 px-3 pb-3">
        {error && (
          <p role="alert" className="px-1 text-sm text-destructive">
            {error}
          </p>
        )}
        {toReview.length === 0 && cooking.length === 0 && (
          <p className="px-1 pb-2 text-sm text-muted-foreground">{tl.noOrders}</p>
        )}
        {toReview.length > 0 && (
          <ul aria-label={tt.title} className="flex flex-col gap-2.5">
            {toReview.map((order) => (
              <li
                key={order.id}
                aria-label={tt.order(order.tableLabel, order.number)}
                className="flex flex-col gap-3 rounded-2xl border border-brand-text/50 bg-muted p-3.5 ring-3 ring-primary/15"
              >
                <div className="flex items-center gap-3">
                  <TableBox label={order.tableLabel} />
                  <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                    <span className="font-semibold">{tl.order(order.number)}</span>
                    <span className="truncate text-[13px] text-muted-foreground">
                      {order.sectorName} · {t.kitchen.since((now - Date.parse(order.createdAt)) / 1000)}
                    </span>
                  </span>
                  <Badge tone="accent" plain>
                    {tl.toConfirm}
                  </Badge>
                </div>
                <ul className="flex flex-col gap-1.5 rounded-xl bg-secondary px-3 py-2.5 text-[13px]">
                  {order.items
                    .filter((i) => i.status !== "CANCELED")
                    .map((i) => (
                      <li key={i.id} className="flex justify-between gap-3">
                        <span>
                          {i.quantity}× {i.productName}
                          {i.note && <span className="text-muted-foreground"> · {i.note}</span>}
                        </span>
                        <span className="shrink-0 text-muted-foreground">
                          {formatCents(i.unitPriceCents * i.quantity)}
                        </span>
                      </li>
                    ))}
                  <li className="flex justify-between gap-3 border-t border-border pt-1.5 font-semibold">
                    <span>{tl.total}</span>
                    <span>{formatCents(order.totalCents)}</span>
                  </li>
                </ul>
                <span className="text-[13px] text-warning">{tt.subtitle}</span>
                <div className="flex gap-2">
                  <Button size="sm" className="h-9 flex-1" disabled={busy !== null} onClick={() => approve(order.id)}>
                    {tt.approve}
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    className="h-9 border-destructive/35 bg-transparent text-destructive"
                    disabled={busy !== null}
                    onClick={() => setRejecting(order)}
                  >
                    {tt.reject}
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
        {cooking.length > 0 && (
          <ul aria-label={tl.inKitchen} className="flex flex-col gap-2.5">
            {cooking.map((order) => {
              const count = order.items.reduce((sum, i) => sum + i.quantity, 0);
              return (
                <li
                  key={order.id}
                  aria-label={tt.order(order.tableLabel, order.number)}
                  className="flex flex-col gap-3 rounded-2xl border border-border bg-muted p-3.5"
                >
                  <div className="flex items-center gap-3">
                    <TableBox label={order.tableLabel} />
                    <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                      <span className="font-semibold">{tl.order(order.number)}</span>
                      <span className="truncate text-[13px] text-muted-foreground">
                        {order.sectorName} · {tl.items(count)}
                      </span>
                    </span>
                    <Badge tone="warn">{tl.inKitchen}</Badge>
                  </div>
                  <ul className="flex flex-col gap-1 text-[13px] text-muted-foreground">
                    {order.items.map((i) => (
                      <li key={i.id} className="flex justify-between gap-2">
                        <span>
                          {i.quantity}× {i.productName}
                          {i.note && ` · ${i.note}`}
                        </span>
                        <span className={i.status === "PREPARING" ? "text-warning" : undefined}>
                          {t.kitchenStatus[i.status as "PENDING" | "PREPARING"]}
                        </span>
                      </li>
                    ))}
                  </ul>
                </li>
              );
            })}
          </ul>
        )}
      </div>
      <ConfirmDialog
        open={rejecting !== null}
        onOpenChange={(open) => !open && setRejecting(null)}
        title={rejecting ? tt.rejectTitle(rejecting.tableLabel) : ""}
        description={tt.rejectText}
        confirmLabel={tt.rejectConfirm}
        onConfirm={async () => {
          if (!rejecting) return;
          await reviewOrder(rejecting.id, false);
          await queryClient.invalidateQueries({ queryKey: WAITING_KEY });
        }}
      />
    </section>
  );
}

/**
 * A coluna "Prontos para entregar" do Ao vivo (desenho 7.1 e 10.4): os itens prontos na cozinha,
 * com a mesa, o setor, há quanto tempo estão prontos e o "Entregue". A cozinha só acompanha.
 */
export function ReadyColumn({ sector = null, canDeliver }: { sector?: string | null; canDeliver: boolean }) {
  const queryClient = useQueryClient();
  const { list } = useLiveOrders(KITCHEN_KEY, () => fetchKitchenQueue());
  const now = useNow();
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const tt = t.deliveries;

  async function deliver(id: string) {
    setBusy(id);
    setError(null);
    try {
      await stepItem(id, "deliver");
    } catch (e) {
      setError((e as Error).message);
    }
    await queryClient.invalidateQueries({ queryKey: KITCHEN_KEY });
    setBusy(null);
  }

  const items = (list.data ?? []).filter((i) => i.status === "READY" && (sector === null || i.sectorName === sector));

  return (
    <section aria-labelledby="prontos" className="flex min-w-0 flex-col rounded-[20px] border border-border bg-card">
      <ColumnHead id="prontos" title={tt.title} count={items.length} tone={items.length > 0 ? "info" : undefined} />
      <div className="flex flex-col gap-2.5 px-3 pb-3">
        {error && (
          <p role="alert" className="px-1 text-sm text-destructive">
            {error}
          </p>
        )}
        {list.data && items.length === 0 ? (
          <p className="px-1 pb-2 text-sm text-muted-foreground">{tt.empty}</p>
        ) : (
          <ul className="flex flex-col gap-2.5">
            {items.map((item) => {
              const name = `${item.quantity}× ${item.productName}`;
              return (
                <li
                  key={item.id}
                  aria-label={`${t.calls.table} ${item.tableLabel} · ${name}`}
                  className="flex items-center gap-3 rounded-2xl border border-border bg-muted p-3.5"
                >
                  <TableBox label={item.tableLabel} />
                  <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                    <span className="font-semibold">{name}</span>
                    <span className="truncate text-[13px] text-muted-foreground">
                      {item.sectorName} ·{" "}
                      {tt.readyFor(t.kitchen.since((now - Date.parse(item.readyAt ?? item.createdAt)) / 1000))}
                    </span>
                  </span>
                  {canDeliver && (
                    <Button size="sm" className={GREEN} disabled={busy !== null} onClick={() => deliver(item.id)}>
                      {busy === item.id ? tt.delivering : tt.deliver}
                    </Button>
                  )}
                </li>
              );
            })}
          </ul>
        )}
        {items.length > 0 && <p className="px-1 text-[13px] text-muted-foreground/80">{tt.hint}</p>}
      </div>
    </section>
  );
}
