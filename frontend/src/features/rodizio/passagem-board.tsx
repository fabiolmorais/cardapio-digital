"use client";

import { ChevronLeft, Flame } from "lucide-react";
import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { elapsed, useNow } from "@/features/calls/call-queue";
import { useLiveOrders } from "@/features/orders/use-live-orders";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { fetchPassagemTables, type PassagemTable } from "./rodizio-api";

const t = messages.panel.passagem;
const tc = messages.panel.calls;
const KEY = ["passagem-tables"] as const;

/** A lista ao vivo precisa de um id: o da mesa serve (uma mesa tem um rodízio por vez). */
async function fetchTables() {
  return (await fetchPassagemTables()).map((table) => ({ ...table, id: table.tableId }));
}

/**
 * A tela dos passadores (no celular): as mesas do rodízio de passagem, as de cartão vermelho
 * primeiro, com há quanto tempo a cor está assim. Atualiza a cada virada de cartão e a cada conta
 * fechada, pelo tempo real.
 */
export function PassagemBoard() {
  const { status } = useRequiredSession();
  // O relógio da tela (Date.now) só depois da sessão: na pré-renderização do Next, não há hora.
  if (status !== "authenticated") return <p className="p-6 text-muted-foreground">{t.loading}</p>;
  return <Board />;
}

function Board() {
  const { list, connected } = useLiveOrders(KEY, fetchTables, undefined, true, ["rodizio", "tab"]);
  const now = useNow();
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
