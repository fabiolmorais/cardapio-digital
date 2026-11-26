"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogClose, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { sectorsOf, type LiveTable } from "@/features/live/live-api";
import { LIVE_FLOOR_KEY, useLiveFloor } from "@/features/live/use-live-floor";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { moveTab, type MovedTab } from "./tabs-api";

const t = messages.panel.moveTab;

/** A comanda que vai mudar: o id, a mesa de agora e o total (para a confirmação da junção). */
/** `counterName`: a comanda é do balcão (ADR 0015); o texto fala do cliente, e não da mesa. */
export type MoveTarget = {
  tabId: string;
  tableId: string;
  tableLabel: string;
  totalCents: number;
  counterName?: string | null;
};

function fromText(target: MoveTarget) {
  return target.counterName ? t.fromCounter(target.counterName) : t.fromTable(target.tableLabel);
}

/**
 * "Mudar de mesa" (RF44, ADR 0012): as mesas do salão por setor, as livres e as com comanda (que
 * se juntam), e a confirmação. A conta de quanto fica na junção é só para o garçom conferir: quem
 * soma de verdade é o back.
 */
export function MoveTabDialog({
  target,
  onClose,
  onMoved,
}: {
  target: MoveTarget | null;
  onClose: () => void;
  onMoved: (moved: MovedTab, to: LiveTable) => void;
}) {
  return (
    <Dialog open={target !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] gap-5 overflow-y-auto p-6 sm:max-w-lg">
        {target && <Picker key={target.tabId} target={target} onMoved={onMoved} />}
      </DialogContent>
    </Dialog>
  );
}

function Picker({ target, onMoved }: { target: MoveTarget; onMoved: (moved: MovedTab, to: LiveTable) => void }) {
  const queryClient = useQueryClient();
  const floor = useLiveFloor();
  const [chosen, setChosen] = useState<LiveTable | null>(null);
  const move = useMutation({
    mutationFn: (to: LiveTable) => moveTab(target.tabId, to.id),
    onSuccess: (moved, to) => {
      void queryClient.invalidateQueries({ queryKey: LIVE_FLOOR_KEY });
      void queryClient.invalidateQueries({ queryKey: ["open-tabs"] });
      void queryClient.invalidateQueries({ queryKey: ["tab"] });
      onMoved(moved, to);
    },
  });

  // O destino é sempre uma mesa (ADR 0015): o balcão não aparece na lista.
  const others = (floor.data?.tables ?? []).filter((table) => table.id !== target.tableId && table.kind !== "COUNTER");

  if (chosen) {
    const merging = chosen.tabId != null;
    return (
      <>
        <DialogHeader>
          <DialogTitle className="text-lg">{t.title(fromText(target))}</DialogTitle>
        </DialogHeader>
        <p className="text-base">
          {merging
            ? t.confirmMerge(
                fromText(target),
                formatCents(target.totalCents),
                chosen.label,
                formatCents(chosen.tabTotalCents),
                formatCents(target.totalCents + chosen.tabTotalCents),
              )
            : t.confirmMove(fromText(target), chosen.label)}
        </p>
        <p role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
          {move.error?.message ?? ""}
        </p>
        <DialogFooter className="-mx-6 -mb-6 gap-2 px-6 pb-6">
          <Button
            variant="outline"
            size="lg"
            className="h-11 px-4"
            disabled={move.isPending}
            onClick={() => {
              move.reset();
              setChosen(null);
            }}
          >
            {t.back}
          </Button>
          <Button size="lg" className="h-11 px-4" disabled={move.isPending} onClick={() => move.mutate(chosen)}>
            {move.isPending ? t.moving : merging ? t.merge : t.move(chosen.label)}
          </Button>
        </DialogFooter>
      </>
    );
  }

  return (
    <>
      <DialogHeader>
        <DialogTitle className="text-lg">{t.title(fromText(target))}</DialogTitle>
      </DialogHeader>
      <p className="text-sm text-muted-foreground">{t.hint}</p>
      {floor.isPending && <p className="text-muted-foreground">{t.loading}</p>}
      {floor.isError && (
        <p role="alert" className="text-destructive">
          {t.loadError}
        </p>
      )}
      {floor.data && others.length === 0 && <p className="text-muted-foreground">{t.none}</p>}
      {sectorsOf(others).map((sector) => {
        const inSector = others.filter((table) => table.sectorName === sector);
        const free = inSector.filter((table) => table.tabId == null);
        const busy = inSector.filter((table) => table.tabId != null);
        return (
          <section key={sector} aria-label={sector} className="flex flex-col gap-3">
            <h3 className="font-semibold">{sector}</h3>
            {free.length > 0 && <Group title={t.free} tables={free} onPick={setChosen} />}
            {busy.length > 0 && <Group title={t.occupied} tables={busy} onPick={setChosen} withTotal />}
          </section>
        );
      })}
      <DialogFooter className="-mx-6 -mb-6 gap-2 px-6 pb-6">
        <DialogClose render={<Button variant="outline" size="lg" className="h-11 px-4" />}>
          {messages.common.cancel}
        </DialogClose>
      </DialogFooter>
    </>
  );
}

function Group({
  title,
  tables,
  onPick,
  withTotal = false,
}: {
  title: string;
  tables: LiveTable[];
  onPick: (table: LiveTable) => void;
  withTotal?: boolean;
}) {
  return (
    <div className="flex flex-col gap-2">
      <span className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">{title}</span>
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
        {tables.map((table) => (
          <Button
            key={table.id}
            variant="outline"
            className={cn("h-auto min-h-12 flex-col gap-0.5 py-2", withTotal && "border-primary/40")}
            onClick={() => onPick(table)}
          >
            <span className="font-semibold">{t.table(table.label)}</span>
            {withTotal && (
              <span className="text-xs font-normal text-muted-foreground">{formatCents(table.tabTotalCents)}</span>
            )}
          </Button>
        ))}
      </div>
    </div>
  );
}
