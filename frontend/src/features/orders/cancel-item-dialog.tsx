"use client";

import { useMutation } from "@tanstack/react-query";
import { Minus, Plus } from "lucide-react";
import { useId, useState } from "react";
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
import { Input } from "@/components/ui/input";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { cancelItem } from "./orders-api";

/** O que o diálogo precisa de um item: serve à fila da cozinha e à dos cortes do passagem (sem número). */
export type CancelableItem = { id: string; tableLabel: string; productName: string; quantity: number; orderNumber?: number };

const t = messages.panel.cancelItem;
const OTHER = -1;

/**
 * "Cancelar item" (desenho 9.2): qual item da ficha, quantos (pediram 10, mas só dá para fazer 5),
 * o motivo (os comuns num toque, ou escrito) e a prévia do que o cliente vê. O motivo é
 * obrigatório: o back recusa sem ele.
 */
export function CancelItemDialog({
  items,
  onClose,
  onDone,
}: {
  items: CancelableItem[] | null;
  onClose: () => void;
  onDone: () => void;
}) {
  return (
    <Dialog open={items !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] gap-5 overflow-y-auto p-6 sm:max-w-lg">
        {items && items.length > 0 && <Body key={items[0].id} items={items} onDone={onDone} />}
      </DialogContent>
    </Dialog>
  );
}

function Body({ items, onDone }: { items: CancelableItem[]; onDone: () => void }) {
  const id = useId();
  const [itemId, setItemId] = useState(items[0].id);
  const [choice, setChoice] = useState(0);
  const [other, setOther] = useState("");
  const item = items.find((i) => i.id === itemId) ?? items[0];
  // Quantos cancelar: começa em todos; volta a todos ao trocar de item.
  const [amount, setAmount] = useState(item.quantity);
  const count = Math.min(amount, item.quantity);
  const reason = choice === OTHER ? other.trim() : t.reasons[choice];
  const cancel = useMutation({
    mutationFn: () => cancelItem(item.id, reason, count < item.quantity ? count : undefined),
    onSuccess: onDone,
  });

  const option = (on: boolean) =>
    cn(
      "flex min-h-12 items-center gap-3 rounded-xl border border-border px-4 text-left font-medium focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
      on && "border-destructive bg-destructive/10",
    );
  const chip = (on: boolean) =>
    cn(
      "h-11 rounded-full border border-border px-4 text-sm font-semibold focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
      on && "border-transparent bg-foreground text-background",
    );

  return (
    <>
      <DialogHeader>
        <DialogTitle className="text-lg">{t.title}</DialogTitle>
        <DialogDescription>{t.subtitle(item.tableLabel, item.orderNumber)}</DialogDescription>
      </DialogHeader>

      <fieldset className="flex flex-col gap-2">
        <legend className="pb-2 text-sm font-semibold text-muted-foreground">{t.which}</legend>
        {items.map((i) => (
          <label key={i.id} className={option(i.id === itemId)}>
            <input
              type="radio"
              name={`${id}item`}
              checked={i.id === itemId}
              onChange={() => {
                setItemId(i.id);
                setAmount(i.quantity);
              }}
              className="size-5 accent-destructive"
            />
            {i.quantity}× {i.productName}
          </label>
        ))}
      </fieldset>

      {item.quantity > 1 && (
        <fieldset className="flex flex-col gap-2">
          <legend className="pb-2 text-sm font-semibold text-muted-foreground">{t.howMany}</legend>
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-1 rounded-xl border border-border p-1">
              <Button
                variant="ghost"
                size="icon-lg"
                className="size-11"
                aria-label={t.less}
                disabled={count <= 1}
                onClick={() => setAmount(count - 1)}
              >
                <Minus aria-hidden />
              </Button>
              <output aria-live="polite" className="w-10 text-center text-lg font-bold tabular-nums">
                {count}
              </output>
              <Button
                variant="ghost"
                size="icon-lg"
                className="size-11"
                aria-label={t.more}
                disabled={count >= item.quantity}
                onClick={() => setAmount(count + 1)}
              >
                <Plus aria-hidden />
              </Button>
            </div>
            <span className="text-sm text-muted-foreground">
              {count < item.quantity ? t.keeps(item.quantity - count) : t.all(item.quantity)}
            </span>
          </div>
        </fieldset>
      )}

      <fieldset className="flex flex-col gap-2">
        <legend className="pb-2 text-sm font-semibold text-muted-foreground">{t.reason}</legend>
        <div className="flex flex-wrap gap-2">
          {t.reasons.map((text, index) => (
            <button
              key={text}
              type="button"
              aria-pressed={choice === index}
              onClick={() => setChoice(index)}
              className={chip(choice === index)}
            >
              {text}
            </button>
          ))}
          <button
            type="button"
            aria-pressed={choice === OTHER}
            onClick={() => setChoice(OTHER)}
            className={chip(choice === OTHER)}
          >
            {t.other}
          </button>
        </div>
        {choice === OTHER && (
          <Input
            aria-label={t.otherLabel}
            placeholder={t.otherLabel}
            value={other}
            maxLength={140}
            autoFocus
            onChange={(event) => setOther(event.target.value)}
            className="h-11 rounded-xl"
          />
        )}
      </fieldset>

      <div className="flex flex-col gap-1 rounded-xl bg-muted p-4">
        <span className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">{t.preview}</span>
        <span>
          {count}× {item.productName}: {reason || "…"}
        </span>
      </div>

      <p role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
        {cancel.error?.message ?? ""}
      </p>
      <DialogFooter className="-mx-6 -mb-6 gap-2 px-6 pb-6">
        <DialogClose render={<Button variant="outline" size="lg" className="h-12 px-4" />}>{t.back}</DialogClose>
        <Button
          variant="destructive"
          size="lg"
          className="h-12 px-4"
          disabled={!reason || cancel.isPending}
          onClick={() => cancel.mutate()}
        >
          {cancel.isPending ? t.canceling : t.confirm}
        </Button>
      </DialogFooter>
    </>
  );
}
