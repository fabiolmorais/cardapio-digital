"use client";

import { Bell, Receipt, UserCheck } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";
import { tablePage } from "@/messages/table.pt-BR";
import { PresencePanel } from "./presence-panel";
import { requestCall, cancelCall, useTableCalls } from "./table-calls";

const t = tablePage;

/**
 * Os dois botões grandes da mesa (desenho Main) e o aviso do chamado aberto: "Chamado enviado"
 * enquanto ninguém aceitou, "Garçom a caminho" depois. Tocar de novo não duplica: o back devolve
 * o mesmo chamado. "Pedir a conta" abre a tela da forma de pagamento. Sem a presença no salão
 * confirmada, mostra a trava (PresencePanel) no lugar dos botões.
 */
export function TableActions({ token, header, intro }: { token: string; header: ReactNode; intro: ReactNode }) {
  const state = useTableCalls(token);
  const { waiter, bill, busy, error, run } = state;
  const coming = waiter?.status === "IN_PROGRESS";

  // A trava de presença: enquanto o celular não provou que está no salão, os botões dão lugar a ela.
  if (!state.present)
    return (
      <>
        {header}
        {intro}
        <PresencePanel token={token} state={state} />
      </>
    );

  return (
    <>
      {/* O topo da página (servidor) com o selo "No salão": a presença já foi confirmada. */}
      <div className="flex items-center gap-3">
        <div className="min-w-0 flex-1">{header}</div>
        <span className="flex h-6 shrink-0 items-center gap-1.5 rounded-full bg-(--mesa-ok-soft) px-2.5 text-xs font-semibold text-(--mesa-ok)">
          <span aria-hidden className="size-1.5 rounded-full bg-current" />
          {t.present}
        </span>
      </div>

      {/* Com um chamado aberto, o aviso dele ocupa o lugar do "Peça sem esperar" (desenho 2.1). */}
      {!waiter && intro}
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
          className="flex h-32 flex-col items-start justify-between rounded-3xl bg-(--brand) p-4 text-left text-white shadow-[0_14px_30px_rgba(0,0,0,.35)] focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:ring-offset-2 focus-visible:ring-offset-(--mesa-bg) focus-visible:outline-none disabled:opacity-80"
        >
          <span className="flex size-[42px] items-center justify-center rounded-[14px] bg-white/20">
            <Bell aria-hidden className="size-[22px]" />
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
          <span className="flex size-[42px] items-center justify-center rounded-[14px] border border-(--mesa-stroke) bg-(--mesa-glass)">
            <Receipt aria-hidden className="size-[22px]" />
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
