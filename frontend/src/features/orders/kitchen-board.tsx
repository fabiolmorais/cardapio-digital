"use client";

import { useQueryClient } from "@tanstack/react-query";
import { ChefHat, ChevronLeft, Soup, X } from "lucide-react";
import Link from "next/link";
import { useCallback, useState } from "react";
import { Button, buttonVariants } from "@/components/ui/button";
import { useMe } from "@/features/account/use-me";
import { playChime } from "@/features/calls/alert-sound";
import { elapsed, useNow } from "@/features/calls/call-queue";
import { clockTime, useMinuteClock } from "@/features/live/clock";
import { Badge, Timer } from "@/features/live/live-ui";
import { SoundTag } from "@/features/live/sound-tag";
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

type Level = "ok" | "warn" | "late";
function levelOf(seconds: number): Level {
  return seconds >= LATE_AFTER_S ? "late" : seconds >= WARN_AFTER_S ? "warn" : "ok";
}

/** A cor do título de cada coluna, como no desenho: na fila neutro, em preparo amarelo, pronto azul. */
const LANE_COLOR: Record<Lane, string> = { PENDING: "", PREPARING: "text-warning", READY: "text-info" };

type View = "order" | "item";
/** A visão escolhida fica no aparelho: o tablet da cozinha abre sempre do jeito que a equipe usa. */
const VIEW_KEY = "kitchen-view";

function useView(): [View, (view: View) => void] {
  // O quadro só monta no navegador, depois da sessão confirmada: dá para ler o aparelho já aqui.
  const [view, setView] = useState<View>(() => {
    try {
      return window.localStorage.getItem(VIEW_KEY) === "item" ? "item" : "order";
    } catch {
      return "order"; // sem armazenamento (aba privada): fica na visão por pedido
    }
  });
  return [
    view,
    (next) => {
      setView(next);
      try {
        window.localStorage.setItem(VIEW_KEY, next);
      } catch {
        // idem
      }
    },
  ];
}

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
 * A tela da cozinha. Por pedido (desenho 9.1): três colunas (na fila, em preparo, pronto), uma
 * ficha por pedido. "Começar" põe a ficha em preparo; em preparo, cada toque num item marca ele
 * pronto, e "Tudo pronto" marca a ficha inteira. Por item (9.3): o que está na fila e em preparo,
 * somado por produto, com as mesas de cada um. Atualiza pelo tempo real e toca o alerta a cada
 * item novo; no topo, o "Som ligado" e a hora no fuso do restaurante.
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
  const clock = useMinuteClock();
  const me = useMe();
  const establishment = me.data?.establishment;
  const [view, setView] = useView();
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
      <header className="flex flex-wrap items-center gap-3 border-b border-border px-4 py-3 sm:gap-4 sm:px-6">
        <Link href="/painel" aria-label={t.back} className={buttonVariants({ variant: "outline", size: "icon-lg" })}>
          <ChevronLeft aria-hidden />
        </Link>
        <span className="flex size-11 items-center justify-center rounded-xl bg-secondary">
          <ChefHat aria-hidden className="size-6" />
        </span>
        <span className="mr-auto flex flex-col">
          <h1 className="text-xl font-semibold tracking-tight">{view === "item" ? t.byItemTitle : t.title}</h1>
          <span className="text-[13px] text-muted-foreground">
            {view === "item" ? t.byItemSubtitle : t.subtitle(establishment?.name)}
          </span>
        </span>
        <div
          role="group"
          aria-label={t.views.label}
          className="flex gap-1.5 rounded-[14px] border border-border bg-card p-1"
        >
          {(["order", "item"] as const).map((option) => (
            <button
              key={option}
              type="button"
              aria-pressed={view === option}
              onClick={() => setView(option)}
              className={cn(
                "h-9 rounded-[10px] px-3 text-[13px] font-semibold text-muted-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                view === option && "bg-foreground text-background",
              )}
            >
              {t.views[option]}
            </button>
          ))}
        </div>
        <SoundTag />
        {!connected && (
          <span role="status" className="text-sm text-muted-foreground">
            {tc.reconnecting}
          </span>
        )}
        <span className="min-w-[3.5ch] font-mono text-xl font-bold tabular-nums">
          {clock && clockTime(clock, establishment?.timeZone)}
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
      ) : view === "item" ? (
        <ByItem queue={list.data ?? []} now={now} />
      ) : (
        <div className="grid flex-1 gap-4 p-4 sm:p-6 lg:grid-cols-3">
          {LANES.map((lane) => {
            const inLane = all.filter((ticket) => ticket.lane === lane);
            return (
              <section key={lane} aria-label={t.lanes[lane]} className="flex flex-col gap-3">
                <h2 className="flex items-center gap-2.5 px-1 font-semibold">
                  <span className={LANE_COLOR[lane]}>{t.lanes[lane]}</span>
                  <span className="flex h-7 min-w-7 items-center justify-center rounded-full bg-secondary px-2 text-sm font-bold">
                    {inLane.length}
                  </span>
                </h2>
                {inLane.length === 0 && <p className="px-1 text-sm text-muted-foreground">{t.empty}</p>}
                {inLane.map((ticket) => {
                  const since = Math.min(...ticket.items.map((i) => Date.parse(i.createdAt)));
                  const waited = (now - since) / 1000;
                  const level = levelOf(waited);
                  const readyAt = Math.min(...ticket.items.map((i) => Date.parse(i.readyAt ?? i.createdAt)));
                  const title = `${t.table} ${ticket.tableLabel} · ${t.order(ticket.orderNumber)}`;
                  return (
                    <article
                      key={ticket.key}
                      aria-label={title}
                      className={cn(
                        "flex flex-col overflow-hidden rounded-[20px] border border-border bg-card",
                        lane !== "READY" && level === "late" && "border-destructive/55",
                        lane === "READY" && "opacity-85",
                      )}
                    >
                      <div
                        className={cn(
                          "flex items-center gap-3 bg-muted px-3.5 py-3",
                          lane !== "READY" && level === "late" && "bg-destructive/15",
                        )}
                      >
                        <span className="flex flex-1 flex-col">
                          <span className="text-[11px] font-semibold tracking-wide text-muted-foreground/80 uppercase">
                            {t.table}
                          </span>
                          <span className="text-[26px] leading-[26px] font-bold">{ticket.tableLabel}</span>
                        </span>
                        <span className="flex flex-col items-end gap-1">
                          {lane === "READY" ? (
                            <Badge tone="info" className="h-[30px] text-sm">
                              {t.readyFor(t.since((now - readyAt) / 1000))}
                            </Badge>
                          ) : (
                            <Timer level={level}>
                              <span className="sr-only">{t.waitingFor(elapsed(waited))}</span>
                              <span aria-hidden>{elapsed(waited)}</span>
                            </Timer>
                          )}
                          {lane !== "READY" && (
                            <span className="text-[13px] text-muted-foreground">{t.order(ticket.orderNumber)}</span>
                          )}
                        </span>
                      </div>
                      <ul className="flex flex-col">
                        {ticket.items.map((item) => {
                          const body = (
                            <>
                              <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-secondary text-xl font-bold">
                                {item.quantity}
                              </span>
                              <span className="flex min-w-0 flex-1 flex-col items-start gap-1">
                                <span className="font-semibold">{item.productName}</span>
                                {item.note && (
                                  <span className="rounded-lg bg-warning/15 px-2 py-0.5 text-sm font-semibold text-warning">
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
                                  className="flex w-full items-start gap-3 px-3.5 py-3 text-left hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none focus-visible:ring-inset disabled:opacity-60"
                                >
                                  {body}
                                </button>
                              ) : (
                                <div className="flex items-start gap-3 px-3.5 py-3">{body}</div>
                              )}
                            </li>
                          );
                        })}
                      </ul>
                      <div className="flex gap-2 px-3 pt-2.5 pb-3">
                        {lane === "PENDING" && (
                          <Button
                            className="h-13 flex-1 rounded-[14px] bg-warning text-base font-bold text-[#1A1205] hover:bg-warning/90"
                            disabled={busy !== null}
                            onClick={() => run(ticket.key, ticket.items, "start")}
                          >
                            {busy === ticket.key ? t.starting : t.start}
                          </Button>
                        )}
                        {lane === "PREPARING" && (
                          <Button
                            className="h-13 flex-1 rounded-[14px] bg-[#15803D] text-base font-bold text-white hover:bg-[#166534]"
                            disabled={busy !== null}
                            onClick={() => run(ticket.key, ticket.items, "ready")}
                          >
                            {t.allReady}
                          </Button>
                        )}
                        <Button
                          variant="outline"
                          className={cn("h-13 rounded-[14px]", lane === "READY" ? "flex-1" : "w-13")}
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

/** Um produto na visão por item: o total, as mesas (com a quantidade e a espera) e as observações. */
type Aggregate = {
  key: string;
  name: string;
  photoUrl?: string;
  total: number;
  tables: { label: string; quantity: number; since: number }[];
  notes: { note: string; table: string }[];
};

/** Soma a fila e o preparo por produto, mantendo a ordem de chegada das mesas. */
function aggregate(queue: KitchenItem[]): Aggregate[] {
  const byProduct = new Map<string, Aggregate>();
  for (const item of queue) {
    if (item.status === "READY") continue;
    const key = item.productId ?? item.productName;
    let entry = byProduct.get(key);
    if (!entry) {
      entry = { key, name: item.productName, photoUrl: item.photoUrl, total: 0, tables: [], notes: [] };
      byProduct.set(key, entry);
    }
    entry.total += item.quantity;
    const since = Date.parse(item.createdAt);
    const table = entry.tables.find((t) => t.label === item.tableLabel);
    if (table) {
      table.quantity += item.quantity;
      table.since = Math.min(table.since, since);
    } else {
      entry.tables.push({ label: item.tableLabel, quantity: item.quantity, since });
    }
    if (item.note && !entry.notes.some((n) => n.note === item.note && n.table === item.tableLabel)) {
      entry.notes.push({ note: item.note, table: item.tableLabel });
    }
  }
  // Os que mais pedem primeiro; no empate, quem espera há mais tempo.
  const oldest = (a: Aggregate) => Math.min(...a.tables.map((t) => t.since));
  return [...byProduct.values()].sort((a, b) => b.total - a.total || oldest(a) - oldest(b));
}

/**
 * A cozinha por item (desenho 9.3): um cartão por produto com a foto, o total a fazer e as mesas
 * ("Mesa 05 ×2"). A mesa atrasada (10 min ou mais) fica vermelha, com os minutos, e pinta o total;
 * as observações aparecem embaixo, com a mesa quando o produto é de mais de uma.
 */
function ByItem({ queue, now }: { queue: KitchenItem[]; now: number }) {
  const products = aggregate(queue);
  if (products.length === 0) return <p className="p-6 text-muted-foreground">{t.emptyByItem}</p>;
  return (
    <div className="grid content-start gap-4 p-4 sm:grid-cols-2 sm:p-6 lg:grid-cols-3">
      {products.map((product) => {
        const late = product.tables.some((table) => (now - table.since) / 1000 >= LATE_AFTER_S);
        return (
          <article
            key={product.key}
            aria-label={t.itemTotal(product.name, product.total)}
            className="flex flex-col gap-3.5 rounded-[22px] border border-border bg-card p-4"
          >
            <div className="flex items-center gap-3.5">
              <span className="flex size-16 shrink-0 items-center justify-center overflow-hidden rounded-2xl bg-secondary text-muted-foreground">
                {product.photoUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element -- miniatura já no tamanho certo, vinda do R2
                  <img src={product.photoUrl} alt="" decoding="async" className="size-full object-cover" />
                ) : (
                  <Soup aria-hidden className="size-6.5" />
                )}
              </span>
              <span className="min-w-0 flex-1 font-semibold">{product.name}</span>
              <span
                aria-hidden
                className={cn("font-mono text-[44px] leading-[44px] font-bold", late && "text-destructive")}
              >
                {product.total}
              </span>
            </div>
            <ul className="flex flex-wrap gap-1.5">
              {product.tables.map((table) => {
                const minutes = Math.floor((now - table.since) / 60_000);
                const tableLate = minutes * 60 >= LATE_AFTER_S;
                return (
                  <li
                    key={table.label}
                    className={cn(
                      "inline-flex h-8 items-center gap-1.5 rounded-[10px] bg-secondary px-2.5 text-sm font-semibold",
                      tableLate && "bg-destructive/15 text-destructive",
                    )}
                  >
                    {t.table} {table.label}
                    <span className={tableLate ? "text-destructive" : "text-muted-foreground"}>
                      {tableLate ? t.tableLate(table.quantity, minutes) : t.tableQty(table.quantity)}
                    </span>
                  </li>
                );
              })}
            </ul>
            {product.notes.map(({ note, table }) => (
              <span
                key={`${table}|${note}`}
                className="self-start rounded-lg bg-warning/15 px-2 py-0.5 text-[13px] font-semibold text-warning"
              >
                {product.tables.length > 1 ? t.noteAt(note, table) : note}
              </span>
            ))}
          </article>
        );
      })}
    </div>
  );
}
