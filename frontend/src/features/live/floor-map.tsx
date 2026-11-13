"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { X } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { Button, buttonVariants } from "@/components/ui/button";
import { useNow } from "@/features/calls/call-queue";
import { RodizioDialog, type RodizioTarget } from "@/features/rodizio/rodizio-dialog";
import { clock } from "@/features/tabs/open-tabs";
import { fetchTab } from "@/features/tabs/tabs-api";
import { formatCents } from "@/lib/format";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { subscribeStaffEvents } from "@/lib/realtime/shared-staff-events";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { sectorsOf, type LiveTable } from "./live-api";
import { Badge, ITEM_TONE, TableBox, elapsed, type Tone } from "./live-ui";
import { useLiveFloor } from "./use-live-floor";

const t = messages.panel.floorLive;
const calls = messages.panel.calls;
const status = messages.tablePage.orders.status;
const tm = messages.panel.tab;

/** A cor de cada estado no mapa (a legenda do desenho 7.2). */
const STATE_CLASS: Record<LiveTable["state"], string> = {
  FREE: "border-dashed bg-transparent text-muted-foreground",
  OCCUPIED: "bg-muted",
  CALLING: "border-brand-text/60 bg-primary/15",
  BILL: "border-info/50 bg-info/15",
};

const BOX_TONE: Record<LiveTable["state"], Tone | undefined> = {
  FREE: undefined,
  OCCUPIED: undefined,
  CALLING: "accent",
  BILL: "info",
};

/**
 * "Mesas" (desenho 7.2): o salão por setor, cada mesa pintada pelo que acontece nela (livre,
 * ocupada, chamando, conta pedida), com a linha que diz o porquê. Tocar numa mesa abre a gaveta
 * com a comanda (itens, passos, total), o rodízio e o atalho para fechar. Ao vivo.
 */
export function FloorMap() {
  const { status: session } = useRequiredSession();
  if (session !== "authenticated") return <p className="px-4 py-6 text-muted-foreground sm:px-7">{t.loading}</p>;
  return <Floor />;
}

function Floor() {
  const floor = useLiveFloor();
  const now = useNow();
  const [selected, setSelected] = useState<string | null>(null);

  if (floor.isPending) return <p className="px-4 py-6 text-muted-foreground sm:px-7">{t.loading}</p>;
  if (!floor.data) {
    return (
      <p role="alert" className="px-4 py-6 text-destructive sm:px-7">
        {t.loadError}
      </p>
    );
  }

  const tables = floor.data.tables;
  const bills = tables.filter((table) => table.state === "BILL").length;
  const table = tables.find((tb) => tb.id === selected) ?? null;

  return (
    <>
      <header className="flex flex-wrap items-center gap-3 border-b border-border px-4 py-4 sm:px-7">
        <div className="mr-auto flex flex-col gap-0.5">
          <h1 className="text-[28px] leading-[34px] font-bold tracking-tight">{t.title}</h1>
          <span className="text-[13px] text-muted-foreground">
            {t.subtitle(floor.data.occupied, tables.length, bills)}
          </span>
        </div>
        <ul aria-label={t.legend} className="flex flex-wrap gap-3.5 text-[13px] text-muted-foreground">
          {(["FREE", "OCCUPIED", "CALLING", "BILL"] as const).map((state) => (
            <li key={state} className="flex items-center gap-1.5">
              <span aria-hidden className={cn("size-3 rounded border border-border", STATE_CLASS[state])} />
              {t.states[state]}
            </li>
          ))}
        </ul>
      </header>

      <div className="flex flex-col lg:flex-row">
        <div className="flex min-w-0 flex-1 flex-col gap-6 px-4 py-6 sm:px-7">
          {tables.length === 0 && <p className="text-muted-foreground">{t.empty}</p>}
          {sectorsOf(tables).map((sector) => (
            <section key={sector} aria-label={sector} className="flex flex-col gap-3">
              <h2 className="font-semibold text-muted-foreground">{sector}</h2>
              <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 xl:grid-cols-6">
                {tables
                  .filter((tb) => tb.sectorName === sector)
                  .map((tb) => (
                    <button
                      key={tb.id}
                      type="button"
                      aria-pressed={tb.id === selected}
                      aria-label={`${t.select(tb.label)} · ${line(tb, now).text}`}
                      onClick={() => setSelected(tb.id === selected ? null : tb.id)}
                      className={cn(
                        "flex h-27 flex-col justify-between rounded-[18px] border border-border p-3 text-left focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                        STATE_CLASS[tb.state],
                        tb.id === selected && "outline-2 outline-offset-2 outline-foreground",
                      )}
                    >
                      <span className="text-2xl leading-6 font-bold">{tb.label}</span>
                      <span className={cn("text-[13px] leading-[18px]", line(tb, now).className)}>
                        {line(tb, now).text}
                      </span>
                    </button>
                  ))}
              </div>
            </section>
          ))}
        </div>
        {table && <Drawer key={table.id} table={table} onClose={() => setSelected(null)} />}
      </div>
    </>
  );
}

/** O valor sem o "R$" (a coluna de valores da comanda, como no desenho). */
function bare(cents: number) {
  return formatCents(cents).replace(/^R\$\s*/, "");
}

/** A linha de baixo de cada mesa no mapa: o que está acontecendo nela, na cor do estado. */
function line(table: LiveTable, now: number): { text: string; className: string } {
  const call = table.call;
  if (table.state === "BILL") {
    return { text: t.bill(call?.paymentMethod ? calls.payment[call.paymentMethod] : null), className: "text-info" };
  }
  if (call && call.status === "IN_PROGRESS") {
    return { text: t.attending(call.acceptedByName), className: "text-warning" };
  }
  if (call) {
    const text =
      call.type === "UNLOCK"
        ? t.unlock
        : call.type === "RODIZIO"
          ? t.rodizioCall
          : t.waiter(elapsed((now - Date.parse(call.createdAt)) / 1000));
    return { text, className: "text-brand-text" };
  }
  if (table.waitingOrders > 0) return { text: t.toConfirm, className: "text-warning" };
  if (table.state === "FREE") return { text: t.states.FREE, className: "" };
  if (table.rodizioPeople > 0) return { text: t.rodizio(table.rodizioPeople), className: "text-muted-foreground" };
  if (table.tabOpenedAt) {
    const minutes = Math.max(0, Math.floor((now - Date.parse(table.tabOpenedAt)) / 60_000));
    return { text: t.tab(formatCents(table.tabTotalCents), minutes), className: "text-muted-foreground" };
  }
  return { text: t.phones(table.phones), className: "text-muted-foreground" };
}

/** A gaveta da mesa (desenho 7.2): a comanda aberta, os celulares e as ações. */
function Drawer({ table, onClose }: { table: LiveTable; onClose: () => void }) {
  const queryClient = useQueryClient();
  const tab = useQuery({
    queryKey: ["tab", table.tabId],
    queryFn: () => fetchTab(table.tabId!),
    enabled: table.tabId != null,
    retry: false,
  });
  const [rodizio, setRodizio] = useState<RodizioTarget | null>(null);

  useEffect(
    () =>
      subscribeStaffEvents(
        (event) => {
          if (["ready", "order", "tab", "rodizio", "call"].includes(event.name)) {
            void queryClient.invalidateQueries({ queryKey: ["tab", table.tabId] });
          }
        },
        () => {},
      ),
    [queryClient, table.tabId],
  );

  const data = table.tabId ? tab.data : null;
  const billWith = data?.billRequestedWith ?? (table.state === "BILL" ? table.call?.paymentMethod : null);

  return (
    <aside
      aria-label={t.open(table.label)}
      className="flex flex-col border-t border-border bg-card lg:sticky lg:top-0 lg:h-dvh lg:w-[400px] lg:shrink-0 lg:overflow-y-auto lg:border-t-0 lg:border-l"
    >
      <div className="flex flex-col gap-2.5 border-b border-border px-5.5 py-5">
        <div className="flex items-center gap-3">
          <TableBox label={table.label} tone={BOX_TONE[table.state]} />
          <span className="flex min-w-0 flex-1 flex-col gap-0.5">
            <span className="text-xl font-semibold">{table.tabId ? t.openTab : t.noTab}</span>
            <span className="text-[13px] text-muted-foreground">
              {table.tabOpenedAt ? t.since(table.sectorName, clock(table.tabOpenedAt)) : table.sectorName}
            </span>
          </span>
          <Button variant="outline" size="icon-lg" className="size-10" aria-label={t.close} onClick={onClose}>
            <X aria-hidden />
          </Button>
        </div>
        {(billWith || data?.rodizio || (data && data.paidCents > 0)) && (
          <div className="flex flex-wrap gap-2">
            {billWith && <Badge tone="info">{t.billRequested(calls.payment[billWith])}</Badge>}
            {data && data.paidCents > 0 && (
              <Badge tone="ok">{data.dueCents === 0 ? tm.settled : t.paidSoFar(formatCents(data.paidCents))}</Badge>
            )}
            {data?.rodizio && (
              <Badge tone="accent">
                {t.rodizioBadge(
                  data.rodizio.name,
                  data.rodizio.guests.reduce((sum, g) => sum + g.quantity, 0),
                )}
              </Badge>
            )}
          </div>
        )}
      </div>

      <div className="flex flex-1 flex-col gap-5 px-5.5 py-4.5">
        <section className="flex flex-col gap-2">
          <h3 className="text-[11px] font-semibold tracking-[.4px] text-muted-foreground uppercase">{t.phonesTitle}</h3>
          <p className="rounded-xl bg-secondary px-3 py-2.5 text-[13px]">
            {table.phones === 0 ? t.noPhones : t.phones(table.phones)}
          </p>
        </section>

        {table.tabId && (
          <section className="flex flex-col gap-2.5">
            <h3 className="text-[11px] font-semibold tracking-[.4px] text-muted-foreground uppercase">
              {t.itemsTitle}
            </h3>
            {!data ? (
              <p className="text-[13px] text-muted-foreground">{t.loading}</p>
            ) : (
              <>
                {data.rodizio && (
                  <div className="flex items-center gap-2.5 text-[13px]">
                    <span className="flex-1">
                      {data.rodizio.guests
                        .filter((g) => g.quantity > 0)
                        .map((g) =>
                          t.rodizioLine(
                            `${data.rodizio!.name} ${g.tierName}`,
                            g.quantity,
                            formatCents(g.unitPriceCents),
                          ),
                        )
                        .join(" · ")}
                    </span>
                    <span className="font-mono">{formatCents(data.rodizio.totalCents)}</span>
                  </div>
                )}
                {data.orders.flatMap((o) => o.items).length === 0 && !data.rodizio && (
                  <p className="text-[13px] text-muted-foreground">{t.noItems}</p>
                )}
                {data.orders
                  .flatMap((o) => o.items)
                  .map((item) => {
                    const canceled = item.status === "CANCELED";
                    return (
                      <div key={item.id} className="flex items-center gap-2.5 text-[13px]">
                        <span className={cn("flex-1", canceled && "text-muted-foreground line-through")}>
                          {item.quantity}× {item.productName}
                        </span>
                        <Badge tone={ITEM_TONE[item.status]} plain>
                          {status[item.status]}
                        </Badge>
                        <span className={cn("w-16 text-right font-mono", canceled && "text-muted-foreground")}>
                          {bare(canceled || item.rodizio ? 0 : item.unitPriceCents * item.quantity)}
                        </span>
                      </div>
                    );
                  })}
                <div className="h-px bg-border" />
                <div className="flex items-center gap-2.5">
                  <span className="flex-1 font-semibold">{t.total}</span>
                  <span className="font-mono text-xl font-semibold">{formatCents(data.totalCents)}</span>
                </div>
                {data.payments.length > 0 && (
                  <>
                    {/* O que a mesa já pagou (adiantado, com itens ainda a caminho), como na comanda. */}
                    <ul aria-label={tm.paidTotal} className="flex flex-col gap-1.5">
                      {data.payments.map((p) => (
                        <li key={p.receivedAt} className="flex items-center gap-2.5 text-[13px]">
                          <span className="flex-1">
                            <Badge tone="ok">{tm.paidTag(calls.payment[p.paymentMethod], clock(p.receivedAt))}</Badge>
                          </span>
                          <span className="font-mono text-success">− {bare(p.amountCents)}</span>
                        </li>
                      ))}
                    </ul>
                    <div className="flex items-center gap-2.5">
                      <span className="flex-1 font-semibold">{tm.due}</span>
                      <span className="font-mono text-xl font-semibold">{formatCents(data.dueCents)}</span>
                    </div>
                  </>
                )}
              </>
            )}
          </section>
        )}

        <div className="mt-auto flex flex-col gap-2">
          {table.tabId && (
            <Link
              href={`/painel/comandas/${table.tabId}`}
              className={cn(buttonVariants(), "h-11 w-full bg-[#15803d] text-white hover:bg-[#166534]")}
            >
              {t.closeTab}
            </Link>
          )}
          <Button
            variant="outline"
            className="h-11"
            onClick={() =>
              setRodizio({
                tableId: table.id,
                tableLabel: table.label,
                current: data?.rodizio ?? null,
                requestedAt: table.call?.type === "RODIZIO" ? table.call.createdAt : undefined,
              })
            }
          >
            {data?.rodizio ? t.adjustRodizio : t.activateRodizio}
          </Button>
        </div>
      </div>
      <RodizioDialog target={rodizio} onClose={() => setRodizio(null)} />
    </aside>
  );
}
