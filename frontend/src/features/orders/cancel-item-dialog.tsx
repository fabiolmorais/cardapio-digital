"use client";

import { useMutation } from "@tanstack/react-query";
import { Minus, Plus, X } from "lucide-react";
import { useId, useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { TableBox } from "@/features/live/live-ui";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { cancelItem } from "./orders-api";

/** O que o diálogo precisa de um item: serve à fila da cozinha e à dos cortes do passagem (sem número). */
export type CancelableItem = {
  id: string;
  tableLabel: string;
  productName: string;
  quantity: number;
  orderNumber?: number;
};

const t = messages.panel.cancelItem;
const OTHER = -1;

/**
 * "Cancelar item" (desenho 9.2): a mesa e o pedido no topo, qual item da ficha, quantos (pediram 10,
 * mas só dá para fazer 5), o motivo (os comuns num toque, ou escrito) e a prévia do que o cliente
 * vê, igual ao aviso da mesa. O motivo é obrigatório: o back recusa sem ele.
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
      <DialogContent
        showCloseButton={false}
        className="max-h-[calc(100dvh-2rem)] gap-[18px] overflow-y-auto rounded-3xl border border-border bg-card p-6 shadow-[0_30px_80px_rgb(0_0_0/60%)] ring-0 sm:max-w-[560px]"
      >
        {items && items.length > 0 && <Body key={items[0].id} items={items} onDone={onDone} />}
      </DialogContent>
    </Dialog>
  );
}

const label = "text-[11px] font-semibold tracking-[.6px] text-muted-foreground/80 uppercase";
const focus = "focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none";

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
  const name = count > 1 ? `${count}× ${item.productName}` : item.productName;
  const cancel = useMutation({
    mutationFn: () => cancelItem(item.id, reason, count < item.quantity ? count : undefined),
    onSuccess: onDone,
  });

  const chip = (on: boolean) =>
    cn(
      "h-11 rounded-full border border-foreground/15 bg-muted px-4 text-sm font-semibold",
      focus,
      on && "border-transparent bg-foreground text-background",
    );

  return (
    <>
      <div className="flex items-start gap-3">
        <TableBox label={item.tableLabel} />
        <div className="flex flex-1 flex-col gap-0.5">
          <DialogTitle className="text-xl font-semibold tracking-tight">{t.title}</DialogTitle>
          <DialogDescription className="text-[13px]">{t.subtitle(item.orderNumber)}</DialogDescription>
        </div>
        <DialogClose
          aria-label={t.close}
          className={cn(
            "flex size-10 shrink-0 items-center justify-center rounded-xl border border-border bg-muted",
            focus,
          )}
        >
          <X aria-hidden className="size-4.5" />
        </DialogClose>
      </div>

      <fieldset className="flex flex-col gap-2">
        <legend className={cn(label, "pb-2")}>{t.which}</legend>
        {items.map((i) => {
          const on = i.id === itemId;
          return (
            <label
              key={i.id}
              className={cn(
                "relative flex min-h-12 items-center gap-3 rounded-[14px] border border-foreground/15 bg-muted px-3.5 py-3 has-focus-visible:ring-3 has-focus-visible:ring-ring/50",
                on && "border-destructive/70 bg-destructive/15",
              )}
            >
              {/* Rádio de verdade, invisível por cima do cartão inteiro: o toque em qualquer ponto vale. */}
              <input
                type="radio"
                name={`${id}item`}
                checked={on}
                onChange={() => {
                  setItemId(i.id);
                  setAmount(i.quantity);
                }}
                className="absolute inset-0 size-full cursor-pointer opacity-0"
              />
              <span
                aria-hidden
                className={cn(
                  "flex size-[22px] shrink-0 items-center justify-center rounded-full border-2 border-foreground/15",
                  on && "border-destructive",
                )}
              >
                {on && <span className="size-2.5 rounded-full bg-destructive" />}
              </span>
              <span className="flex-1 font-semibold">
                {i.quantity}× {i.productName}
              </span>
            </label>
          );
        })}
      </fieldset>

      {item.quantity > 1 && (
        <fieldset className="flex flex-col gap-2">
          <legend className={cn(label, "pb-2")}>{t.howMany}</legend>
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-1 rounded-[14px] border border-foreground/15 bg-muted p-1">
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
        <legend className={cn(label, "pb-2")}>{t.reason}</legend>
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

      <div className="flex flex-col gap-1.5 rounded-[14px] bg-secondary px-4 py-3.5">
        <span className={label}>{t.preview}</span>
        <span className="text-sm">{t.previewText(name, reason || "…")}</span>
      </div>

      {cancel.error && (
        <p role="alert" className="text-sm text-destructive">
          {cancel.error.message}
        </p>
      )}
      <div className="flex gap-2.5">
        <DialogClose render={<Button variant="ghost" className="h-13 flex-1 text-base font-semibold" />}>
          {t.back}
        </DialogClose>
        <Button
          className="h-13 flex-2 bg-[#B91C1C] text-base font-semibold text-white hover:bg-[#991B1B]"
          disabled={!reason || cancel.isPending}
          onClick={() => cancel.mutate()}
        >
          {cancel.isPending ? t.canceling : t.confirm}
        </Button>
      </div>
    </>
  );
}
