"use client";

import { Flame, Pause, Play } from "lucide-react";
import { useState } from "react";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";
import { MesaNotice } from "./mesa-notice";
import { turnCard, type TableOrders } from "./table-orders";

const t = tablePage.rodizio;

/**
 * O cartão verde/vermelho do rodízio de passagem (desenho Rodízio de passagem): verde, os
 * passadores servem; vermelho, a mesa pausou. Um toque vira o cartão; a cor nova chega pelo tempo
 * real (o mesmo aviso que os passadores recebem), e a tela mostra a que o back gravou.
 */
export function PassagemCard({ token, rodizio }: { token: string; rodizio: NonNullable<TableOrders["rodizio"]> }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Enquanto o aviso não volta, a tela já mostra a cor pedida (e volta atrás se der erro).
  const [wanted, setWanted] = useState<typeof rodizio.status | null>(null);
  const status = wanted ?? rodizio.status;
  const paused = status === "PAUSED";

  async function flip() {
    const next = paused ? "SERVING" : "PAUSED";
    setBusy(true);
    setError(null);
    setWanted(next);
    try {
      await turnCard(token, next);
    } catch (e) {
      setWanted(null);
      setError((e as Error).message);
    }
    setBusy(false);
  }

  // Quando o aviso do tempo real traz a cor gravada, a pedida sai de cena.
  if (wanted !== null && rodizio.status === wanted && !busy) setWanted(null);

  return (
    <section id="rodizio" aria-label={t.card.label} className="flex scroll-mt-6 flex-col gap-3">
      <span className="flex items-center gap-2 text-sm font-semibold text-(--mesa-muted)">
        <Flame aria-hidden className="size-4 text-(--brand-text)" />
        {t.banner(rodizio.name, rodizio.people)}
      </span>
      <div
        role="status"
        aria-live="polite"
        className={cn(
          "flex flex-col gap-3.5 rounded-[28px] p-5.5 text-white shadow-lg",
          paused ? "bg-[#B91C1C] shadow-red-900/40" : "bg-[#15803D] shadow-green-900/40",
        )}
      >
        <span className="text-xs font-bold tracking-wider uppercase opacity-90">
          {paused ? t.card.redTag : t.card.greenTag}
        </span>
        <span className="text-4xl font-bold tracking-tight">{paused ? t.card.red : t.card.green}</span>
        <span className="text-white/90">{paused ? t.card.redText : t.card.greenText}</span>
        <button
          type="button"
          disabled={busy}
          onClick={flip}
          className={cn(
            "flex h-12 items-center gap-2 self-start rounded-full bg-white px-5 font-bold focus-visible:ring-3 focus-visible:ring-white/70 focus-visible:ring-offset-2 focus-visible:ring-offset-transparent focus-visible:outline-none disabled:opacity-80",
            paused ? "text-[#15803D]" : "text-[#B91C1C]",
          )}
        >
          {paused ? (
            <Play aria-hidden className="size-4 fill-current" />
          ) : (
            <Pause aria-hidden className="size-4 fill-current" />
          )}
          {busy ? t.card.turning : paused ? t.card.resume : t.card.pause}
        </button>
      </div>
      {error && (
        <MesaNotice tone="warning" role="alert">
          {error}
        </MesaNotice>
      )}
    </section>
  );
}
