"use client";

import { Check, ChevronLeft, Flame, UserCheck } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { formatCents } from "@/lib/format";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";
import { PresencePanel } from "./presence-panel";
import { cancelCall, requestCall, useTableCalls } from "./table-calls";
import { listRodizios, useTableOrders, type RodizioQuote } from "./table-orders";
import { TabClosed } from "./tab-closed";

const t = tablePage.rodizio;
const focus = "focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none";

/**
 * "Escolha o rodízio": os rodízios com o preço por pessoa de agora, "Como funciona" e o "Chamar
 * garçom para ativar" (um chamado RODIZIO). Quem escolhe e confirma as pessoas é o garçom, ao
 * ativar; quando ele ativa, a tela mostra "Rodízio ativado!" e leva ao cardápio.
 */
export function RodizioChooser({ token, tableLabel }: { token: string; tableLabel: string }) {
  const calls = useTableCalls(token);
  const orders = useTableOrders(token);
  const [options, setOptions] = useState<RodizioQuote[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const request = calls.rodizio;
  const active = orders.data?.rodizio;

  useEffect(() => {
    let cancelled = false;
    listRodizios(token)
      .then((list) => !cancelled && setOptions(list))
      .catch((e: Error) => !cancelled && setLoadError(e.message));
    return () => {
      cancelled = true;
    };
  }, [token]);

  const header = (
    <header className="flex items-center gap-3">
      <Link
        href={`/m/${token}`}
        aria-label={t.back}
        className={cn("flex size-11 items-center justify-center rounded-full border border-(--mesa-stroke) bg-(--mesa-glass)", focus)}
      >
        <ChevronLeft aria-hidden className="size-5" />
      </Link>
      <div className="flex flex-col">
        <h1 className="text-2xl font-bold tracking-tight">{t.chooseTitle}</h1>
        <span className="text-sm text-(--mesa-muted)">{t.chooseSub(tableLabel)}</span>
      </div>
    </header>
  );

  if (calls.closed || orders.closed) {
    return (
      <>
        {header}
        <TabClosed />
      </>
    );
  }

  if (active) {
    return (
      <>
        {header}
        <section role="status" className="flex flex-col items-center gap-4 pt-6 text-center">
          <span className="flex size-20 items-center justify-center rounded-full bg-(--brand) text-white">
            <Check aria-hidden className="size-9" />
          </span>
          <h2 className="text-2xl font-bold tracking-tight">{t.activeTitle}</h2>
          <p className="text-(--mesa-muted)">{t.banner(active.name, active.people)}</p>
          <Link
            href={`/m/${token}/cardapio#rodizio`}
            className={cn("flex h-14 w-full items-center justify-center rounded-2xl bg-(--brand) font-semibold text-white", focus)}
          >
            {t.orderRound}
          </Link>
        </section>
      </>
    );
  }

  return (
    <>
      {header}
      {loadError && (
        <p role="alert" className="text-sm font-medium text-(--brand-text)">
          {loadError}
        </p>
      )}
      {options === null ? (
        !loadError && <p className="text-(--mesa-muted)">{t.loading}</p>
      ) : options.length === 0 ? (
        <p className="text-(--mesa-muted)">{t.none}</p>
      ) : (
        <>
          <ul className="flex flex-col gap-3">
            {options.map((r) => (
              <li
                key={r.id}
                className="flex flex-col gap-2 rounded-[26px] border border-(--mesa-stroke) bg-(--mesa-glass) p-4.5"
              >
                <span className="flex items-center gap-2.5">
                  <span className="flex size-10 items-center justify-center rounded-xl bg-(--brand) text-white">
                    <Flame aria-hidden className="size-5" />
                  </span>
                  <h2 className="text-xl font-bold tracking-tight">{r.name}</h2>
                </span>
                {r.description && <p className="text-sm text-(--mesa-muted)">{r.description}</p>}
                <ul className="flex flex-col gap-0.5">
                  {r.tiers.map((tier, i) => (
                    <li key={tier.name} className={cn(i === 0 ? "text-lg font-bold" : "text-sm text-(--mesa-muted)")}>
                      {t.tierPrice(tier.name, formatCents(tier.priceCents))}
                    </li>
                  ))}
                </ul>
              </li>
            ))}
          </ul>

          <section className="flex flex-col gap-3 rounded-[22px] border border-(--mesa-stroke) bg-(--mesa-glass) p-4">
            <h2 className="font-semibold">{t.howTitle}</h2>
            <ol className="flex flex-col gap-2.5">
              {t.how.map((line, i) => (
                <li key={line} className="flex items-start gap-3 text-sm text-(--mesa-muted)">
                  <span className="flex size-6 shrink-0 items-center justify-center rounded-full border border-(--brand-text) text-xs font-bold text-(--brand-text)">
                    {i + 1}
                  </span>
                  {line}
                </li>
              ))}
            </ol>
          </section>

          {!calls.present ? (
            calls.presence && <PresencePanel token={token} state={calls} />
          ) : request ? (
            <section role="status" className="flex flex-col gap-3.5 rounded-3xl border border-(--mesa-stroke) bg-(--mesa-glass) p-4">
              <span className="flex items-center gap-3.5">
                <span className="flex size-12 shrink-0 items-center justify-center rounded-full bg-(--brand) text-white">
                  <UserCheck aria-hidden className="size-5" />
                </span>
                <span className="flex flex-col gap-0.5">
                  <span className="font-semibold">
                    {request.status === "IN_PROGRESS" ? t.comingTitle : t.requestedTitle}
                  </span>
                  <span className="text-sm text-(--mesa-muted)">{t.requestedText}</span>
                </span>
              </span>
              <button
                type="button"
                disabled={calls.busy !== null}
                onClick={() => calls.run("cancel", () => cancelCall(token, request.id))}
                className={cn("h-11 rounded-2xl border border-(--mesa-stroke) text-sm font-semibold disabled:opacity-60", focus)}
              >
                {calls.busy === "cancel" ? t.canceling : t.cancel}
              </button>
            </section>
          ) : (
            <div className="flex flex-col gap-2">
              <button
                type="button"
                disabled={calls.busy !== null}
                onClick={() => calls.run("rodizio", () => requestCall(token, { type: "RODIZIO" }))}
                className={cn("h-14 rounded-2xl bg-(--brand) font-semibold text-white disabled:opacity-80", focus)}
              >
                {calls.busy === "rodizio" ? t.sending : t.callToActivate}
              </button>
              <span className="text-center text-sm text-(--mesa-muted)">{t.staffConfirms}</span>
            </div>
          )}
          {calls.error && (
            <p role="alert" className="text-sm font-medium text-(--brand-text)">
              {calls.error}
            </p>
          )}
        </>
      )}
    </>
  );
}
