"use client";

import { Ban, ChevronRight, LoaderCircle, Lock, MapPin, UserCheck, Wifi } from "lucide-react";
import type { ReactNode } from "react";
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
      <section role="alert" className="flex flex-col gap-3 rounded-3xl border border-(--mesa-stroke) bg-(--mesa-glass) p-5">
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
      <section role="status" className="flex flex-col items-center gap-4 rounded-3xl border border-(--mesa-stroke) bg-(--mesa-glass) p-6 text-center">
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
    <section aria-labelledby="presenca" className="flex flex-col gap-4 rounded-3xl border border-(--mesa-stroke) bg-(--mesa-glass) p-5">
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
      <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-(--brand) text-white">{icon}</span>
      <span className="flex flex-1 flex-col gap-0.5">
        <span className="font-semibold">{title}</span>
        <span className="text-sm text-(--mesa-muted)">{hint}</span>
      </span>
      <ChevronRight aria-hidden className="size-5 text-(--mesa-muted)" />
    </button>
  );
}
