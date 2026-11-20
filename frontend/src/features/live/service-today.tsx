"use client";

import { ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import type { Schemas } from "@/lib/api/client";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";

const t = messages.panel.service;

/**
 * A taxa de serviço de hoje (RF39) no card do faturado: sempre o total. Na regra "garçom que
 * atendeu", o total vira um botão que abre a divisão por garçom num modal; o que caiu na caixinha
 * da equipe (a regra mudou durante o dia, ou a comanda ficou sem garçom) aparece numa linha à parte.
 */
export function ServiceToday({ service }: { service: Schemas["ServiceToday"] }) {
  const total = formatCents(service.totalCents);
  if (service.byWaiter.length === 0) return <>{t.today(total)}</>;

  const waiters = [...service.byWaiter].sort((a, b) => b.amountCents - a.amountCents);
  const team = service.totalCents - waiters.reduce((sum, w) => sum + w.amountCents, 0);
  const row = "flex items-center justify-between gap-4 py-3";

  return (
    <Dialog>
      <DialogTrigger className="-mx-1 flex min-h-11 flex-col items-start rounded-md px-1 text-left text-[13px] text-muted-foreground hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none">
        <span>{t.todayTotal(total)}</span>
        <span className="flex items-center gap-0.5 font-semibold text-foreground underline underline-offset-2">
          {t.seeByWaiter}
          <ChevronRight aria-hidden className="size-4 shrink-0" />
        </span>
      </DialogTrigger>
      <DialogContent className="max-h-[calc(100dvh-2rem)] gap-5 overflow-y-auto p-6 sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="text-lg">{t.byWaiterTitle}</DialogTitle>
          <DialogDescription>{t.byWaiterHint}</DialogDescription>
        </DialogHeader>
        <ul className="divide-y divide-border text-[15px]">
          {waiters.map((w, i) => (
            <li key={`${w.name}-${i}`} className={row}>
              <span className="min-w-0 truncate">{w.name}</span>
              <span className="font-mono font-semibold whitespace-nowrap">{formatCents(w.amountCents)}</span>
            </li>
          ))}
          {team > 0 && (
            <li className={row}>
              <span className="text-muted-foreground">{t.teamShare}</span>
              <span className="font-mono font-semibold whitespace-nowrap">{formatCents(team)}</span>
            </li>
          )}
          <li className={row}>
            <span className="font-bold">{t.total}</span>
            <span className="font-mono font-bold whitespace-nowrap">{total}</span>
          </li>
        </ul>
        <DialogFooter className="-mx-6 -mb-6 px-6 pb-6">
          <DialogClose render={<Button variant="outline" size="lg" className="h-11 px-4" />}>{t.close}</DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
