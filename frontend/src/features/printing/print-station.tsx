"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronLeft, Pause, Play, Printer } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { flushSync } from "react-dom";
import { Button, buttonVariants } from "@/components/ui/button";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { subscribeStaffEvents } from "@/lib/realtime/shared-staff-events";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { claimJob, fetchPendingJobs, fetchPrintedJobs, type PrintJob, type TicketKind } from "./print-api";
import { PrintTicket, type Ticket } from "./print-ticket";

const t = messages.panel.printing;
const PRINTED_KEY = ["printed-jobs"] as const;
/** Mesmo com o tempo real ligado, confere de tempos em tempos: nenhum pedido fica sem comanda. */
const SWEEP_MS = 30_000;
/** O tamanho da página do back: uma página cheia quer dizer que pode haver mais. */
const PAGE = 20;
const KINDS: TicketKind[] = ["ORDER", "CANCELLATION"];
type Paper = "80" | "58";

/** Uma preferência desta tela, guardada no navegador (o computador do balcão). */
function usePreference<T extends string>(key: string, initial: T): [T, (value: T) => void] {
  // A estação só monta no navegador (depois da sessão), então dá para ler já na criação.
  const [value, setValue] = useState<T>(() => {
    try {
      return (localStorage.getItem(key) as T | null) ?? initial;
    } catch {
      return initial; // Sem acesso ao armazenamento: fica o padrão.
    }
  });
  const save = useCallback(
    (next: T) => {
      setValue(next);
      try {
        localStorage.setItem(key, next);
      } catch {
        // Idem.
      }
    },
    [key],
  );
  return [value, save];
}

const TEST_JOB: PrintJob = {
  orderId: "teste",
  kind: "ORDER",
  orderNumber: 0,
  tableLabel: "00",
  sectorName: "Teste",
  items: [
    {
      id: "teste",
      productName: "Comanda de teste",
      quantity: 1,
      rodizio: false,
      choices: { options: [] },
    },
  ],
};

/**
 * A estação de impressão: aberta no computador do balcão, imprime sozinha cada pedido novo e a via
 * "CANCELADO" do item cancelado depois de impresso. Pega cada comanda no back antes de imprimir,
 * então duas telas abertas não imprimem o mesmo pedido, e um F5 não repete nada.
 */
export function PrintStation() {
  const { status, session } = useRequiredSession();
  if (status !== "authenticated") return <p className="p-6 text-muted-foreground">{t.loading}</p>;
  return <Station restaurant={session.establishment.name} />;
}

function Station({ restaurant }: { restaurant: string }) {
  const queryClient = useQueryClient();
  const [paper, setPaper] = usePreference<Paper>("ohchefe:print-paper", "80");
  const [pausedPref, setPausedPref] = usePreference<"sim" | "nao">("ohchefe:print-paused", "nao");
  const paused = pausedPref === "sim";
  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [count, setCount] = useState(0);
  const [connected, setConnected] = useState(false);
  const [failing, setFailing] = useState(false);
  const printed = useQuery({
    queryKey: PRINTED_KEY,
    queryFn: fetchPrintedJobs,
    retry: false,
  });

  const pausedRef = useRef(paused);
  const busy = useRef(false);
  const again = useRef(false);
  useEffect(() => {
    pausedRef.current = paused;
  }, [paused]);

  /** Põe a comanda na área de impressão e chama a impressora (sem janela, no Chrome com --kiosk-printing). */
  const print = useCallback((next: Ticket) => {
    flushSync(() => setTicket(next));
    window.print();
  }, []);

  /** Busca o que falta, pega cada comanda e imprime, até não sobrar nada. Uma volta por vez. */
  const drain = useCallback(async () => {
    if (pausedRef.current) return;
    if (busy.current) {
      again.current = true;
      return;
    }
    busy.current = true;
    try {
      do {
        again.current = false;
        for (const kind of KINDS) {
          const jobs = await fetchPendingJobs(kind);
          if (jobs.length === PAGE) again.current = true;
          for (const job of jobs) {
            if (pausedRef.current) return;
            const claimed = await claimJob(job.orderId, kind);
            if (claimed.items.length === 0) continue;
            print({ job: claimed, variant: "new", printedAt: new Date() });
            setCount((n) => n + 1);
          }
        }
      } while (again.current && !pausedRef.current);
      setFailing(false);
    } catch {
      setFailing(true);
    } finally {
      busy.current = false;
      void queryClient.invalidateQueries({ queryKey: PRINTED_KEY });
    }
  }, [print, queryClient]);

  // O tempo real avisa cada pedido novo (e o "ready" de cada conexão faz uma volta completa).
  useEffect(() => {
    return subscribeStaffEvents((event) => {
      if (event.name === "order" || event.name === "ready") void drain();
    }, setConnected);
  }, [drain]);

  // A volta de segurança: pega o que o tempo real não avisou (internet que caiu e voltou). A
  // primeira volta sai com o "ready" da conexão.
  useEffect(() => {
    const sweep = window.setInterval(() => void drain(), SWEEP_MS);
    return () => window.clearInterval(sweep);
  }, [drain]);

  const ticketWidth = paper === "80" ? "w-[72mm]" : "w-[48mm]";

  return (
    <>
      {/* O tamanho da página da impressora térmica: a largura do papel, e a altura da comanda. */}
      <style>{`@page { size: ${paper}mm auto; margin: 2mm; }`}</style>

      <main className="mx-auto flex w-full max-w-3xl flex-col gap-5 px-4 py-6 print:hidden">
        <Link href="/painel" className={cn(buttonVariants({ variant: "ghost" }), "self-start")}>
          <ChevronLeft aria-hidden className="size-4" />
          {t.back}
        </Link>
        <header className="flex flex-col gap-1">
          <h1 className="flex items-center gap-2 text-2xl font-bold">
            <Printer aria-hidden className="size-6 text-brand-text" />
            {t.title}
          </h1>
          <p className="text-muted-foreground">{t.subtitle}</p>
        </header>

        <section className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-5" aria-label={t.title}>
          <div className="flex flex-wrap items-center gap-3">
            <span
              role="status"
              className={cn(
                "inline-flex items-center gap-2 rounded-full px-3 py-1 text-sm font-semibold",
                connected ? "bg-success/15 text-success" : "bg-muted text-muted-foreground",
              )}
            >
              <span aria-hidden className="size-2 rounded-full bg-current" />
              {connected ? t.live : t.reconnecting}
            </span>
            <span className="text-sm text-muted-foreground">{t.printedToday(count)}</span>
          </div>
          <p className="text-lg font-semibold">{paused ? t.paused : t.on}</p>
          {failing && (
            <p role="alert" className="text-sm text-destructive">
              {t.loadError}
            </p>
          )}
          <div className="flex flex-wrap gap-3">
            <Button
              size="xl"
              variant={paused ? "default" : "secondary"}
              onClick={() => {
                const resuming = paused;
                pausedRef.current = !resuming;
                setPausedPref(resuming ? "nao" : "sim");
                if (resuming) void drain();
              }}
            >
              {paused ? <Play aria-hidden /> : <Pause aria-hidden />}
              {paused ? t.resume : t.pause}
            </Button>
            <Button
              size="xl"
              variant="secondary"
              onClick={() => print({ job: TEST_JOB, variant: "test", printedAt: new Date() })}
            >
              <Printer aria-hidden />
              {t.test}
            </Button>
          </div>
          <fieldset className="flex flex-col gap-2">
            <legend className="pb-1 text-sm font-medium text-muted-foreground">{t.paper}</legend>
            <div className="flex flex-wrap gap-2">
              {(["80", "58"] as const).map((width) => (
                <label
                  key={width}
                  className="flex min-h-11 cursor-pointer items-center gap-2 rounded-xl border border-input px-4 has-checked:border-ring has-checked:bg-muted"
                >
                  <input
                    type="radio"
                    name="paper"
                    value={width}
                    checked={paper === width}
                    onChange={() => setPaper(width)}
                    className="accent-primary"
                  />
                  {t.papers[width]}
                </label>
              ))}
            </div>
          </fieldset>
        </section>

        <section aria-labelledby="impressas" className="flex flex-col gap-3">
          <h2 id="impressas" className="text-lg font-semibold">
            {t.recent}
          </h2>
          {printed.data && printed.data.length === 0 && <p className="text-muted-foreground">{t.recentEmpty}</p>}
          <ul className="flex flex-col gap-2">
            {printed.data?.map((job) => (
              <li
                key={job.orderId}
                className="flex items-center gap-3 rounded-xl border border-border bg-card px-4 py-3"
              >
                <span className="flex min-w-0 flex-1 flex-col">
                  <span className="font-semibold">
                    {messages.panel.kitchen.table} {job.tableLabel} · {messages.panel.kitchen.order(job.orderNumber)}
                  </span>
                  <span className="truncate text-sm text-muted-foreground">
                    {job.items.map((i) => `${i.quantity}× ${i.productName}`).join(", ")}
                  </span>
                </span>
                <Button
                  variant="secondary"
                  aria-label={t.reprintLabel(job.tableLabel, job.orderNumber)}
                  onClick={() => print({ job, variant: "reprint", printedAt: new Date() })}
                >
                  {t.reprint}
                </Button>
              </li>
            ))}
          </ul>
        </section>

        <section aria-labelledby="como-configurar" className="flex flex-col gap-2 rounded-2xl border border-border p-5">
          <h2 id="como-configurar" className="font-semibold">
            {t.howTitle}
          </h2>
          <ol className="flex list-decimal flex-col gap-1.5 pl-5 text-sm text-muted-foreground">
            {t.howSteps.map((step) => (
              <li key={step}>{step}</li>
            ))}
          </ol>
          <p className="text-sm text-muted-foreground">{t.howNote}</p>
        </section>
      </main>

      <div className={cn("hidden print:block", ticketWidth)}>
        {ticket && <PrintTicket ticket={ticket} restaurant={restaurant} />}
      </div>
    </>
  );
}
