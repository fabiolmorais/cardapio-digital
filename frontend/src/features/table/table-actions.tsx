"use client";

import { Bell, Receipt, UserCheck } from "lucide-react";
import Link from "next/link";
import { tablePage } from "@/messages/table.pt-BR";
import { requestCall, cancelCall, useTableCalls } from "./table-calls";

const t = tablePage;

/**
 * Os dois botões grandes da mesa (desenho Main) e o aviso do chamado aberto: "Chamado enviado"
 * enquanto ninguém aceitou, "Garçom a caminho" depois. Tocar de novo não duplica: o back devolve
 * o mesmo chamado. "Pedir a conta" abre a tela da forma de pagamento.
 */
export function TableActions({ token }: { token: string }) {
  const { waiter, bill, busy, error, run } = useTableCalls(token);
  const coming = waiter?.status === "IN_PROGRESS";

  return (
    <>
      {waiter && (
        <section
          role="status"
          className="flex flex-col gap-3.5 rounded-3xl border border-(--mesa-stroke) bg-(--mesa-glass) p-4"
        >
          <div className="flex items-center gap-3.5">
            <span className="flex size-12 shrink-0 items-center justify-center rounded-full bg-(--brand) text-white">
              {coming ? <UserCheck aria-hidden className="size-5" /> : <Bell aria-hidden className="size-5" />}
            </span>
            <span className="flex flex-col gap-0.5">
              <span className="font-semibold">{coming ? t.calls.comingTitle : t.calls.pendingTitle}</span>
              <span className="text-sm text-(--mesa-muted)">
                {coming ? t.calls.comingText(waiter.waiterFirstName) : t.calls.pendingText}
              </span>
            </span>
          </div>
          <button
            type="button"
            disabled={busy !== null}
            onClick={() => run("cancel", () => cancelCall(token, waiter.id))}
            className="h-11 rounded-2xl border border-(--mesa-stroke) text-sm font-semibold focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none disabled:opacity-60"
          >
            {busy === "cancel" ? t.calls.canceling : t.calls.cancel}
          </button>
        </section>
      )}

      <section aria-label={t.actions} className="grid grid-cols-2 gap-3">
        <button
          type="button"
          aria-pressed={waiter !== null}
          disabled={busy !== null}
          onClick={() => run("waiter", () => requestCall(token, { type: "WAITER" }))}
          className="flex h-32 flex-col items-start justify-between rounded-3xl bg-(--brand) p-4 text-left text-white focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:ring-offset-2 focus-visible:ring-offset-(--mesa-bg) focus-visible:outline-none disabled:opacity-80"
        >
          <span className="flex size-10 items-center justify-center rounded-xl bg-white/20">
            <Bell aria-hidden className="size-5" />
          </span>
          <span className="flex flex-col">
            <span className="font-semibold">{t.callWaiter}</span>
            <span className="text-sm opacity-90">
              {busy === "waiter" ? t.calls.sending : waiter ? t.calls.waiterSent : t.calls.waiterSub}
            </span>
          </span>
        </button>
        <Link
          href={`/m/${token}/conta`}
          className="flex h-32 flex-col items-start justify-between rounded-3xl border border-(--mesa-stroke) bg-(--mesa-glass) p-4 text-left focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
        >
          <span className="flex size-10 items-center justify-center rounded-xl border border-(--mesa-stroke)">
            <Receipt aria-hidden className="size-5" />
          </span>
          <span className="flex flex-col">
            <span className="font-semibold">{t.askBill}</span>
            <span className="text-sm text-(--mesa-muted)">{bill ? t.calls.billSent : t.calls.billSub}</span>
          </span>
        </Link>
      </section>

      {error && (
        <p role="alert" className="-mt-3 text-sm font-medium text-(--brand-text)">
          {error}
        </p>
      )}
    </>
  );
}
