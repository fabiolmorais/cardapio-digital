"use client";

import { ChevronRight, Plus } from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { useLiveFloor } from "@/features/live/use-live-floor";
import { useLiveOrders } from "@/features/orders/use-live-orders";
import { clock } from "@/features/tabs/open-tabs";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { counterTabKey, fetchCounterTabs } from "./counter-api";
import { CounterTabDialog } from "./counter-tab-dialog";

const t = messages.panel.counter;
/** A lista muda quando abre ou muda de nome uma comanda, chega um pedido, fecha ou muda de mesa. */
const EVENTS = ["counter-tab", "order", "tab", "tab-moved"] as const;

/**
 * Balcão (desenho 10.7): as comandas abertas de cada balcão, cada uma com o nome do cliente, e a
 * "Nova comanda no balcão". Os balcões vêm do mapa ao vivo (mesma consulta do painel).
 */
export function CounterScreen() {
  const { status } = useRequiredSession();
  const floor = useLiveFloor(status === "authenticated");
  const counters = (floor.data?.tables ?? []).filter((place) => place.kind === "COUNTER");
  const searchParams = useSearchParams();
  const chosen = counters.find((c) => c.id === searchParams.get("balcao")) ?? counters[0];
  const router = useRouter();

  if (floor.isError) {
    return (
      <div className="flex flex-col items-start gap-3 px-4 py-6 sm:px-7">
        <p role="alert">{t.loadError}</p>
        <Button variant="outline" onClick={() => void floor.refetch()}>
          {t.retry}
        </Button>
      </div>
    );
  }
  if (!floor.data) {
    return <p className="px-4 py-6 text-muted-foreground sm:px-7">{t.loading}</p>;
  }
  const totalTabs = counters.reduce((sum, c) => sum + c.counterTabs, 0);
  return (
    <div className="flex flex-col gap-5 px-4 py-6 sm:px-7">
      <header className="flex flex-col gap-1">
        <h1 className="text-[28px] leading-[34px] font-bold tracking-tight">{t.title}</h1>
        {counters.length > 0 && (
          <p className="text-sm text-muted-foreground">{t.subtitle(counters.length, totalTabs)}</p>
        )}
      </header>
      {counters.length === 0 ? (
        <p className="text-muted-foreground">{t.none}</p>
      ) : (
        <>
          {counters.length > 1 && (
            <div role="group" aria-label={t.title} className="flex flex-wrap gap-2">
              {counters.map((c) => (
                <Button
                  key={c.id}
                  variant={c.id === chosen?.id ? "default" : "outline"}
                  aria-pressed={c.id === chosen?.id}
                  className="h-11"
                  onClick={() => router.replace(`/painel/balcao?balcao=${c.id}`)}
                >
                  {c.label} · {c.counterTabs}
                </Button>
              ))}
            </div>
          )}
          {chosen && <CounterTabs key={chosen.id} counter={chosen} />}
        </>
      )}
    </div>
  );
}

function CounterTabs({ counter }: { counter: { id: string; label: string } }) {
  const router = useRouter();
  const { status } = useRequiredSession();
  const { list } = useLiveOrders(
    counterTabKey(counter.id),
    () => fetchCounterTabs(counter.id),
    undefined,
    status === "authenticated",
    EVENTS,
  );
  const [opening, setOpening] = useState(false);
  const tabs = list.data ?? [];

  return (
    <section aria-label={counter.label} className="flex flex-col gap-3">
      <h2 className="text-lg font-semibold">{counter.label}</h2>
      {list.isError && <p role="alert">{t.loadError}</p>}
      {list.data && tabs.length === 0 && <p className="text-muted-foreground">{t.empty}</p>}
      <ul className="flex flex-col gap-2.5">
        {tabs.map((tab) => (
          <li key={tab.id}>
            <Link
              href={`/painel/comandas/${tab.id}`}
              aria-label={t.tabLabel(tab.customerName ?? "", tab.cardNumber)}
              className="flex items-center gap-3 rounded-2xl border border-border bg-card p-4 transition-colors hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
            >
              <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                <span className="truncate text-base font-semibold">{tab.customerName}</span>
                <span className={cn("text-sm", tab.itemsOnTheWay > 0 ? "text-warning" : "text-muted-foreground")}>
                  {t.since(clock(tab.openedAt))} · {t.items(tab.itemsOnTheWay, tab.totalCents)}
                </span>
              </span>
              {tab.cardNumber && (
                <span className="rounded-full bg-muted px-2.5 py-0.5 text-xs font-semibold text-muted-foreground">
                  {t.card(tab.cardNumber)}
                </span>
              )}
              <span className="font-mono font-semibold tabular-nums">{formatCents(tab.totalCents)}</span>
              <ChevronRight aria-hidden className="size-5 text-muted-foreground" />
            </Link>
          </li>
        ))}
      </ul>
      <p className="text-sm text-muted-foreground">{t.hint}</p>
      <Button size="xl" className="self-start" onClick={() => setOpening(true)}>
        <Plus aria-hidden />
        {t.newTab}
      </Button>
      <CounterTabDialog
        open={opening}
        onOpenChange={setOpening}
        counter={counter}
        onSaved={(saved) => router.push(`/painel/comandas/${saved.id}`)}
      />
    </section>
  );
}
