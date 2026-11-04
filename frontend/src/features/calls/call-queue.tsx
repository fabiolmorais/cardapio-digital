"use client";

import { useQueryClient } from "@tanstack/react-query";
import { Ban, Tv, Volume2 } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { Button, buttonVariants } from "@/components/ui/button";
import { RodizioDialog, type RodizioTarget } from "@/features/rodizio/rodizio-dialog";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { enableSound, playChime } from "./alert-sound";
import { acceptCall, blockCaller, declineCall, finishCall, type StaffCall } from "./calls-api";
import { useLiveCalls, useSoundOn } from "./use-live-calls";

const t = messages.panel.calls;
type Role = "OWNER" | "MANAGER" | "STAFF" | "KITCHEN";

/** Até 2 min, normal; até 4 min, atenção; depois, atrasado (desenho 7.1). */
const WARN_AFTER_S = 120;
const LATE_AFTER_S = 240;

/** O relógio da tela, de segundo em segundo, para os cronômetros andarem sozinhos. */
export function useNow() {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);
  return now;
}

/** 252 s → "4:12". */
export function elapsed(seconds: number) {
  const s = Math.max(0, Math.floor(seconds));
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;
}

/**
 * A fila de chamados do painel (desenho 7.1, coluna Chamados): a mesa, o tipo, há quanto tempo
 * espera, e os botões Aceitar e Concluir. A cozinha só acompanha. Atualiza na hora, pelo tempo
 * real, e toca o alerta a cada chamado novo, se o som estiver ligado.
 */
export function CallQueue({ role }: { role: Role }) {
  const queryClient = useQueryClient();
  const { queue, connected } = useLiveCalls(useCallback(() => playChime(), []));
  const soundOn = useSoundOn();
  const now = useNow();
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [blocking, setBlocking] = useState<StaffCall | null>(null);
  const [activating, setActivating] = useState<RodizioTarget | null>(null);
  const canAct = role !== "KITCHEN";

  async function act(id: string, action: (id: string) => Promise<StaffCall>) {
    setBusy(id);
    setError(null);
    try {
      await action(id);
    } catch (e) {
      // 409: outra pessoa aceitou antes, ou a mesa cancelou. A fila atualizada mostra o porquê.
      setError((e as Error).message);
    }
    await queryClient.invalidateQueries({ queryKey: ["call-queue"] });
    setBusy(null);
  }

  const calls = queue.data ?? [];

  return (
    <section aria-labelledby="chamados" className="flex flex-col gap-3 rounded-2xl border border-border bg-card p-5">
      <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
        <h2 id="chamados" className="text-xl font-semibold">
          {t.title}
        </h2>
        {queue.data && <span className="text-sm font-semibold text-brand-text">{t.open(calls.length)}</span>}
        <span className="text-sm text-muted-foreground">{t.order}</span>
        <span
          role="status"
          className={cn("ml-auto flex items-center gap-1.5 text-sm", connected ? "text-success" : "text-muted-foreground")}
        >
          <span aria-hidden className={cn("size-2 rounded-full", connected ? "bg-success" : "bg-muted-foreground")} />
          {connected ? t.live : t.reconnecting}
        </span>
      </div>

      <div className="flex flex-wrap gap-2">
        {soundOn ? (
          <span className="flex h-9 items-center gap-1.5 rounded-lg bg-muted px-3 text-sm font-medium">
            <Volume2 aria-hidden className="size-4" />
            {t.soundOn}
          </span>
        ) : (
          <Button variant="outline" onClick={() => void enableSound()}>
            <Volume2 aria-hidden />
            {t.enableSound}
          </Button>
        )}
        <Link href="/painel/tv" className={buttonVariants({ variant: "outline" })}>
          <Tv aria-hidden />
          {t.openTv}
        </Link>
      </div>
      {!soundOn && <p className="text-sm text-muted-foreground">{t.soundHint}</p>}

      {!canAct && <p className="text-sm text-muted-foreground">{t.kitchenOnly}</p>}
      {error && (
        <p role="alert" className="text-sm text-destructive">
          {error}
        </p>
      )}

      {queue.isPending ? (
        <p className="text-sm text-muted-foreground">{t.loading}</p>
      ) : queue.isError && !queue.data ? (
        <p role="alert" className="text-sm text-destructive">
          {t.loadError}
        </p>
      ) : calls.length === 0 ? (
        <p className="text-sm text-muted-foreground">{t.empty}</p>
      ) : (
        <ul className="flex flex-col gap-2.5">
          {calls.map((call) => {
            const waited = (now - Date.parse(call.createdAt)) / 1000;
            const level = waited >= LATE_AFTER_S ? "late" : waited >= WARN_AFTER_S ? "warn" : "ok";
            const title = `${t.types[call.type]} · ${t.table} ${call.tableLabel}`;
            const unlock = call.type === "UNLOCK";
            // "Quero o rodízio" se resolve ativando: o chamado termina sozinho na ativação.
            const rodizio = call.type === "RODIZIO";
            const activate = rodizio && canAct && (
              <Button
                className="h-11"
                disabled={busy !== null}
                onClick={() => setActivating({ tableId: call.tableId, tableLabel: call.tableLabel })}
              >
                {t.activateRodizio}
              </Button>
            );
            // O pedido de liberação é justamente de quem não provou a presença; os outros dizem como provaram.
            const detail = [
              unlock ? t.notPresent : call.paymentMethod && t.payment[call.paymentMethod],
              call.sectorName,
              call.presenceMethod && t.presence[call.presenceMethod],
            ]
              .filter(Boolean)
              .join(" · ");
            return (
              <li
                key={call.id}
                aria-label={title}
                className={cn(
                  "flex flex-col gap-3 rounded-xl border border-border bg-background p-3.5",
                  level === "late" && call.status === "PENDING" && "border-destructive",
                )}
              >
                <div className="flex items-center gap-3">
                  <span className="flex size-14 shrink-0 flex-col items-center justify-center rounded-xl bg-muted leading-none">
                    <span className="text-[10px] font-semibold tracking-wide text-muted-foreground uppercase">{t.table}</span>
                    <span className="text-lg font-bold">{call.tableLabel}</span>
                  </span>
                  <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                    <span className="font-semibold">{t.types[call.type]}</span>
                    <span className="truncate text-sm text-muted-foreground">{detail}</span>
                  </span>
                  <span
                    className={cn(
                      "font-mono text-sm font-semibold tabular-nums",
                      level === "ok" && "text-success",
                      level === "warn" && "text-amber-700 dark:text-amber-400",
                      level === "late" && "text-destructive",
                    )}
                  >
                    <span className="sr-only">{t.waitingFor(elapsed(waited))}</span>
                    <span aria-hidden>{elapsed(waited)}</span>
                  </span>
                </div>
                {call.status === "PENDING" && canAct && !unlock && (
                  <div className="flex gap-2 *:flex-1">
                    {activate}
                    <Button
                      variant={rodizio ? "outline" : "default"}
                      className="h-11"
                      disabled={busy !== null}
                      onClick={() => act(call.id, acceptCall)}
                    >
                      {busy === call.id ? t.accepting : t.accept}
                    </Button>
                  </div>
                )}
                {call.status === "PENDING" && canAct && unlock && (
                  <div className="flex gap-2">
                    <Button className="h-11 flex-1" disabled={busy !== null} onClick={() => act(call.id, acceptCall)}>
                      {busy === call.id ? t.releasing : t.release}
                    </Button>
                    <Button
                      variant="outline"
                      className="h-11"
                      disabled={busy !== null}
                      onClick={() => act(call.id, declineCall)}
                    >
                      {t.decline}
                    </Button>
                  </div>
                )}
                {call.status === "IN_PROGRESS" && (
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="mr-auto rounded-full bg-muted px-3 py-1 text-sm font-medium">
                      {t.inProgress(call.acceptedByName)}
                    </span>
                    {activate}
                    {canAct && (
                      <Button
                        variant="outline"
                        className="h-11"
                        disabled={busy !== null}
                        onClick={() => act(call.id, finishCall)}
                      >
                        {busy === call.id ? t.finishing : t.finish}
                      </Button>
                    )}
                  </div>
                )}
                {canAct && (
                  <button
                    type="button"
                    disabled={busy !== null}
                    onClick={() => setBlocking(call)}
                    className="flex w-fit items-center gap-1.5 text-sm text-muted-foreground underline-offset-4 hover:text-destructive hover:underline focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none disabled:opacity-60"
                  >
                    <Ban aria-hidden className="size-4" />
                    {t.block}
                  </button>
                )}
              </li>
            );
          })}
        </ul>
      )}

      <RodizioDialog target={activating} onClose={() => setActivating(null)} />
      <ConfirmDialog
        open={blocking !== null}
        onOpenChange={(open) => !open && setBlocking(null)}
        title={blocking ? t.blockTitle(blocking.tableLabel) : ""}
        description={t.blockText}
        confirmLabel={t.blockConfirm}
        onConfirm={async () => {
          if (!blocking) return;
          await blockCaller(blocking.id);
          await queryClient.invalidateQueries({ queryKey: ["call-queue"] });
        }}
      />
    </section>
  );
}
