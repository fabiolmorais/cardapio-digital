"use client";

import { useQuery } from "@tanstack/react-query";
import { ChevronLeft } from "lucide-react";
import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { fetchTab } from "@/features/tabs/tabs-api";
import { useSession } from "@/lib/auth/use-session";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { dateTime } from "./tab-history";

const t = messages.tabHistory.detail;
const pay = messages.panel.calls.payment;
const status = messages.tablePage.orders.status;

function time(iso: string, timeZone: string) {
  return new Date(iso).toLocaleTimeString("pt-BR", { timeZone, hour: "2-digit", minute: "2-digit" });
}

/** Uma comanda do histórico: quando abriu e fechou, como pagou, os pedidos com os itens e o total. */
export function ClosedTabDetail({ id }: { id: string }) {
  const session = useSession();
  const tab = useQuery({
    queryKey: ["tab", id],
    queryFn: () => fetchTab(id),
    enabled: session.status === "authenticated",
    retry: false,
  });

  const back = (
    <Link
      href="/admin/comandas"
      className={cn(buttonVariants({ variant: "ghost" }), "w-fit gap-2 px-2 text-muted-foreground")}
    >
      <ChevronLeft aria-hidden />
      {t.back}
    </Link>
  );

  if (session.status !== "authenticated" || tab.isPending) {
    return (
      <div className="flex flex-col gap-4 px-4 py-6 sm:px-7">
        {back}
        <p className="text-muted-foreground">{t.loading}</p>
      </div>
    );
  }
  if (!tab.data) {
    return (
      <div className="flex flex-col gap-4 px-4 py-6 sm:px-7">
        {back}
        <p role="alert" className="text-destructive">
          {t.loadError}
        </p>
      </div>
    );
  }

  const data = tab.data;
  // As horas no relógio do restaurante, como no histórico.
  const timeZone = session.session.establishment.timeZone;
  return (
    <div className="flex max-w-2xl flex-col gap-5 px-4 py-6 sm:px-7">
      {back}
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">{t.title(data.tableLabel)}</h1>
        <p className="text-sm text-muted-foreground">{data.sectorName}</p>
      </header>

      {data.status !== "CLOSED" && <p className="rounded-xl bg-muted p-3 text-sm">{t.stillOpen}</p>}

      <dl className="grid grid-cols-1 gap-3 rounded-2xl border border-border bg-card p-5 sm:grid-cols-3">
        <div className="flex flex-col gap-0.5">
          <dt className="text-sm text-muted-foreground">{t.opened}</dt>
          <dd className="font-semibold">{dateTime(data.openedAt, timeZone)}</dd>
        </div>
        {data.closedAt && (
          <div className="flex flex-col gap-0.5">
            <dt className="text-sm text-muted-foreground">{t.closed}</dt>
            <dd className="font-semibold">{dateTime(data.closedAt, timeZone)}</dd>
          </div>
        )}
        {data.paymentMethod && (
          <div className="flex flex-col gap-0.5">
            <dt className="text-sm text-muted-foreground">{t.payment}</dt>
            <dd className="font-semibold">{pay[data.paymentMethod]}</dd>
          </div>
        )}
      </dl>

      <section className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-5">
        {data.orders.map((order) => (
          <div key={order.id} className="flex flex-col gap-1.5">
            <h2 className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              {t.order(order.number, time(order.createdAt, timeZone))}
            </h2>
            <ul className="flex flex-col gap-1.5">
              {order.items.map((item) => {
                const canceled = item.status === "CANCELED";
                return (
                  <li key={item.id} className="flex flex-wrap items-center gap-x-2 gap-y-0.5 text-sm">
                    <span className={cn("flex-1", canceled && "text-muted-foreground line-through")}>
                      {item.quantity}× {item.productName}
                    </span>
                    <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium">{status[item.status]}</span>
                    <span className="w-20 text-right font-mono tabular-nums">
                      {canceled ? "" : formatCents(item.unitPriceCents * item.quantity)}
                    </span>
                    {canceled && item.cancelReason && (
                      <span className="w-full text-xs text-muted-foreground">{item.cancelReason}</span>
                    )}
                  </li>
                );
              })}
            </ul>
          </div>
        ))}
        <div className="flex items-baseline justify-between border-t border-border pt-3">
          <span className="font-semibold">{t.total}</span>
          <span className="font-mono text-lg font-bold tabular-nums">{formatCents(data.totalCents)}</span>
        </div>
      </section>
    </div>
  );
}
