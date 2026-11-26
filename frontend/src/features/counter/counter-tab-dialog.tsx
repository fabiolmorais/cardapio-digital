"use client";

import { useMutation } from "@tanstack/react-query";
import { type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogClose, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ErrorText } from "@/features/subscription/error-text";
import { messages } from "@/messages/pt-BR";
import { openCounterTab, renameCounterTab, type CounterTab } from "./counter-api";

const t = messages.panel.counter.form;

/**
 * "Nova comanda no balcão" (desenho 10.7) ou, com `tab`, "Nome e cartão": como chamar o cliente e o
 * número do cartão, se a casa usa.
 */
export function CounterTabDialog({
  open,
  onOpenChange,
  counter,
  tab,
  onSaved,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** O balcão em que a comanda abre (na correção, não é usado). */
  counter?: { id: string; label: string };
  tab?: Pick<CounterTab, "id" | "customerName" | "cardNumber">;
  onSaved: (saved: CounterTab) => void;
}) {
  const mutation = useMutation({
    mutationFn: ({ name, card }: { name: string; card: string }) =>
      tab ? renameCounterTab(tab.id, name, card) : openCounterTab(counter!.id, name, card),
    onSuccess: (saved) => {
      onOpenChange(false);
      onSaved(saved);
    },
  });

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    mutation.mutate({
      name: String(form.get("name") ?? "").trim(),
      card: String(form.get("card") ?? "").trim(),
    });
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) mutation.reset();
        onOpenChange(next);
      }}
    >
      <DialogContent className="max-h-[calc(100dvh-2rem)] gap-5 overflow-y-auto p-6 sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="text-lg">{tab ? t.renameTitle : t.title}</DialogTitle>
        </DialogHeader>
        <form onSubmit={onSubmit} className="flex flex-col gap-5">
          <div className="flex flex-col gap-2">
            <Label htmlFor="counter-tab-name" className="text-muted-foreground">
              {t.name}
            </Label>
            <Input
              id="counter-tab-name"
              name="name"
              required
              maxLength={40}
              autoComplete="off"
              defaultValue={tab?.customerName ?? ""}
              aria-describedby="counter-tab-name-hint"
              className="h-12 rounded-xl px-4 text-base"
            />
            <span id="counter-tab-name-hint" className="text-sm text-muted-foreground">
              {t.nameHint}
            </span>
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="counter-tab-card" className="text-muted-foreground">
              {t.card}
            </Label>
            <Input
              id="counter-tab-card"
              name="card"
              maxLength={10}
              inputMode="numeric"
              autoComplete="off"
              defaultValue={tab?.cardNumber ?? ""}
              aria-describedby="counter-tab-card-hint"
              className="h-12 rounded-xl px-4 font-mono text-base"
            />
            <span id="counter-tab-card-hint" className="text-sm text-muted-foreground">
              {t.cardHint}
            </span>
          </div>
          {!tab && counter && (
            <p className="text-sm text-muted-foreground">
              {t.counter}: <span className="font-semibold text-foreground">{counter.label}</span>
            </p>
          )}
          <p role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
            {mutation.error && <ErrorText message={mutation.error.message} />}
          </p>
          <DialogFooter className="-mx-6 -mb-6 gap-2 px-6 pb-6">
            <DialogClose render={<Button variant="outline" size="lg" className="h-11 px-4" />}>{t.cancel}</DialogClose>
            <Button type="submit" size="lg" className="h-11 px-4" disabled={mutation.isPending}>
              {mutation.isPending ? (tab ? t.saving : t.opening) : tab ? t.save : t.open}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
