"use client";

import { Flame } from "lucide-react";
import { useState, useSyncExternalStore } from "react";
import { tablePage } from "@/messages/table.pt-BR";
import { joinRodizio, type RodizioParticipation, type TableOrders } from "./table-orders";

const t = tablePage.rodizio.invite;
const focus = "focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none";

const noop = () => () => {};
const key = (token: string) => `ohchefe:rodizio-recusado:${token}`;

function declinedHere(token: string) {
  try {
    return sessionStorage.getItem(key(token)) === "1";
  } catch {
    return false;
  }
}

/**
 * O celular que chegou depois da ativação do rodízio (ele é da mesa, mas cobrado por pessoa):
 * "Você vai participar?". "Sim" chama o garçom, e as rodadas liberam quando ele inclui este
 * celular (ao ajustar as pessoas); "Não" deixa só o cardápio de sempre, com um atalho discreto
 * para mudar de ideia. Já dentro do rodízio, não aparece.
 */
export function RodizioInvite({
  token,
  rodizio,
  participation,
  onChange,
}: {
  token: string;
  rodizio: NonNullable<TableOrders["rodizio"]>;
  participation: RodizioParticipation | undefined;
  onChange: () => void;
}) {
  // No servidor não há sessionStorage: lá conta como "não respondeu", e o navegador acerta.
  const stored = useSyncExternalStore(
    noop,
    () => declinedHere(token),
    () => false,
  );
  const [declined, setDeclined] = useState(false);
  const [asked, setAsked] = useState<RodizioParticipation | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const state = asked ?? participation ?? "NONE";

  if (state === "JOINED") return null;

  async function join() {
    setBusy(true);
    setError(null);
    try {
      setAsked(await joinRodizio(token));
      onChange();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }

  function decline() {
    try {
      sessionStorage.setItem(key(token), "1");
    } catch {
      // Sem sessionStorage (aba privada): vale só nesta tela.
    }
    setDeclined(true);
  }

  if (state === "REQUESTED") {
    return (
      <p
        role="status"
        className="flex items-start gap-3 rounded-3xl border border-(--mesa-stroke) bg-(--mesa-info-soft) p-4 text-sm"
      >
        <Flame aria-hidden className="mt-0.5 size-4 shrink-0 text-(--mesa-info)" />
        {t.requested}
      </p>
    );
  }

  if (declined || stored) {
    return (
      <button
        type="button"
        disabled={busy}
        onClick={() => void join()}
        className={`flex min-h-12 items-center gap-2.5 rounded-2xl border border-(--mesa-stroke) bg-(--mesa-glass) px-4 text-left text-sm font-medium disabled:opacity-60 ${focus}`}
      >
        <Flame aria-hidden className="size-4 shrink-0 text-(--brand-text)" />
        {busy ? t.joining : t.changeMind(rodizio.name)}
      </button>
    );
  }

  return (
    <section
      aria-label={t.label}
      className="flex flex-col gap-3 rounded-3xl border border-(--brand-text)/40 bg-(--mesa-glass) p-4.5"
    >
      <span className="flex items-center gap-1.5 text-[11px] font-semibold tracking-[.3px] text-(--brand-text) uppercase">
        <Flame aria-hidden className="size-3.5" />
        {t.label}
      </span>
      <h2 className="text-lg leading-6 font-semibold">{t.title(rodizio.name, rodizio.people)}</h2>
      <p className="text-sm text-(--mesa-muted)">{t.question}</p>
      {error && (
        <p role="alert" className="text-sm font-medium text-(--mesa-danger)">
          {error}
        </p>
      )}
      <div className="flex flex-col gap-2">
        <button
          type="button"
          disabled={busy}
          onClick={() => void join()}
          className={`h-12 rounded-2xl bg-(--brand) font-semibold text-white disabled:opacity-70 ${focus}`}
        >
          {busy ? t.joining : t.yes}
        </button>
        <button
          type="button"
          disabled={busy}
          onClick={decline}
          className={`h-11 rounded-2xl text-sm font-semibold text-(--mesa-muted) ${focus}`}
        >
          {t.no}
        </button>
      </div>
    </section>
  );
}
