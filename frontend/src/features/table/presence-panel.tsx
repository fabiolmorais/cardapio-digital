"use client";

import { Ban, ChevronRight, LoaderCircle, Lock, MapPin, UserCheck, Wifi } from "lucide-react";
import { useEffect, useState, useSyncExternalStore, type ReactNode } from "react";
import { tablePage } from "@/messages/table.pt-BR";
import { cancelCall, requestCall, type useTableCalls } from "./table-calls";

const t = tablePage.presence;

type TableState = ReturnType<typeof useTableCalls>;

/**
 * A trava de presença na tela da mesa (desenhos Verificando presença, Presença não confirmada e
 * Aguardando liberação). Fica no lugar dos botões enquanto o celular não provou que está no
 * salão; o cardápio continua aberto. Devolve null quando a presença está confirmada.
 */
export function PresencePanel({ token, state }: { token: string; state: TableState }) {
  const { presence, present, unlock, busy, error, run, locate, recheck } = state;

  if (present) return null;

  if (!presence) {
    return (
      <p role="status" className="flex items-center gap-2.5 text-sm text-(--mesa-muted)">
        <LoaderCircle aria-hidden className="size-4 animate-spin" />
        {t.checking}
      </p>
    );
  }

  if (presence.blocked) {
    return (
      <section
        role="alert"
        className="flex flex-col gap-3 rounded-3xl border border-(--mesa-stroke) bg-(--mesa-glass) p-5"
      >
        <span className="flex size-12 items-center justify-center rounded-full bg-red-500/15 text-red-400">
          <Ban aria-hidden className="size-6" />
        </span>
        <h2 className="text-xl font-bold tracking-tight">{t.blockedTitle}</h2>
        <p className="text-sm text-(--mesa-muted)">{t.blockedText}</p>
      </section>
    );
  }

  if (unlock) {
    return (
      <section
        role="status"
        className="flex flex-col items-center gap-4 rounded-3xl border border-(--mesa-stroke) bg-(--mesa-glass) p-6 text-center"
      >
        <span className="relative flex size-20 items-center justify-center">
          <span aria-hidden className="absolute inset-0 animate-ping rounded-full bg-(--brand) opacity-20" />
          <span className="flex size-16 items-center justify-center rounded-full bg-(--brand) text-white">
            <UserCheck aria-hidden className="size-7" />
          </span>
        </span>
        <div className="flex flex-col gap-1.5">
          <h2 className="text-xl font-bold tracking-tight">{t.releaseTitle}</h2>
          <p className="text-sm text-(--mesa-muted)">{t.releaseText}</p>
        </div>
        <button
          type="button"
          disabled={busy !== null}
          onClick={() => run("cancel-release", () => cancelCall(token, unlock.id))}
          className="h-11 rounded-2xl border border-(--mesa-stroke) px-5 text-sm font-semibold focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none disabled:opacity-60"
        >
          {t.cancelRelease}
        </button>
      </section>
    );
  }

  return (
    <section
      aria-labelledby="presenca"
      className="flex flex-col gap-4 rounded-3xl border border-(--mesa-stroke) bg-(--mesa-glass) p-5"
    >
      <span className="flex size-12 items-center justify-center rounded-full bg-amber-400/15 text-amber-400">
        <Lock aria-hidden className="size-6" />
      </span>
      <div className="flex flex-col gap-1.5">
        <h2 id="presenca" className="text-xl font-bold tracking-tight">
          {t.title}
        </h2>
        <p className="text-sm text-(--mesa-muted)">{t.text}</p>
      </div>

      {(presence.locationEnabled || presence.wifiName) && (
        <div role="group" aria-label={t.options} className="flex flex-col gap-2.5">
          {presence.locationEnabled && (
            <Option
              icon={<MapPin aria-hidden className="size-5" />}
              title={busy === "locate" ? t.locating : t.locate}
              hint={t.locateHint}
              disabled={busy !== null}
              onClick={locate}
            />
          )}
          {presence.wifiName && (
            <Option
              icon={<Wifi aria-hidden className="size-5" />}
              title={busy === "wifi" ? t.rechecking : t.wifi(presence.wifiName)}
              hint={t.wifiHint}
              disabled={busy !== null}
              onClick={recheck}
            />
          )}
        </div>
      )}

      {error && (
        <p role="alert" className="text-sm font-medium text-(--brand-text)">
          {error}
        </p>
      )}

      <button
        type="button"
        disabled={busy !== null}
        onClick={() => run("release", () => requestCall(token, { type: "UNLOCK" }))}
        className="h-13 rounded-2xl bg-(--brand) font-semibold text-white focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:ring-offset-2 focus-visible:ring-offset-(--mesa-bg) focus-visible:outline-none disabled:opacity-80"
      >
        {busy === "release" ? t.sending : t.askRelease}
      </button>
    </section>
  );
}

function Option({
  icon,
  title,
  hint,
  disabled,
  onClick,
}: {
  icon: ReactNode;
  title: string;
  hint: string;
  disabled: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      className="flex items-center gap-3.5 rounded-2xl border border-(--mesa-stroke) p-3.5 text-left focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none disabled:opacity-60"
    >
      <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-(--brand) text-white">
        {icon}
      </span>
      <span className="flex flex-1 flex-col gap-0.5">
        <span className="font-semibold">{title}</span>
        <span className="text-sm text-(--mesa-muted)">{hint}</span>
      </span>
      <ChevronRight aria-hidden className="size-5 text-(--mesa-muted)" />
    </button>
  );
}

const DISMISSED = (token: string) => `ohchefe:so-cardapio:${token}`;
const noop = () => () => {};

/**
 * A chegada à mesa em tela cheia (desenhos 1.1, 1.2 e 1.3): enquanto confere a presença, "Bem-vindo";
 * se não der, as formas de confirmar e o "Ver só o cardápio"; com a liberação pedida, a espera.
 * Confirmada a presença, some sozinha. Depois do "Ver só o cardápio", a página mostra a trava no
 * meio (PresencePanel), como antes, até o celular fechar a aba.
 */
export function ArrivalGate({
  token,
  state,
  name,
  where,
  logo,
  fallback,
}: {
  token: string;
  state: TableState;
  name: string;
  where: string;
  logo: ReactNode;
  /** A trava no meio da página, para depois do "Ver só o cardápio" (ou com o celular bloqueado). */
  fallback: ReactNode;
}) {
  const { presence, present, unlock, busy, error, run, locate, recheck } = state;
  const stored = useSyncExternalStore(
    noop,
    () => {
      try {
        return sessionStorage.getItem(DISMISSED(token)) === "1";
      } catch {
        return false;
      }
    },
    () => false,
  );
  const [dismissed, setDismissed] = useState(false);
  const elapsed = useElapsed(unlock?.createdAt ?? null);

  if (present) return null;
  if (stored || dismissed || presence?.blocked) return fallback;

  function onlyMenu() {
    try {
      sessionStorage.setItem(DISMISSED(token), "1");
    } catch {
      // Sem armazenamento (aba anônima restrita): vale só até recarregar.
    }
    setDismissed(true);
  }

  const screen =
    "fixed inset-0 z-50 flex flex-col overflow-y-auto bg-(--mesa-bg) px-5 pt-8 pb-[max(env(safe-area-inset-bottom),24px)] text-(--mesa-fg)";

  // 1.1: conferindo.
  if (!presence) {
    return (
      <div role="dialog" aria-modal="true" aria-labelledby="chegada" className={screen}>
        <div className="mx-auto flex w-full max-w-md flex-1 flex-col items-center justify-center gap-6 text-center">
          <span className="size-20 overflow-hidden rounded-full text-2xl">{logo}</span>
          <div className="flex flex-col gap-1.5">
            <span className="text-[13px] text-(--mesa-muted)">{where}</span>
            <h1 id="chegada" className="text-[26px] leading-[31px] font-bold tracking-tight">
              {t.welcome(name)}
            </h1>
            <p className="text-[15px] text-(--mesa-muted)">{t.welcomeText}</p>
          </div>
          <ul className="flex w-full flex-col gap-2 rounded-3xl border border-(--mesa-stroke) bg-(--mesa-glass) p-3 text-left">
            <Step icon={<Wifi aria-hidden className="size-5" />} title={t.stepWifi} hint={t.stepWifiHint} busy />
            <Step icon={<MapPin aria-hidden className="size-5" />} title={t.stepLocation} hint={t.stepLocationHint} />
          </ul>
          <p role="status" className="text-[13px] text-(--mesa-muted)">
            {t.fewSeconds}
          </p>
        </div>
      </div>
    );
  }

  // 1.3: a liberação foi pedida; o garçom confirma.
  if (unlock) {
    return (
      <div role="dialog" aria-modal="true" aria-labelledby="chegada" className={screen}>
        <div
          role="status"
          className="mx-auto flex w-full max-w-md flex-1 flex-col items-center justify-center gap-5 text-center"
        >
          <span className="relative flex size-28 items-center justify-center">
            <span aria-hidden className="absolute inset-0 animate-ping rounded-full bg-(--brand) opacity-20" />
            <span className="flex size-20 items-center justify-center rounded-full bg-(--brand) text-white">
              <UserCheck aria-hidden className="size-8" />
            </span>
          </span>
          <h1 id="chegada" className="text-[26px] leading-[31px] font-bold tracking-tight">
            {t.releaseTitle}
          </h1>
          <p className="text-[15px] text-(--mesa-muted)">{t.releaseText}</p>
          <span
            role="timer"
            className="rounded-full border border-(--mesa-stroke) px-3.5 py-1.5 text-[13px] text-(--mesa-muted)"
          >
            {t.waitingFor(elapsed)}
          </span>
          <button
            type="button"
            disabled={busy !== null}
            onClick={() => run("cancel-release", () => cancelCall(token, unlock.id))}
            className="h-11 rounded-2xl border border-(--mesa-stroke) bg-(--mesa-glass) px-5 text-sm font-semibold focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none disabled:opacity-60"
          >
            {t.cancelRelease}
          </button>
        </div>
      </div>
    );
  }

  // 1.2: não confirmou. As formas de confirmar, o garçom e o cardápio.
  return (
    <div role="dialog" aria-modal="true" aria-labelledby="chegada" className={screen}>
      <div className="mx-auto flex w-full max-w-md flex-1 flex-col gap-6">
        <header className="flex items-center gap-3">
          <span className="size-10 overflow-hidden rounded-full text-[13px]">{logo}</span>
          <span className="flex min-w-0 flex-col">
            <span className="truncate font-semibold">{name}</span>
            <span className="text-[13px] text-(--mesa-muted)">{where}</span>
          </span>
        </header>
        <div className="flex flex-1 flex-col justify-end gap-6">
          <span className="flex size-16 items-center justify-center rounded-full bg-amber-400/15 text-amber-400">
            <Lock aria-hidden className="size-7" />
          </span>
          <div className="flex flex-col gap-2.5">
            <h1 id="chegada" className="text-[26px] leading-[31px] font-bold tracking-tight">
              {t.title}
            </h1>
            <p className="text-[15px] text-(--mesa-muted)">{t.text}</p>
          </div>
          {(presence.locationEnabled || presence.wifiName) && (
            <div role="group" aria-label={t.options} className="flex flex-col gap-2.5">
              {presence.locationEnabled && (
                <Option
                  icon={<MapPin aria-hidden className="size-5" />}
                  title={busy === "locate" ? t.locating : t.locate}
                  hint={t.locateHint}
                  disabled={busy !== null}
                  onClick={locate}
                />
              )}
              {presence.wifiName && (
                <Option
                  icon={<Wifi aria-hidden className="size-5" />}
                  title={busy === "wifi" ? t.rechecking : t.wifi(presence.wifiName)}
                  hint={t.wifiHint}
                  disabled={busy !== null}
                  onClick={recheck}
                />
              )}
            </div>
          )}
          {error && (
            <p role="alert" className="text-sm font-medium text-(--brand-text)">
              {error}
            </p>
          )}
          <div className="flex flex-col gap-2">
            <button
              type="button"
              disabled={busy !== null}
              onClick={() => run("release", () => requestCall(token, { type: "UNLOCK" }))}
              className="h-13 rounded-2xl bg-(--brand) font-semibold text-white focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:ring-offset-2 focus-visible:ring-offset-(--mesa-bg) focus-visible:outline-none disabled:opacity-80"
            >
              {busy === "release" ? t.sending : t.askRelease}
            </button>
            <button
              type="button"
              onClick={onlyMenu}
              className="h-13 rounded-2xl font-semibold focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
            >
              {t.onlyMenu}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function Step({ icon, title, hint, busy = false }: { icon: ReactNode; title: string; hint: string; busy?: boolean }) {
  return (
    <li className="flex items-center gap-3 rounded-2xl p-2">
      <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-(--brand)/15 text-(--brand-text)">
        {icon}
      </span>
      <span className="flex flex-1 flex-col">
        <span className="font-semibold">{title}</span>
        <span className="text-[13px] text-(--mesa-muted)">{hint}</span>
      </span>
      {busy ? (
        <LoaderCircle aria-hidden className="size-5 animate-spin text-(--brand-text)" />
      ) : (
        <span className="rounded-full border border-(--mesa-stroke) px-2 py-0.5 text-[11px] text-(--mesa-muted)">
          {t.stepWaiting}
        </span>
      )}
    </li>
  );
}

/** "0:42": há quanto tempo o pedido de liberação espera, contando a cada segundo. */
function useElapsed(since: string | null) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    if (!since) return;
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [since]);
  if (!since) return "0:00";
  const seconds = Math.max(0, Math.floor((now - new Date(since).getTime()) / 1000));
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`;
}
