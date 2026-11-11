"use client";

import { useQueryClient } from "@tanstack/react-query";
import { ChefHat, ChevronLeft, X } from "lucide-react";
import Link from "next/link";
import { useCallback, useState } from "react";
import { Button, buttonVariants } from "@/components/ui/button";
import { playChime } from "@/features/calls/alert-sound";
import { SoundToggle } from "@/features/calls/sound-toggle";
import { elapsed, useNow } from "@/features/calls/call-queue";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { CancelItemDialog } from "./cancel-item-dialog";
import { fetchKitchenQueue, stepItem, type ItemStep, type KitchenItem } from "./orders-api";
import { useLiveOrders } from "./use-live-orders";
import { useWakeLock } from "@/lib/pwa/use-wake-lock";

const t = messages.panel.kitchen;
const tc = messages.panel.calls;
type Lane = "PENDING" | "PREPARING" | "READY";
const LANES: Lane[] = ["PENDING", "PREPARING", "READY"];
const QUEUE_KEY = ["kitchen-queue"] as const;

/** Até 5 min, normal; até 10 min, atenção; depois, atrasado (desenho 9.1). */
const WARN_AFTER_S = 300;
const LATE_AFTER_S = 600;

/** Uma ficha: os itens de um pedido que estão no mesmo passo. */
type Ticket = { key: string; lane: Lane; tableLabel: string; orderNumber: number; items: KitchenItem[] };

/** Junta a fila (já por ordem de chegada) em fichas por pedido e por passo, mantendo a ordem. */
function tickets(queue: KitchenItem[]): Ticket[] {
  const byKey = new Map<string, Ticket>();
  for (const item of queue) {
    const lane = item.status as Lane;
    const key = `${item.orderId}|${lane}`;
    const ticket = byKey.get(key);
    if (ticket) ticket.items.push(item);
    else byKey.set(key, { key, lane, tableLabel: item.tableLabel, orderNumber: item.orderNumber, items: [item] });
  }
  return [...byKey.values()];
}

/**
 * A tela da cozinha (desenho 9.1): três colunas (na fila, em preparo, pronto), uma ficha por
 * pedido. "Começar" põe a ficha em preparo; em preparo, cada toque num item marca ele pronto, e
 * "Tudo pronto" marca a ficha inteira. Atualiza pelo tempo real e toca o alerta a cada item novo.
 */
export function KitchenBoard() {
  const { status } = useRequiredSession();
  // O quadro (com o relógio e a fila) só monta com a sessão pronta, depois de um F5 também.
  if (status !== "authenticated") return <p className="p-6 text-muted-foreground">{t.loading}</p>;
  return <Board />;
}

function Board() {
  const queryClient = useQueryClient();
  useWakeLock();
  const { list, connected } = useLiveOrders(
    QUEUE_KEY,
    () => fetchKitchenQueue(),
    useCallback(() => playChime(), []),
  );
  const now = useNow();
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [canceling, setCanceling] = useState<KitchenItem[] | null>(null);

  async function run(key: string, items: KitchenItem[], step: ItemStep) {
    setBusy(key);
    setError(null);
    const results = await Promise.allSettled(items.map((i) => stepItem(i.id, step)));
    const failed = results.find((r) => r.status === "rejected");
    // 409: alguém mudou o item antes (o cliente cancelou, por exemplo). A fila atualizada mostra.
    if (failed) setError((failed.reason as Error).message);
    await queryClient.invalidateQueries({ queryKey: QUEUE_KEY });
    setBusy(null);
  }

  const all = tickets(list.data ?? []);

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="flex flex-wrap items-center gap-3 border-b border-border px-4 py-3 sm:px-6">
        <Link href="/painel" aria-label={t.back} className={buttonVariants({ variant: "outline", size: "icon-lg" })}>
          <ChevronLeft aria-hidden />
        </Link>
        <span className="flex size-11 items-center justify-center rounded-xl bg-muted">
          <ChefHat aria-hidden className="size-6" />
        </span>
        <span className="mr-auto flex flex-col">
          <h1 className="text-xl font-bold">{t.title}</h1>
          <span className="text-sm text-muted-foreground">{t.subtitle}</span>
        </span>
        <SoundToggle />
        <span
          role="status"
          className={cn("flex items-center gap-1.5 text-sm", connected ? "text-success" : "text-muted-foreground")}
        >
          <span aria-hidden className={cn("size-2 rounded-full", connected ? "bg-success" : "bg-muted-foreground")} />
          {connected ? tc.live : tc.reconnecting}
        </span>
      </header>

      {error && (
        <p role="alert" className="px-4 pt-3 text-sm text-destructive sm:px-6">
          {error}
        </p>
      )}

      {list.isPending ? (
        <p className="p-6 text-muted-foreground">{t.loading}</p>
      ) : list.isError && !list.data ? (
        <p role="alert" className="p-6 text-destructive">
          {t.loadError}
        </p>
      ) : (
        <div className="grid flex-1 gap-4 p-4 sm:p-6 lg:grid-cols-3">
          {LANES.map((lane) => {
            const inLane = all.filter((ticket) => ticket.lane === lane);
            return (
              <section key={lane} aria-label={t.lanes[lane]} className="flex flex-col gap-3">
                <h2 className="flex items-center gap-2 px-1 font-semibold">
                  {t.lanes[lane]}
                  <span className="flex h-7 min-w-7 items-center justify-center rounded-full bg-muted px-2 text-sm font-bold">
                    {inLane.length}
                  </span>
                </h2>
                {inLane.length === 0 && <p className="px-1 text-sm text-muted-foreground">{t.empty}</p>}
                {inLane.map((ticket) => {
                  const since = Math.min(...ticket.items.map((i) => Date.parse(i.createdAt)));
                  const waited = (now - since) / 1000;
                  const level = waited >= LATE_AFTER_S ? "late" : waited >= WARN_AFTER_S ? "warn" : "ok";
                  const readyAt = Math.min(...ticket.items.map((i) => Date.parse(i.readyAt ?? i.createdAt)));
                  const title = `${t.table} ${ticket.tableLabel} · ${t.order(ticket.orderNumber)}`;
                  return (
                    <article
                      key={ticket.key}
                      aria-label={title}
                      className={cn(
                        "flex flex-col overflow-hidden rounded-2xl border border-border bg-card",
                        lane !== "READY" && level === "late" && "border-destructive",
                        lane === "READY" && "opacity-85",
                      )}
                    >
                      <div className={cn("flex items-center gap-3 bg-muted px-4 py-3", lane !== "READY" && level === "late" && "bg-destructive/15")}>
                        <span className="flex flex-1 flex-col leading-none">
                          <span className="text-[11px] font-semibold tracking-wide text-muted-foreground uppercase">{t.table}</span>
                          <span className="text-2xl font-bold">{ticket.tableLabel}</span>
                        </span>
                        <span className="flex flex-col items-end gap-1">
                          {lane === "READY" ? (
                            <span className="rounded-full bg-success/15 px-3 py-1 text-sm font-semibold text-success">
                              {t.readyFor(t.since((now - readyAt) / 1000))}
                            </span>
                          ) : (
                            <span
                              className={cn(
                                "font-mono text-lg font-semibold tabular-nums",
                                level === "ok" && "text-success",
                                level === "warn" && "text-warning",
                                level === "late" && "text-destructive",
                              )}
                            >
                              <span className="sr-only">{t.waitingFor(elapsed(waited))}</span>
                              <span aria-hidden>{elapsed(waited)}</span>
                            </span>
                          )}
                          <span className="text-sm text-muted-foreground">{t.order(ticket.orderNumber)}</span>
                        </span>
                      </div>
                      <ul className="flex flex-col">
                        {ticket.items.map((item) => {
                          const body = (
                            <>
                              <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-muted text-lg font-bold">
                                {item.quantity}
                              </span>
                              <span className="flex min-w-0 flex-1 flex-col items-start gap-1">
                                <span className="font-semibold">{item.productName}</span>
                                {item.note && (
                                  <span className="rounded-md bg-warning/15 px-2 py-0.5 text-sm font-semibold text-warning">
                                    {item.note}
                                  </span>
                                )}
                              </span>
                            </>
                          );
                          return (
                            <li key={item.id} className="border-t border-border">
                              {lane === "PREPARING" ? (
                                <button
                                  type="button"
                                  disabled={busy !== null}
                                  aria-label={t.markReady(item.productName)}
                                  onClick={() => run(item.id, [item], "ready")}
                                  className="flex w-full items-start gap-3 px-4 py-3 text-left hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none focus-visible:ring-inset disabled:opacity-60"
                                >
                                  {body}
                                </button>
                              ) : (
                                <div className="flex items-start gap-3 px-4 py-3">{body}</div>
                              )}
                            </li>
                          );
                        })}
                      </ul>
                      <div className="flex gap-2 border-t border-border p-3">
                        {lane === "PENDING" && (
                          <Button
                            className="h-13 flex-1 bg-amber-400 text-base font-bold text-amber-950 hover:bg-amber-300"
                            disabled={busy !== null}
                            onClick={() => run(ticket.key, ticket.items, "start")}
                          >
                            {busy === ticket.key ? t.starting : t.start}
                          </Button>
                        )}
                        {lane === "PREPARING" && (
                          <Button
                            className="h-13 flex-1 bg-emerald-700 text-base font-bold text-white hover:bg-emerald-600"
                            disabled={busy !== null}
                            onClick={() => run(ticket.key, ticket.items, "ready")}
                          >
                            {t.allReady}
                          </Button>
                        )}
                        <Button
                          variant="outline"
                          className={cn("h-13", lane === "READY" ? "flex-1" : "w-13")}
                          aria-label={t.cancel}
                          disabled={busy !== null}
                          onClick={() => setCanceling(ticket.items)}
                        >
                          <X aria-hidden />
                          {lane === "READY" && t.cancel}
                        </Button>
                      </div>
                    </article>
                  );
                })}
              </section>
            );
          })}
        </div>
      )}

      <CancelItemDialog
        items={canceling}
        onClose={() => setCanceling(null)}
        onDone={() => {
          setCanceling(null);
          void queryClient.invalidateQueries({ queryKey: QUEUE_KEY });
        }}
      />
    </div>
  );
}
