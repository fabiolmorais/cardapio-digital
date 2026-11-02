"use client";

import { Bell, Volume2, X } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { DeviceNotLinkedError } from "@/features/devices/device-api";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { enableSound, playChime } from "./alert-sound";
import { elapsed, useNow } from "./call-queue";
import type { StaffCall } from "./calls-api";
import { staffSource, tvDeviceSource, useLiveCalls, useSoundOn, type LiveCallsSource } from "./use-live-calls";

const t = messages.tv;
const calls = messages.panel.calls;
/** Quanto tempo o alerta grande de chamado novo fica na tela. */
const ALERT_MS = 8000;
/** "Alerta sonoro contínuo": enquanto houver chamado esperando, o som repete neste intervalo. */
const REPEAT_MS = 20_000;
const WARN_AFTER_S = 120;
const LATE_AFTER_S = 240;

/**
 * Modo TV (desenhos 8.1 e 8.2): tela cheia, só de leitura, com letras grandes para ver de longe.
 * Começa com um toque ("Ativar som e começar"): é ele que libera o som e a tela cheia, que o
 * navegador só permite depois de uma ação de alguém.
 */
export function TvMode() {
  const { status, session } = useRequiredSession();
  const [started, setStarted] = useState(false);

  if (status !== "authenticated") {
    return <p className="p-10 text-2xl text-muted-foreground">{t.loading}</p>;
  }
  if (!started) return <TvStart onStart={() => setStarted(true)} />;
  return <TvBoard establishmentName={session.establishment.name} source={staffSource} exitHref="/painel" />;
}

/**
 * O Modo TV na TV vinculada (/aparelho), sem login: a fila e os avisos vêm pelo cookie do
 * aparelho. Não tem "sair": a TV só serve para isso. Se for desvinculada, onDeviceLost.
 */
export function DeviceTvMode({ establishmentName, onDeviceLost }: { establishmentName: string; onDeviceLost: () => void }) {
  const [started, setStarted] = useState(false);
  if (!started) return <TvStart onStart={() => setStarted(true)} />;
  return <TvBoard establishmentName={establishmentName} source={tvDeviceSource} onDeviceLost={onDeviceLost} />;
}

/** O toque inicial: é ele que libera o som e a tela cheia (desenho 7.4). */
function TvStart({ onStart }: { onStart: () => void }) {
  return (
    <main className="flex min-h-dvh flex-col items-center justify-center gap-8 p-10 text-center">
      <span className="flex size-28 items-center justify-center rounded-full bg-primary text-primary-foreground">
        <Volume2 aria-hidden className="size-12" />
      </span>
      <div className="flex max-w-xl flex-col gap-3">
        <h1 className="text-4xl font-bold tracking-tight">{t.startTitle}</h1>
        <p className="text-xl text-muted-foreground">{t.startText}</p>
      </div>
      <button
        type="button"
        onClick={async () => {
          await enableSound();
          await document.documentElement.requestFullscreen?.().catch(() => undefined);
          onStart();
        }}
        className="h-16 rounded-2xl bg-primary px-10 text-xl font-semibold text-primary-foreground focus-visible:ring-4 focus-visible:ring-ring focus-visible:outline-none"
      >
        {t.start}
      </button>
      <button
        type="button"
        onClick={onStart}
        className="text-lg text-muted-foreground underline-offset-4 hover:underline"
      >
        {t.startMuted}
      </button>
    </main>
  );
}

function TvBoard({
  establishmentName,
  source,
  exitHref,
  onDeviceLost,
}: {
  establishmentName: string;
  source: LiveCallsSource;
  exitHref?: string;
  onDeviceLost?: () => void;
}) {
  const [alert, setAlert] = useState<StaffCall | null>(null);
  const soundOn = useSoundOn();
  const now = useNow();
  const onNewCall = useCallback((call: StaffCall) => {
    playChime();
    setAlert(call);
  }, []);
  const { queue, connected } = useLiveCalls(onNewCall, source);
  const list = queue.data ?? [];
  const waiting = list.some((call) => call.status === "PENDING");

  useEffect(() => {
    if (queue.error instanceof DeviceNotLinkedError) onDeviceLost?.();
  }, [queue.error, onDeviceLost]);

  useEffect(() => {
    if (!alert) return;
    const timer = window.setTimeout(() => setAlert(null), ALERT_MS);
    return () => window.clearTimeout(timer);
  }, [alert]);

  useEffect(() => {
    if (!waiting) return;
    const timer = window.setInterval(playChime, REPEAT_MS);
    return () => window.clearInterval(timer);
  }, [waiting]);

  const clock = new Date(now).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });

  return (
    <main className="relative flex min-h-dvh flex-col gap-8 p-10">
      <header className="flex items-center gap-6">
        <div className="mr-auto flex flex-col gap-1">
          <h1 className="text-5xl font-bold tracking-tight">{establishmentName}</h1>
          <span role="status" className="flex items-center gap-3 text-2xl text-muted-foreground">
            <span
              aria-hidden
              className={cn("size-3.5 rounded-full", connected ? "animate-pulse bg-success" : "bg-muted-foreground")}
            />
            {connected ? t.live : calls.reconnecting}
            {!soundOn && ` · ${t.muted}`}
          </span>
        </div>
        <span className="font-mono text-7xl font-bold tabular-nums">{clock}</span>
        {exitHref && (
          <Link
            href={exitHref}
            aria-label={t.exit}
            className="flex size-14 items-center justify-center rounded-full text-muted-foreground hover:bg-muted focus-visible:ring-4 focus-visible:ring-ring focus-visible:outline-none"
          >
            <X aria-hidden className="size-7" />
          </Link>
        )}
      </header>

      <section aria-labelledby="tv-chamados" className="flex flex-col gap-5">
        <div className="flex items-center gap-4">
          <h2 id="tv-chamados" className="text-4xl font-bold">
            {calls.title}
          </h2>
          <span className="flex h-12 min-w-12 items-center justify-center rounded-full bg-primary px-4 text-2xl font-bold text-primary-foreground">
            {list.length}
          </span>
        </div>
        {list.length === 0 ? (
          <p className="text-2xl text-muted-foreground">{t.empty}</p>
        ) : (
          <ul className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            {list.map((call) => {
              const waited = (now - Date.parse(call.createdAt)) / 1000;
              const pending = call.status === "PENDING";
              const level = !pending ? "taken" : waited >= LATE_AFTER_S ? "late" : waited >= WARN_AFTER_S ? "warn" : "ok";
              return (
                <li
                  key={call.id}
                  className={cn(
                    "flex items-center gap-6 rounded-3xl border-2 border-border bg-card p-6",
                    level === "late" && "border-destructive",
                    !pending && "opacity-75",
                  )}
                >
                  <span className="flex size-24 shrink-0 flex-col items-center justify-center rounded-2xl bg-muted leading-none">
                    <span className="text-base font-semibold tracking-widest text-muted-foreground">{calls.table.toUpperCase()}</span>
                    <span className="text-5xl font-bold">{call.tableLabel}</span>
                  </span>
                  <span className="flex min-w-0 flex-1 flex-col gap-1.5">
                    <span className="text-3xl font-semibold">{calls.types[call.type]}</span>
                    <span className="truncate text-2xl text-muted-foreground">
                      {pending
                        ? [call.type === "UNLOCK" ? calls.notPresent : call.paymentMethod && calls.payment[call.paymentMethod], call.sectorName].filter(Boolean).join(" · ")
                        : t.taken(call.acceptedByName)}
                    </span>
                  </span>
                  <span
                    className={cn(
                      "rounded-2xl px-5 py-3 font-mono text-4xl font-bold tabular-nums",
                      level === "ok" && "text-success",
                      level === "warn" && "text-amber-400",
                      level === "late" && "text-destructive",
                      level === "taken" && "text-muted-foreground",
                    )}
                  >
                    {elapsed(waited)}
                  </span>
                </li>
              );
            })}
          </ul>
        )}
      </section>

      {alert && (
        <div role="alert" className="absolute inset-0 flex items-center justify-center bg-background/80 p-10 backdrop-blur">
          <section className="flex w-full max-w-5xl items-center gap-14 rounded-[48px] bg-primary p-16 text-primary-foreground shadow-2xl">
            <span className="flex size-64 shrink-0 flex-col items-center justify-center rounded-[64px] bg-black/20 leading-none">
              <span className="text-2xl font-semibold tracking-widest opacity-85">{calls.table.toUpperCase()}</span>
              <span className="text-[140px] font-bold">{alert.tableLabel}</span>
            </span>
            <span className="flex flex-col gap-5">
              <span className="flex items-center gap-3 text-2xl font-semibold opacity-90">
                <Bell aria-hidden className="size-8" />
                {t.newCall}
              </span>
              <span className="text-7xl font-bold tracking-tight">{calls.types[alert.type]}</span>
              <span className="text-3xl opacity-90">
                {[alert.type === "UNLOCK" ? calls.notPresent : alert.paymentMethod && calls.payment[alert.paymentMethod], alert.sectorName].filter(Boolean).join(" · ")}
              </span>
            </span>
          </section>
        </div>
      )}
    </main>
  );
}
