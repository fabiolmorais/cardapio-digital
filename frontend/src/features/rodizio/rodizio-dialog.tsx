"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Minus, Plus } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { activateRodizio, changeGuests, fetchQuotes, type GuestCount, type TableRodizio } from "./rodizio-api";

const t = messages.panel.rodizio;

/** A mesa do diálogo e, para ajustar as pessoas, o rodízio que ela já tem. */
export type RodizioTarget = { tableId: string; tableLabel: string; current?: TableRodizio | null };

/**
 * "Ativar rodízio" (desenho 7.3): o rodízio, as pessoas por faixa com + e −, e quanto entra na
 * comanda. Com `current`, é o "Ajustar pessoas": o rodízio não muda, e o preço é o copiado na
 * ativação. O total aqui é só uma prévia: quem calcula de verdade é o back.
 */
export function RodizioDialog({ target, onClose }: { target: RodizioTarget | null; onClose: () => void }) {
  return (
    <Dialog open={target !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] gap-5 overflow-y-auto p-6 sm:max-w-lg">
        {target && <Body key={target.tableId} target={target} onDone={onClose} />}
      </DialogContent>
    </Dialog>
  );
}

type Tier = { name: string; priceCents: number };

function Body({ target, onDone }: { target: RodizioTarget; onDone: () => void }) {
  const adjusting = Boolean(target.current);
  const quotes = useQuery({ queryKey: ["rodizio-quotes"], queryFn: fetchQuotes, enabled: !adjusting });
  const [chosen, setChosen] = useState<string | null>(null);
  const [counts, setCounts] = useState<Record<string, number>>(() =>
    Object.fromEntries((target.current?.guests ?? []).map((g) => [g.tierName, g.quantity])),
  );
  const queryClient = useQueryClient();

  const options = quotes.data ?? [];
  const rodizio = options.find((r) => r.id === chosen) ?? options[0];
  const tiers: Tier[] = target.current
    ? target.current.guests.map((g) => ({ name: g.tierName, priceCents: g.unitPriceCents }))
    : (rodizio?.tiers ?? []);
  const people = tiers.reduce((sum, tier) => sum + (counts[tier.name] ?? 0), 0);
  const total = tiers.reduce((sum, tier) => sum + (counts[tier.name] ?? 0) * tier.priceCents, 0);
  const guests: GuestCount[] = tiers.map((tier) => ({ tierName: tier.name, quantity: counts[tier.name] ?? 0 }));

  const save = useMutation({
    mutationFn: () =>
      adjusting ? changeGuests(target.tableId, guests) : activateRodizio(target.tableId, rodizio!.id, guests),
    onSuccess: async () => {
      await Promise.all(
        ["tab", "open-tabs", "call-queue"].map((key) => queryClient.invalidateQueries({ queryKey: [key] })),
      );
      onDone();
    },
  });

  function change(tier: string, delta: number) {
    setCounts((c) => ({ ...c, [tier]: Math.max(0, Math.min(99, (c[tier] ?? 0) + delta)) }));
  }

  const option = (on: boolean) =>
    cn(
      "flex flex-1 flex-col gap-0.5 rounded-xl border border-border px-3.5 py-3 text-left focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
      on && "border-primary bg-primary/10",
    );

  return (
    <>
      <DialogHeader>
        <DialogTitle>
          {adjusting ? t.adjustTitle : t.activateTitle} · {t.table(target.tableLabel)}
        </DialogTitle>
        <DialogDescription>{adjusting ? t.adjustText : t.activateText}</DialogDescription>
      </DialogHeader>

      {!adjusting &&
        (quotes.isPending ? (
          <p className="text-sm text-muted-foreground">{t.loading}</p>
        ) : quotes.isError ? (
          <p role="alert" className="text-sm text-destructive">
            {quotes.error.message}
          </p>
        ) : options.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t.none}</p>
        ) : (
          options.length > 1 && (
            <fieldset className="flex flex-col gap-2">
              <legend className="pb-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                {t.choose}
              </legend>
              <div className="flex flex-wrap gap-2">
                {options.map((r) => (
                  <button
                    key={r.id}
                    type="button"
                    aria-pressed={r.id === rodizio?.id}
                    onClick={() => setChosen(r.id)}
                    className={option(r.id === rodizio?.id)}
                  >
                    <span className="font-semibold">{r.name}</span>
                    {r.tiers[0] && (
                      <span className="text-sm text-muted-foreground">
                        {formatCents(r.tiers[0].priceCents)} · {r.tiers[0].name}
                      </span>
                    )}
                  </button>
                ))}
              </div>
            </fieldset>
          )
        ))}

      {tiers.length > 0 && (
        <fieldset className="flex flex-col gap-2">
          <legend className="pb-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            {adjusting ? target.current!.name : rodizio?.name} · {t.people}
          </legend>
          {tiers.map((tier) => {
            const n = counts[tier.name] ?? 0;
            return (
              <div key={tier.name} className="flex items-center gap-3 rounded-xl border border-border bg-muted/40 px-3.5 py-2.5">
                <span className="flex flex-1 flex-col">
                  <span className="font-semibold">{tier.name}</span>
                  <span className="text-sm text-muted-foreground">{t.each(formatCents(tier.priceCents))}</span>
                </span>
                <Button
                  type="button"
                  variant="outline"
                  size="icon-lg"
                  aria-label={t.fewer(tier.name)}
                  disabled={n === 0}
                  onClick={() => change(tier.name, -1)}
                >
                  <Minus aria-hidden />
                </Button>
                <output aria-label={tier.name} className="w-8 text-center text-lg font-bold tabular-nums">
                  {n}
                </output>
                <Button
                  type="button"
                  size="icon-lg"
                  aria-label={t.more(tier.name)}
                  onClick={() => change(tier.name, 1)}
                >
                  <Plus aria-hidden />
                </Button>
              </div>
            );
          })}
          <div className="flex items-baseline justify-between rounded-xl bg-muted px-4 py-3">
            <span className="font-semibold">{t.total}</span>
            <span className="font-mono text-2xl font-bold tabular-nums">{formatCents(total)}</span>
          </div>
        </fieldset>
      )}

      {save.error && (
        <p role="alert" className="text-sm text-destructive">
          {save.error.message}
        </p>
      )}
      <DialogFooter>
        <DialogClose render={<Button variant="outline" className="h-11" />}>{t.cancel}</DialogClose>
        <Button
          className="h-11"
          disabled={save.isPending || people === 0 || (!adjusting && !rodizio)}
          onClick={() => save.mutate()}
        >
          {save.isPending ? t.saving : adjusting ? t.save : t.activate}
        </Button>
      </DialogFooter>
      {people === 0 && tiers.length > 0 && <p className="-mt-3 text-sm text-muted-foreground">{t.noPeople}</p>}
    </>
  );
}
