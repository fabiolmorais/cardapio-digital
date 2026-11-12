"use client";

import { useQuery } from "@tanstack/react-query";
import { Bell, Volume2, X } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { DeviceNotLinkedError } from "@/features/devices/device-api";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { enableSound, playChime } from "./alert-sound";
import { SoundToggle } from "./sound-toggle";
import { useNow } from "./call-queue";
import { fetchLiveFloor, fetchTvBoard, sectorsOf } from "@/features/live/live-api";
import { Badge, ITEM_TONE, TONE, TableBox, Timer, elapsed, waitLevel, type Tone } from "@/features/live/live-ui";
import { fetchKitchenQueue, type KitchenItem } from "@/features/orders/orders-api";
import { initials } from "@/lib/initials";
import type { StaffCall } from "./calls-api";
import { staffSource, tvDeviceSource, useLiveCalls, useSoundOn, type LiveCallsSource } from "./use-live-calls";
import { useWakeLock } from "@/lib/pwa/use-wake-lock";

const t = messages.tv;
const calls = messages.panel.calls;
const orders = messages.panel.live;
const status = messages.tablePage.orders.status;
/** Quanto tempo o alerta grande de chamado novo fica na tela. */
const ALERT_MS = 8000;
/** "Alerta sonoro contínuo": enquanto houver chamado esperando, o som repete neste intervalo. */
const REPEAT_MS = 20_000;

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
      <span className="relative flex size-28 items-center justify-center">
        <span aria-hidden className="absolute inset-0 animate-ping rounded-full border-2 border-brand-text/55" />
        <span className="flex size-28 items-center justify-center rounded-full bg-primary text-primary-foreground">
          <Volume2 aria-hidden className="size-12" />
        </span>
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

/** O que o Modo TV mostra além dos chamados: a cozinha, os setores e a espera média de hoje. */
type Board = { kitchen: KitchenItem[]; sectors: string[]; averageWaitSeconds?: number | null };

async function staffBoard(): Promise<Board> {
  const [kitchen, floor] = await Promise.all([fetchKitchenQueue(), fetchLiveFloor()]);
  return { kitchen, sectors: sectorsOf(floor.tables), averageWaitSeconds: floor.averageWaitSeconds };
}

/** O detalhe curto do chamado na TV: o setor e, na conta, a forma; na liberação, o porquê. */
function tvDetail(call: StaffCall) {
  const extra =
    call.type === "UNLOCK" ? calls.notPresent : call.paymentMethod ? calls.payment[call.paymentMethod] : null;
  return [call.sectorName, extra].filter(Boolean).join(" · ");
}

/** "Salão principal e Deck"; "Salão, Deck e Varanda". */
function joinSectors(names: string[]) {
  if (names.length <= 1) return names.join("");
  return `${names.slice(0, -1).join(", ")} e ${names[names.length - 1]}`;
}

/** 130 s → "2:10". */
function minutes(seconds: number) {
  return `${Math.floor(seconds / 60)}:${String(Math.round(seconds) % 60).padStart(2, "0")}`;
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
  useWakeLock();
  const now = useNow();
  const onNewCall = useCallback((call: StaffCall) => {
    playChime();
    setAlert(call);
  }, []);
  const { queue, connected } = useLiveCalls(onNewCall, source);
  const board = useQuery({
    queryKey: [source.queryKey, "board"],
    queryFn: source === tvDeviceSource ? fetchTvBoard : staffBoard,
    refetchInterval: connected ? 60_000 : 15_000,
    retry: false,
  });
  const list = queue.data ?? [];
  const waiting = list.some((call) => call.status === "PENDING");
  const kitchen = (board.data?.kitchen ?? []).filter((item) => item.status !== "READY");
  const ready = (board.data?.kitchen ?? []).filter((item) => item.status === "READY");
  const sectors = board.data?.sectors ?? [];
  const wait = board.data?.averageWaitSeconds;

  useEffect(() => {
    if (queue.error instanceof DeviceNotLinkedError || board.error instanceof DeviceNotLinkedError) onDeviceLost?.();
  }, [queue.error, board.error, onDeviceLost]);

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
    <main className="relative flex min-h-dvh flex-col gap-9 bg-background px-6 py-8 lg:h-dvh lg:overflow-hidden lg:px-14 lg:py-12">
      <header className="flex flex-wrap items-center gap-6">
        <span
          aria-hidden
          className="flex size-18 shrink-0 items-center justify-center rounded-[20px] bg-primary text-[26px] font-bold text-primary-foreground"
        >
          {initials(establishmentName)}
        </span>
        <div className="mr-auto flex min-w-60 flex-1 flex-col gap-1">
          <h1 className="truncate text-[40px] leading-[44px] font-bold">{establishmentName}</h1>
          <span role="status" className="flex items-center gap-3 text-[22px] leading-7 text-muted-foreground">
            <span
              aria-hidden
              className={cn("size-3.5 shrink-0 rounded-full", connected ? "animate-pulse bg-success" : "bg-muted-foreground")}
            />
            {connected ? t.live : calls.reconnecting}
            {connected && sectors.length > 0 && ` · ${joinSectors(sectors)}`}
            {!soundOn && ` · ${t.muted}`}
          </span>
        </div>
        <span className="flex flex-col items-end gap-1">
          <span className="font-mono text-[64px] leading-[64px] font-bold tabular-nums">{clock}</span>
          <span className="text-[22px] leading-7 text-muted-foreground">
            {wait == null ? t.noWait : t.averageWait(minutes(wait))}
          </span>
        </span>
        <div className="flex flex-col gap-2">
          <SoundToggle className="h-12 px-4 text-base" />
          {exitHref && (
            <Link
              href={exitHref}
              aria-label={t.exit}
              className="flex h-12 items-center justify-center rounded-xl text-muted-foreground hover:bg-muted focus-visible:ring-4 focus-visible:ring-ring focus-visible:outline-none"
            >
              <X aria-hidden className="size-7" />
            </Link>
          )}
        </div>
      </header>

      <div className="grid min-h-0 flex-1 grid-cols-1 gap-7 lg:grid-cols-3">
        <Column id="tv-chamados" title={calls.title} count={list.length} countClass="bg-primary text-primary-foreground">
          {list.length === 0 ? (
            <p className="text-2xl text-muted-foreground">{t.empty}</p>
          ) : (
            list.map((call) => {
              const waited = (now - Date.parse(call.createdAt)) / 1000;
              const pending = call.status === "PENDING";
              const level = waitLevel(waited, !pending);
              const late = level === "late";
              return (
                <TvItem
                  key={call.id}
                  label={call.tableLabel}
                  tone={call.type === "BILL" ? "info" : undefined}
                  className={cn(
                    late &&
                      "border-destructive/50 bg-[linear-gradient(180deg,rgb(248_113_113/14%),rgb(248_113_113/2%))]",
                    !pending && "opacity-75",
                  )}
                  title={calls.types[call.type]}
                  detail={pending ? tvDetail(call) : `${call.sectorName} · ${t.taken(call.acceptedByName)}`}
                  detailClass={pending ? undefined : "text-warning"}
                  end={
                    <Timer level={level} size="tv">
                      {elapsed(waited)}
                    </Timer>
                  }
                />
              );
            })
          )}
        </Column>

        <Column id="tv-cozinha" title={t.kitchen} count={kitchen.reduce((sum, i) => sum + i.quantity, 0)}>
          {kitchen.length === 0 ? (
            <p className="text-2xl text-muted-foreground">{t.kitchenEmpty}</p>
          ) : (
            kitchen.map((item) => (
              <TvItem
                key={item.id}
                label={item.tableLabel}
                title={`${item.quantity}× ${item.productName}`}
                detail={[item.sectorName, item.note ?? orders.order(item.orderNumber)].join(" · ")}
                end={
                  <Badge tone={ITEM_TONE[item.status]} className="h-10 px-4 text-xl">
                    {status[item.status]}
                  </Badge>
                }
              />
            ))
          )}
        </Column>

        <Column id="tv-prontos" title={t.ready} count={ready.length} countClass={TONE.info}>
          {ready.length === 0 ? (
            <p className="text-2xl text-muted-foreground">{t.readyEmpty}</p>
          ) : (
            ready.map((item) => (
              <TvItem
                key={item.id}
                label={item.tableLabel}
                tone="info"
                className="border-info/45"
                title={`${item.quantity}× ${item.productName}`}
                detail={`${item.sectorName} · ${t.readySince(
                  Math.max(0, Math.floor((now - Date.parse(item.readyAt ?? item.createdAt)) / 60_000)),
                )}`}
              />
            ))
          )}
        </Column>
      </div>

      {alert && (
        <div
          role="alert"
          className="absolute inset-0 flex items-center justify-center bg-background/70 p-10 backdrop-blur-[10px]"
        >
          <section className="flex w-full max-w-[1180px] items-center gap-18 rounded-[56px] bg-[#b8361a] px-20 py-18 text-white shadow-[0_40px_120px_rgb(204_63_28/45%)]">
            <span className="relative size-[300px] shrink-0">
              <span aria-hidden className="absolute inset-0 animate-ping rounded-[80px] border-4 border-white/55" />
              <span className="relative flex size-full flex-col items-center justify-center rounded-[80px] bg-black/20">
                <span className="text-[30px] font-semibold tracking-[3px] opacity-85">{calls.table.toUpperCase()}</span>
                <span className="text-[170px] leading-[160px] font-bold">{alert.tableLabel}</span>
              </span>
            </span>
            <span className="flex flex-col gap-5.5">
              <span className="flex items-center gap-3.5 text-[30px] font-semibold opacity-90">
                <Bell aria-hidden className="size-10" />
                {t.newCall}
              </span>
              <span className="text-[96px] leading-[100px] font-bold tracking-[-2px]">{calls.types[alert.type]}</span>
              <span className="text-[32px] leading-10 opacity-90">{tvDetail(alert)}</span>
            </span>
          </section>
        </div>
      )}
    </main>
  );
}

function Column({
  id,
  title,
  count,
  countClass,
  children,
}: {
  id: string;
  title: string;
  count: number;
  countClass?: string;
  children: React.ReactNode;
}) {
  return (
    <section
      aria-labelledby={id}
      className="flex min-h-0 flex-col gap-[18px] overflow-hidden rounded-[32px] border border-border bg-card p-7"
    >
      <div className="flex items-center gap-4">
        <h2 id={id} className="text-[34px] font-bold">
          {title}
        </h2>
        <span
          className={cn(
            "flex h-11 min-w-11 items-center justify-center rounded-full bg-secondary px-3.5 text-2xl font-bold",
            countClass,
          )}
        >
          {count}
        </span>
      </div>
      <ul className="flex flex-col gap-[18px]">{children}</ul>
    </section>
  );
}

/** Um cartão do Modo TV: a mesa grande, o que é, o setor junto, e à direita o tempo ou a tag. */
function TvItem({
  label,
  tone,
  title,
  detail,
  detailClass,
  end,
  className,
}: {
  label: string;
  tone?: Tone;
  title: string;
  detail: string;
  detailClass?: string;
  end?: React.ReactNode;
  className?: string;
}) {
  return (
    <li
      aria-label={`${calls.table} ${label} · ${title}`}
      className={cn("flex items-center gap-[22px] rounded-3xl border border-border bg-muted px-[22px] py-5", className)}
    >
      <TableBox label={label} tone={tone} size="tv" />
      <span className="flex min-w-0 flex-1 flex-col gap-1.5">
        <span className="text-[30px] leading-9 font-semibold">{title}</span>
        <span className={cn("line-clamp-2 text-[22px] leading-7 text-muted-foreground", detailClass)}>{detail}</span>
      </span>
      {end}
    </li>
  );
}
