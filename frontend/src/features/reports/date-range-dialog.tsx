"use client";

import { CalendarRange } from "lucide-react";
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
  DialogTrigger,
} from "@/components/ui/dialog";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { clampRange, formatRange, lengthOf, shortcuts, type DayRange, type Limits } from "./period";

const t = messages.reports;
const d = t.dates;

/**
 * "Escolher datas" (desenho 14.14): atalhos e os campos De e Até (no celular, o calendário do próprio
 * aparelho). O intervalo respeita o histórico do plano, termina até hoje e tem no máximo 12 meses.
 */
export function DateRangeDialog({
  active,
  current,
  limits,
  onApply,
}: {
  active: boolean;
  current: DayRange;
  limits: Limits;
  onApply: (range: DayRange) => void;
}) {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState<DayRange>(current);
  const valid = !!draft.from && !!draft.to && draft.from <= draft.to;
  const range = valid ? clampRange(draft, limits) : null;
  const min = limits.earliestDay ?? undefined;
  const months = limits.earliestDay ? Math.round(lengthOf({ from: limits.earliestDay, to: limits.today }) / 30) : null;

  function change(next: boolean) {
    if (next) setDraft(current);
    setOpen(next);
  }

  return (
    <Dialog open={open} onOpenChange={change}>
      <DialogTrigger
        aria-pressed={active}
        className={cn(
          "inline-flex h-10 items-center gap-1.5 rounded-[10px] px-3.5 text-sm font-semibold text-muted-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
          active && "bg-foreground text-background",
        )}
      >
        <CalendarRange aria-hidden className="size-4" />
        {t.chooseDates}
      </DialogTrigger>
      <DialogContent className="max-h-[calc(100dvh-2rem)] gap-5 overflow-y-auto p-6 sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="text-lg">{d.title}</DialogTitle>
          <DialogDescription>{d.limit(months)}</DialogDescription>
        </DialogHeader>
        <div role="group" aria-label={d.shortcuts} className="flex flex-wrap gap-2">
          {shortcuts(limits.today).map(({ key, range: r }) => {
            const on = draft.from === r.from && draft.to === r.to;
            return (
              <Button
                key={key}
                type="button"
                variant={on ? "default" : "outline"}
                aria-pressed={on}
                className="h-10"
                onClick={() => setDraft(clampRange(r, limits))}
              >
                {d.shortcut[key]}
              </Button>
            );
          })}
        </div>
        <div className="grid grid-cols-2 gap-3.5">
          <label className="flex flex-col gap-1.5 text-sm font-semibold text-muted-foreground">
            {d.from}
            <input
              type="date"
              required
              value={draft.from}
              min={min}
              max={limits.today}
              onChange={(e) => setDraft((r) => ({ ...r, from: e.target.value }))}
              className="h-11 rounded-xl border border-input bg-background px-3 text-base text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
            />
          </label>
          <label className="flex flex-col gap-1.5 text-sm font-semibold text-muted-foreground">
            {d.to}
            <input
              type="date"
              required
              value={draft.to}
              min={min}
              max={limits.today}
              onChange={(e) => setDraft((r) => ({ ...r, to: e.target.value }))}
              className="h-11 rounded-xl border border-input bg-background px-3 text-base text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
            />
          </label>
        </div>
        <p role="status" className={cn("text-sm", range ? "font-semibold" : "text-destructive")}>
          {range ? d.summary(formatRange(range, t.range), lengthOf(range)) : d.invalid}
        </p>
        <DialogFooter className="-mx-6 -mb-6 gap-2 px-6 pb-6">
          <DialogClose render={<Button variant="outline" size="lg" className="h-11 px-4" />}>{d.cancel}</DialogClose>
          <Button
            size="lg"
            className="h-11 px-4"
            disabled={!range}
            onClick={() => {
              if (!range) return;
              onApply(range);
              setOpen(false);
            }}
          >
            {d.apply}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
