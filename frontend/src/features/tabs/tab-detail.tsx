"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check, ChevronLeft, TriangleAlert } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { Button, buttonVariants } from "@/components/ui/button";
import { formatCents } from "@/lib/format";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { subscribeStaffEvents } from "@/lib/realtime/shared-staff-events";
import { RodizioDialog, type RodizioTarget } from "@/features/rodizio/rodizio-dialog";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { clock } from "./open-tabs";
import { closeTab, fetchTab, type PaymentMethod, type StaffTab } from "./tabs-api";

const t = messages.panel.tab;
const tp = messages.panel.calls.payment;
const ts = messages.tablePage.orders.status;
const METHODS: PaymentMethod[] = ["PIX", "CARD", "CASH"];

/**
 * A comanda de uma mesa (desenhos 10.5 e 10.6): os pedidos de todos os celulares, o passo de
 * cada item, o total, a conta pedida e "Fechar comanda" com a forma de pagamento. Atualiza a cada
 * pedido, item ou chamado da mesa, pelo tempo real.
 */
export function TabDetail({ id }: { id: string }) {
  const { status } = useRequiredSession();
  if (status !== "authenticated") return <p className="px-4 py-6 text-muted-foreground">{t.loading}</p>;
  return <Detail id={id} />;
}

function Detail({ id }: { id: string }) {
  const queryClient = useQueryClient();
  const key = ["tab", id];
  const tab = useQuery({ queryKey: key, queryFn: () => fetchTab(id), retry: false });

  useEffect(
    () =>
      subscribeStaffEvents(
        (event) => {
          if (["ready", "order", "call", "tab", "rodizio"].includes(event.name)) {
            void queryClient.invalidateQueries({ queryKey: ["tab", id] });
          }
        },
        () => {},
      ),
    [queryClient, id],
  );

  if (tab.isPending) return <p className="px-4 py-6 text-muted-foreground">{t.loading}</p>;
  if (!tab.data) {
    return (
      <p role="alert" className="px-4 py-6 text-destructive">
        {t.loadError}
      </p>
    );
  }
  return <Body tab={tab.data} onClosed={(closed) => queryClient.setQueryData(key, closed)} />;
}

function Body({ tab, onClosed }: { tab: StaffTab; onClosed: (closed: StaffTab) => void }) {
  const queryClient = useQueryClient();
  const [method, setMethod] = useState<PaymentMethod>(tab.billRequestedWith ?? "PIX");
  const [rodizioTarget, setRodizioTarget] = useState<RodizioTarget | null>(null);
  const close = useMutation({
    mutationFn: () => closeTab(tab.id, method),
    onSuccess: (closed) => {
      onClosed(closed);
      void queryClient.invalidateQueries({ queryKey: ["open-tabs"] });
    },
  });
  const open = tab.status !== "CLOSED";
  const items = tab.orders.flatMap((o) => o.items);
  const onTheWay = items.filter((i) => ["PENDING", "PREPARING", "READY"].includes(i.status));

  const header = (
    <header className="flex items-center gap-3">
      <Link href="/painel" aria-label={t.back} className={buttonVariants({ variant: "outline", size: "icon-lg" })}>
        <ChevronLeft aria-hidden />
      </Link>
      <span className="flex size-14 shrink-0 flex-col items-center justify-center rounded-xl bg-muted leading-none">
        <span className="text-[10px] font-semibold tracking-wide text-muted-foreground uppercase">
          {messages.panel.calls.table}
        </span>
        <span className="text-xl font-bold">{tab.tableLabel}</span>
      </span>
      <span className="flex flex-col">
        <h1 className="text-xl font-bold">{open ? t.openTitle : t.closedTitle}</h1>
        <span className="text-sm text-muted-foreground">
          {tab.sectorName} · {t.since(clock(tab.openedAt))}
        </span>
      </span>
    </header>
  );

  if (!open) {
    return (
      <>
        {header}
        <section role="status" className="flex flex-col items-center gap-4 py-8 text-center">
          <span className="flex size-24 items-center justify-center rounded-full bg-emerald-700/20">
            <span className="flex size-16 items-center justify-center rounded-full bg-emerald-700 text-white">
              <Check aria-hidden className="size-8" />
            </span>
          </span>
          <h2 className="text-2xl font-bold">{t.closedTitle}</h2>
          <p className="font-semibold">
            {t.paidWith(tp[tab.paymentMethod ?? "PIX"], formatCents(tab.totalCents))}
          </p>
          <p className="max-w-sm text-muted-foreground">{t.closedText(tab.tableLabel)}</p>
          <Link href="/painel" className={cn(buttonVariants({ variant: "outline" }), "h-12 w-full max-w-sm")}>
            {t.back}
          </Link>
        </section>
      </>
    );
  }

  return (
    <>
      {header}
      {tab.billRequestedWith && (
        <span className="w-fit rounded-full bg-primary/15 px-3 py-1 text-sm font-semibold text-brand-text">
          {t.billRequested(tp[tab.billRequestedWith])}
        </span>
      )}

      {tab.rodizio ? (
        <section
          aria-label={t.rodizio(tab.rodizio.name, tab.rodizio.people)}
          className="flex flex-col gap-2 rounded-2xl border border-border bg-card p-4"
        >
          <div className="flex items-center justify-between gap-3">
            <h2 className="font-semibold">{t.rodizio(tab.rodizio.name, tab.rodizio.people)}</h2>
            <span className="font-mono font-bold tabular-nums">{formatCents(tab.rodizio.totalCents)}</span>
          </div>
          {tab.rodizio.endsAt && (
            <span className="text-sm font-medium">{t.rodizioEnds(clock(tab.rodizio.endsAt))}</span>
          )}
          <ul className="flex flex-col gap-0.5 text-sm text-muted-foreground">
            {tab.rodizio.guests
              .filter((g) => g.quantity > 0)
              .map((g) => (
                <li key={g.tierName}>{t.guestLine(g.tierName, g.quantity, formatCents(g.unitPriceCents))}</li>
              ))}
          </ul>
          <Button
            variant="outline"
            className="h-11 self-start"
            onClick={() =>
              setRodizioTarget({ tableId: tab.tableId, tableLabel: tab.tableLabel, current: tab.rodizio })
            }
          >
            {t.adjustGuests}
          </Button>
        </section>
      ) : (
        <Button
          variant="outline"
          className="h-11 self-start"
          onClick={() => setRodizioTarget({ tableId: tab.tableId, tableLabel: tab.tableLabel })}
        >
          {t.activateRodizio}
        </Button>
      )}
      <RodizioDialog target={rodizioTarget} onClose={() => setRodizioTarget(null)} />

      <section aria-label={t.items} className="flex flex-col gap-3 rounded-2xl border border-border bg-card p-4">
        {tab.orders.map((order) => (
          <div key={order.id} className="flex flex-col gap-1.5">
            <span className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              {t.order(order.number, clock(order.createdAt))}
              {order.status === "REJECTED" && ` · ${t.rejected}`}
            </span>
            <ul className="flex flex-col gap-1.5">
              {order.items.map((item) => {
                const canceled = item.status === "CANCELED";
                return (
                  <li key={item.id} className="flex items-center gap-2 text-sm">
                    <span className={cn("flex-1", canceled && "text-muted-foreground line-through")}>
                      {item.quantity}× {item.productName}
                    </span>
                    <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium">{ts[item.status]}</span>
                    <span className="w-20 text-right font-mono tabular-nums">
                      {canceled ? "" : item.rodizio ? t.rodizioItem : formatCents(item.unitPriceCents * item.quantity)}
                    </span>
                  </li>
                );
              })}
            </ul>
          </div>
        ))}
        <div className="flex items-baseline justify-between border-t border-border pt-3">
          <span className="font-semibold">{t.total}</span>
          <span className="font-mono text-lg font-bold tabular-nums">{formatCents(tab.totalCents)}</span>
        </div>
      </section>

      <section className="flex flex-col items-center gap-1 rounded-2xl border border-border bg-card p-5 text-center">
        <span className="text-sm text-muted-foreground">{t.toReceive}</span>
        <span className="font-mono text-4xl font-bold tabular-nums">{formatCents(tab.totalCents)}</span>
      </section>

      <fieldset className="flex flex-col gap-2">
        <legend className="pb-2 text-sm font-semibold text-muted-foreground">
          {tab.billRequestedWith ? t.paymentChosen(tp[tab.billRequestedWith]) : t.payment}
        </legend>
        <div className="grid grid-cols-3 gap-2">
          {METHODS.map((m) => (
            <label
              key={m}
              className={cn(
                "flex h-14 cursor-pointer items-center justify-center rounded-xl border border-border font-semibold has-focus-visible:ring-3 has-focus-visible:ring-ring/50",
                method === m && "border-emerald-600 bg-emerald-700/15",
              )}
            >
              <input
                type="radio"
                name="payment"
                value={m}
                checked={method === m}
                onChange={() => setMethod(m)}
                className="sr-only"
              />
              {tp[m]}
            </label>
          ))}
        </div>
      </fieldset>

      {onTheWay.length > 0 && (
        <p className="flex gap-2.5 rounded-xl bg-amber-500/15 p-3.5 text-sm">
          <TriangleAlert aria-hidden className="mt-0.5 size-4 shrink-0 text-amber-700 dark:text-amber-400" />
          {t.onTheWayWarning(onTheWay.map((i) => `${i.quantity}× ${i.productName}`).join(", "))}
        </p>
      )}
      {close.error && (
        <p role="alert" className="text-sm text-destructive">
          {close.error.message}
        </p>
      )}
      <Button
        className="h-13 bg-emerald-700 text-base font-bold text-white hover:bg-emerald-600"
        disabled={close.isPending || onTheWay.length > 0}
        onClick={() => close.mutate()}
      >
        {close.isPending ? t.closing : t.close(tp[method])}
      </Button>
    </>
  );
}
