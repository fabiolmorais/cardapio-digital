"use client";

import { useQuery } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
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
import { Label } from "@/components/ui/label";
import { Notice } from "@/components/notice";
import { centsToInput, formatCents, parseCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { fetchBill, finishCall, receivePayment, type Bill, type PaymentMethod, type StaffCall } from "./calls-api";

const t = messages.panel.calls;
const r = t.paymentDialog;
const METHODS: PaymentMethod[] = ["PIX", "CARD", "CASH"];

/**
 * "Receber pagamento": o garçom atende o pedido de conta confirmando o valor e a forma. A comanda
 * continua aberta (pode haver item a caminho): o valor sai do que a mesa deve, e no fechamento só
 * se cobra o que faltar. Começa com o que falta e a forma que a mesa escolheu; dá para receber
 * só parte (a mesa dividiu a conta).
 */
export function PaymentDialog({
  call,
  onClose,
  onDone,
}: {
  call: StaffCall | null;
  onClose: () => void;
  onDone: () => Promise<unknown>;
}) {
  return (
    <Dialog open={call !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] gap-5 overflow-y-auto p-6 sm:max-w-md">
        {call && <Body key={call.id} call={call} onClose={onClose} onDone={onDone} />}
      </DialogContent>
    </Dialog>
  );
}

function Body({ call, onClose, onDone }: { call: StaffCall; onClose: () => void; onDone: () => Promise<unknown> }) {
  const bill = useQuery({ queryKey: ["call-bill", call.id], queryFn: () => fetchBill(call.id), retry: false });
  return (
    <>
      <DialogHeader>
        <DialogTitle className="text-lg">{r.title(call.tableLabel)}</DialogTitle>
        <DialogDescription>{r.description}</DialogDescription>
      </DialogHeader>
      {bill.data ? (
        <Form call={call} bill={bill.data} onClose={onClose} onDone={onDone} />
      ) : bill.isError ? (
        <p role="alert" className="text-sm text-destructive">
          {bill.error.message}
        </p>
      ) : (
        <p className="text-sm text-muted-foreground">{r.loading}</p>
      )}
    </>
  );
}

function Form({
  call,
  bill,
  onClose,
  onDone,
}: {
  call: StaffCall;
  bill: Bill;
  onClose: () => void;
  onDone: () => Promise<unknown>;
}) {
  const [amount, setAmount] = useState(centsToInput(bill.dueCents));
  const [method, setMethod] = useState<PaymentMethod>(bill.requestedWith);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const cents = parseCents(amount);
  const valid = cents !== null && !Number.isNaN(cents) && cents >= 1 && cents <= bill.dueCents;
  const nothingDue = bill.dueCents === 0;

  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    setError(null);
    try {
      await action();
      await onDone();
      onClose();
    } catch (e) {
      setError((e as Error).message);
      await onDone(); // a fila mostra se a mesa cancelou ou outra pessoa atendeu
    } finally {
      setBusy(false);
    }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (nothingDue) {
      void run(() => finishCall(call.id));
      return;
    }
    if (!valid) {
      setError(r.invalid(formatCents(bill.dueCents)));
      return;
    }
    void run(() => receivePayment(call.id, cents, method));
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-5">
      <dl className="flex flex-col gap-1.5 rounded-xl bg-muted p-4 text-sm">
        <div className="flex justify-between gap-3">
          <dt className="text-muted-foreground">{r.total}</dt>
          <dd className="font-mono tabular-nums">{formatCents(bill.totalCents)}</dd>
        </div>
        {bill.paidCents > 0 && (
          <div className="flex justify-between gap-3">
            <dt className="text-muted-foreground">{r.paid}</dt>
            <dd className="font-mono text-success tabular-nums">− {formatCents(bill.paidCents)}</dd>
          </div>
        )}
        <div className="flex justify-between gap-3 border-t border-border pt-1.5 text-base font-semibold">
          <dt>{r.due}</dt>
          <dd className="font-mono tabular-nums">{formatCents(bill.dueCents)}</dd>
        </div>
      </dl>

      {bill.itemsOnTheWay > 0 && !nothingDue && <Notice tone="warning">{r.onTheWay(bill.itemsOnTheWay)}</Notice>}

      {nothingDue ? (
        <p className="text-sm text-muted-foreground">{r.nothingDue}</p>
      ) : (
        <>
          <div className="flex flex-col gap-2">
            <Label htmlFor="payment-amount" className="text-muted-foreground">
              {r.amount}
            </Label>
            <Input
              id="payment-amount"
              value={amount}
              onChange={(event) => setAmount(event.target.value)}
              inputMode="decimal"
              autoComplete="off"
              aria-describedby="payment-hint"
              className="h-12 rounded-xl px-4 font-mono text-lg"
            />
            <span id="payment-hint" className="text-sm text-muted-foreground">
              {r.amountHint}
            </span>
          </div>
          <fieldset className="flex flex-col gap-2">
            <legend className="pb-2 text-sm font-semibold text-muted-foreground">{r.method}</legend>
            <div className="grid grid-cols-3 gap-2">
              {METHODS.map((m) => (
                <label
                  key={m}
                  className={cn(
                    "flex h-12 cursor-pointer items-center justify-center rounded-xl border border-border font-semibold has-focus-visible:ring-3 has-focus-visible:ring-ring/50",
                    method === m && "border-emerald-600 bg-emerald-700/15",
                  )}
                >
                  <input
                    type="radio"
                    name="payment-method"
                    value={m}
                    checked={method === m}
                    onChange={() => setMethod(m)}
                    className="sr-only"
                  />
                  {t.payment[m]}
                </label>
              ))}
            </div>
          </fieldset>
        </>
      )}

      <p role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
        {error ?? ""}
      </p>
      <DialogFooter className="-mx-6 -mb-6 gap-2 px-6 pb-6">
        <DialogClose render={<Button type="button" variant="outline" size="lg" className="h-11 px-4" />}>
          {r.cancel}
        </DialogClose>
        <Button
          type="submit"
          size="lg"
          className="h-11 bg-emerald-700 px-4 font-semibold text-white hover:bg-emerald-600"
          disabled={busy}
        >
          {busy ? r.receiving : nothingDue ? t.finish : r.confirm(valid ? formatCents(cents) : "", t.payment[method])}
        </Button>
      </DialogFooter>
    </form>
  );
}
