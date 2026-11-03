"use client";

import { useQueryClient } from "@tanstack/react-query";
import { ChefHat, ChevronRight } from "lucide-react";
import Link from "next/link";
import { useCallback, useState } from "react";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { Button } from "@/components/ui/button";
import { playChime } from "@/features/calls/alert-sound";
import { useNow } from "@/features/calls/call-queue";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { fetchKitchenQueue, fetchWaitingOrders, reviewOrder, stepItem, type WaitingOrder } from "./orders-api";
import { useLiveOrders } from "./use-live-orders";

const t = messages.panel;
const READY_KEY = ["kitchen-queue", "READY"] as const;
const WAITING_KEY = ["waiting-orders"] as const;

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

/**
 * Os pedidos parados na confirmação do garçom: a mesa, o total e os itens, com "Confirmar" (vai
 * para a cozinha) e "Recusar". Some quando não há nenhum. Toca o alerta a cada pedido novo.
 */
export function WaitingOrders() {
  const queryClient = useQueryClient();
  const { list } = useLiveOrders(WAITING_KEY, fetchWaitingOrders, useCallback(() => playChime(), []));
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [rejecting, setRejecting] = useState<WaitingOrder | null>(null);
  const tt = t.approvals;

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

  const orders = list.data ?? [];
  if (orders.length === 0) return null;

  return (
    <section aria-labelledby="confirmar" className="flex flex-col gap-3 rounded-2xl border border-brand-text/40 bg-card p-5">
      <div className="flex flex-col gap-0.5">
        <h2 id="confirmar" className="text-xl font-semibold">
          {tt.title} <span className="text-brand-text">({orders.length})</span>
        </h2>
        <p className="text-sm text-muted-foreground">{tt.subtitle}</p>
      </div>
      {error && (
        <p role="alert" className="text-sm text-destructive">
          {error}
        </p>
      )}
      <ul className="flex flex-col gap-2.5">
        {orders.map((order) => {
          const title = tt.order(order.tableLabel, order.number);
          return (
            <li key={order.id} aria-label={title} className="flex flex-col gap-3 rounded-xl border border-border bg-background p-3.5">
              <div className="flex items-baseline justify-between gap-3">
                <span className="font-semibold">{title}</span>
                <span className="font-semibold">{formatCents(order.totalCents)}</span>
              </div>
              <ul className="flex flex-col gap-1 text-sm">
                {order.items
                  .filter((i) => i.status !== "CANCELED")
                  .map((i) => (
                    <li key={i.id}>
                      {i.quantity}× {i.productName}
                      {i.note && <span className="text-muted-foreground"> · {i.note}</span>}
                    </li>
                  ))}
              </ul>
              <div className="flex gap-2">
                <Button className="h-11 flex-1" disabled={busy !== null} onClick={() => approve(order.id)}>
                  {tt.approve}
                </Button>
                <Button variant="outline" className="h-11" disabled={busy !== null} onClick={() => setRejecting(order)}>
                  {tt.reject}
                </Button>
              </div>
            </li>
          );
        })}
      </ul>
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

/** "Entregas" (desenho 10.4): os itens prontos na cozinha, com "Entregue". */
export function Deliveries() {
  const queryClient = useQueryClient();
  const { list } = useLiveOrders(READY_KEY, () => fetchKitchenQueue("READY"));
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
    await queryClient.invalidateQueries({ queryKey: READY_KEY });
    setBusy(null);
  }

  const items = list.data ?? [];

  return (
    <section aria-labelledby="entregas" className="flex flex-col gap-3 rounded-2xl border border-border bg-card p-5">
      <div className="flex flex-col gap-0.5">
        <h2 id="entregas" className="text-xl font-semibold">
          {tt.title}
          {items.length > 0 && <span className="text-brand-text"> ({items.length})</span>}
        </h2>
        <p className="text-sm text-muted-foreground">{tt.subtitle}</p>
      </div>
      {error && (
        <p role="alert" className="text-sm text-destructive">
          {error}
        </p>
      )}
      {list.data && items.length === 0 ? (
        <p className="text-sm text-muted-foreground">{tt.empty}</p>
      ) : (
        <ul className="flex flex-col gap-2.5">
          {items.map((item) => {
            const name = `${item.quantity}× ${item.productName}`;
            return (
              <li
                key={item.id}
                aria-label={`${t.calls.table} ${item.tableLabel} · ${name}`}
                className="flex items-center gap-3 rounded-xl border border-border bg-background p-3"
              >
                <span className="flex size-12 shrink-0 flex-col items-center justify-center rounded-xl bg-muted leading-none">
                  <span className="text-[10px] font-semibold tracking-wide text-muted-foreground uppercase">
                    {t.calls.table}
                  </span>
                  <span className="text-lg font-bold">{item.tableLabel}</span>
                </span>
                <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                  <span className="font-semibold">{name}</span>
                  <span className="text-sm text-muted-foreground">
                    {tt.readyFor(t.kitchen.since((now - Date.parse(item.readyAt ?? item.createdAt)) / 1000))}
                  </span>
                </span>
                <Button
                  className="h-11 bg-emerald-700 text-white hover:bg-emerald-600"
                  disabled={busy !== null}
                  onClick={() => deliver(item.id)}
                >
                  {busy === item.id ? tt.delivering : tt.deliver}
                </Button>
              </li>
            );
          })}
        </ul>
      )}
      <p className="text-sm text-muted-foreground">{tt.hint}</p>
    </section>
  );
}
