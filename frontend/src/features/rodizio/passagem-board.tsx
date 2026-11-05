"use client";

import { useQueryClient } from "@tanstack/react-query";
import { ChevronLeft, Flame } from "lucide-react";
import Link from "next/link";
import { useCallback, useState } from "react";
import { Button, buttonVariants } from "@/components/ui/button";
import { playChime } from "@/features/calls/alert-sound";
import { elapsed, useNow } from "@/features/calls/call-queue";
import { CancelItemDialog, type CancelableItem } from "@/features/orders/cancel-item-dialog";
import { stepItem } from "@/features/orders/orders-api";
import { useLiveOrders } from "@/features/orders/use-live-orders";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { fetchPassagemRequests, fetchPassagemTables, type PassagemTable } from "./rodizio-api";
import { useWakeLock } from "@/lib/pwa/use-wake-lock";

const t = messages.panel.passagem;
const tc = messages.panel.calls;
const KEY = ["passagem-tables"] as const;
const CUTS_KEY = ["passagem-requests"] as const;

/** A lista ao vivo precisa de um id: o da mesa serve (uma mesa tem um rodízio por vez). */
async function fetchTables() {
  return (await fetchPassagemTables()).map((table) => ({ ...table, id: table.tableId }));
}

/**
 * A tela dos passadores (no celular): os cortes pedidos pelas mesas (com "Entregue" e "Cancelar",
 * e o alerta a cada corte novo) e as mesas do rodízio de passagem, as de cartão vermelho primeiro,
 * com há quanto tempo a cor está assim. Tudo ao vivo, pelo tempo real.
 */
export function PassagemBoard() {
  const { status, session } = useRequiredSession();
  // O relógio da tela (Date.now) só depois da sessão: na pré-renderização do Next, não há hora.
  if (status !== "authenticated") return <p className="p-6 text-muted-foreground">{t.loading}</p>;
  // Entregar é de quem está no salão: a cozinha vê os cortes, mas não marca a entrega (como no Guia 19).
  return <Board canDeliver={session.establishment.role !== "KITCHEN"} />;
}

function Board({ canDeliver }: { canDeliver: boolean }) {
  const { list, connected } = useLiveOrders(KEY, fetchTables, undefined, true, ["rodizio", "tab"]);
  const now = useNow();
  useWakeLock();
  const tables = list.data ?? [];
  const paused = tables.filter((table) => table.status === "PAUSED").length;

  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-2xl flex-col gap-4 px-4 py-5">
      <header className="flex items-center gap-3">
        <Link href="/painel" aria-label={t.back} className={buttonVariants({ variant: "outline", size: "icon-lg" })}>
          <ChevronLeft aria-hidden />
        </Link>
        <span className="mr-auto flex flex-col">
          <h1 className="text-xl font-bold">{t.title}</h1>
          <span className="text-sm text-muted-foreground">
            {list.data ? t.count(paused, tables.length) : t.subtitle}
          </span>
        </span>
        <span
          role="status"
          className={cn("flex items-center gap-1.5 text-sm", connected ? "text-success" : "text-muted-foreground")}
        >
          <span aria-hidden className={cn("size-2 rounded-full", connected ? "bg-success" : "bg-muted-foreground")} />
          {connected ? tc.live : tc.reconnecting}
        </span>
      </header>

      <Cuts canDeliver={canDeliver} now={now} />

      <h2 className="pt-2 text-lg font-semibold">{t.tablesTitle}</h2>
      {list.isPending ? (
        <p className="text-muted-foreground">{t.loading}</p>
      ) : list.isError && !list.data ? (
        <p role="alert" className="text-destructive">
          {t.loadError}
        </p>
      ) : tables.length === 0 ? (
        <p className="text-muted-foreground">{t.empty}</p>
      ) : (
        <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          {tables.map((table) => (
            <Table key={table.tableId} table={table} now={now} />
          ))}
        </ul>
      )}
    </div>
  );
}

function Table({ table, now }: { table: PassagemTable; now: number }) {
  const paused = table.status === "PAUSED";
  const since = elapsed((now - Date.parse(table.statusChangedAt)) / 1000);
  return (
    <li
      aria-label={`${t.table(table.tableLabel)} · ${paused ? t.paused : t.serving}`}
      className={cn(
        "flex flex-col gap-1.5 rounded-2xl p-4 text-white",
        paused ? "bg-[#B91C1C]" : "bg-[#15803D]",
      )}
    >
      <span className="text-xs font-semibold tracking-wide uppercase opacity-90">{table.sectorName}</span>
      <span className="text-3xl font-bold">{table.tableLabel}</span>
      <span className="text-sm opacity-90">
        {t.people(table.people)} · {table.rodizioName}
      </span>
      <span className="mt-1 flex items-center gap-1.5 font-semibold">
        <Flame aria-hidden className="size-4 shrink-0" />
        {paused ? t.paused : t.serving}
      </span>
      <span className="text-sm tabular-nums opacity-90">{t.since(since)}</span>
    </li>
  );
}

/** Os cortes a caminho, do mais antigo ao mais novo: o passador leva e marca "Entregue". */
function Cuts({ canDeliver, now }: { canDeliver: boolean; now: number }) {
  const queryClient = useQueryClient();
  const { list } = useLiveOrders(CUTS_KEY, fetchPassagemRequests, useCallback(() => playChime(), []));
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [canceling, setCanceling] = useState<CancelableItem[] | null>(null);
  const cuts = list.data ?? [];

  async function deliver(id: string) {
    setBusy(id);
    setError(null);
    try {
      await stepItem(id, "deliver");
    } catch (e) {
      setError((e as Error).message);
    }
    await queryClient.invalidateQueries({ queryKey: CUTS_KEY });
    setBusy(null);
  }

  return (
    <section aria-labelledby="cortes" className="flex flex-col gap-2.5">
      <h2 id="cortes" className="text-lg font-semibold">
        {t.cutsTitle}
      </h2>
      {error && (
        <p role="alert" className="text-sm text-destructive">
          {error}
        </p>
      )}
      {list.data && cuts.length === 0 && <p className="text-sm text-muted-foreground">{t.cutsEmpty}</p>}
      <ul className="flex flex-col gap-2">
        {cuts.map((cut) => (
          <li
            key={cut.id}
            aria-label={t.cut(cut.tableLabel, cut.productName)}
            className="flex flex-wrap items-center gap-3 rounded-xl border border-border bg-card p-3"
          >
            <span className="flex size-12 shrink-0 flex-col items-center justify-center rounded-lg bg-muted leading-none">
              <span className="text-[10px] font-semibold tracking-wide text-muted-foreground uppercase">{tc.table}</span>
              <span className="text-lg font-bold">{cut.tableLabel}</span>
            </span>
            <span className="flex min-w-0 flex-1 flex-col">
              <span className="font-semibold">
                {cut.quantity > 1 && `${cut.quantity}× `}
                {cut.productName}
              </span>
              <span className="text-sm text-muted-foreground">
                {[cut.note, t.since(elapsed((now - Date.parse(cut.createdAt)) / 1000))].filter(Boolean).join(" · ")}
              </span>
            </span>
            <span className="flex gap-2">
              <Button variant="outline" className="h-11" disabled={busy !== null} onClick={() => setCanceling([cut])}>
                {t.cancel}
              </Button>
              {canDeliver && (
                <Button className="h-11" disabled={busy !== null} onClick={() => deliver(cut.id)}>
                  {busy === cut.id ? t.delivering : t.deliver}
                </Button>
              )}
            </span>
          </li>
        ))}
      </ul>
      <CancelItemDialog
        items={canceling}
        onClose={() => setCanceling(null)}
        onDone={() => {
          setCanceling(null);
          void queryClient.invalidateQueries({ queryKey: CUTS_KEY });
        }}
      />
    </section>
  );
}
