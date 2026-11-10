"use client";

import { ChevronRight } from "lucide-react";
import Link from "next/link";
import { useLiveOrders } from "@/features/orders/use-live-orders";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { fetchOpenTabs } from "./tabs-api";

const t = messages.panel.tabs;
const OPEN_TABS_KEY = ["open-tabs"] as const;
const TAB_EVENTS = ["order", "tab"] as const;

/** A hora no relógio deste aparelho (que está no restaurante). */
export function clock(iso: string) {
  return new Date(iso).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
}

/**
 * As mesas com comanda aberta (desenho 7.2): o total até agora e se ainda falta entregar algo.
 * Cada uma abre a comanda, de onde o garçom fecha a conta. Atualiza a cada pedido e fechamento.
 */
export function OpenTabs() {
  const { list } = useLiveOrders(OPEN_TABS_KEY, fetchOpenTabs, undefined, true, TAB_EVENTS);
  const tabs = list.data ?? [];

  return (
    <section aria-labelledby="mesas" className="flex flex-col gap-3 rounded-2xl border border-border bg-card p-5">
      <div className="flex flex-col gap-0.5">
        <h2 id="mesas" className="text-xl font-semibold">
          {t.title}
          {tabs.length > 0 && <span className="text-brand-text"> ({tabs.length})</span>}
        </h2>
        <p className="text-sm text-muted-foreground">{t.subtitle}</p>
      </div>
      {list.data && tabs.length === 0 ? (
        <p className="text-sm text-muted-foreground">{t.empty}</p>
      ) : (
        <ul className="flex flex-col gap-2.5">
          {tabs.map((tab) => (
            <li key={tab.id}>
              <Link
                href={`/painel/comandas/${tab.id}`}
                aria-label={t.open(tab.tableLabel)}
                className="flex items-center gap-3 rounded-xl border border-border bg-background p-3 transition-colors hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
              >
                <span className="flex size-12 shrink-0 flex-col items-center justify-center rounded-xl bg-muted leading-none">
                  <span className="text-[10px] font-semibold tracking-wide text-muted-foreground uppercase">
                    {messages.panel.calls.table}
                  </span>
                  <span className="text-lg font-bold">{tab.tableLabel}</span>
                </span>
                <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                  <span className="font-semibold">{formatCents(tab.totalCents)}</span>
                  <span
                    className={cn(
                      "truncate text-sm",
                      tab.itemsOnTheWay > 0 ? "text-warning" : "text-muted-foreground",
                    )}
                  >
                    {tab.itemsOnTheWay > 0 ? t.onTheWay(tab.itemsOnTheWay) : t.allDelivered} · {tab.sectorName} ·{" "}
                    {t.since(clock(tab.openedAt))}
                  </span>
                </span>
                <ChevronRight aria-hidden className="size-5 text-muted-foreground" />
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
