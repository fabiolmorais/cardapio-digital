"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Ban, Tv } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { Button, buttonVariants } from "@/components/ui/button";
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
import { RodizioDialog, type RodizioTarget } from "@/features/rodizio/rodizio-dialog";
import { canManageTeam } from "@/features/team/team-rules";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { playChime } from "./alert-sound";
import { subscribeStaffEvents } from "@/lib/realtime/shared-staff-events";
import {
  acceptCall,
  blockCaller,
  declineCall,
  fetchBlockedPhones,
  finishCall,
  releaseCall,
  unblockPhone,
  type StaffCall,
} from "./calls-api";
import { SoundToggle } from "./sound-toggle";
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
  const [releasing, setReleasing] = useState<StaffCall | null>(null);
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
          className={cn(
            "ml-auto flex items-center gap-1.5 text-sm",
            connected ? "text-success" : "text-muted-foreground",
          )}
        >
          <span aria-hidden className={cn("size-2 rounded-full", connected ? "bg-success" : "bg-muted-foreground")} />
          {connected ? t.live : t.reconnecting}
        </span>
      </div>

      <div className="flex flex-wrap gap-2">
        <SoundToggle />
        {canManageTeam(role) && (
          <Link href="/painel/tv" className={buttonVariants({ variant: "outline" })}>
            <Tv aria-hidden />
            {t.openTv}
          </Link>
        )}
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
                    <span className="text-[10px] font-semibold tracking-wide text-muted-foreground uppercase">
                      {t.table}
                    </span>
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
                      level === "warn" && "text-warning",
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
                    <Button className="h-11 flex-1" disabled={busy !== null} onClick={() => setReleasing(call)}>
                      {t.release}
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

      {canAct && <BlockedPhones now={now} />}

      <RodizioDialog target={activating} onClose={() => setActivating(null)} />
      <ReleaseDialog
        call={releasing}
        onClose={() => setReleasing(null)}
        onReleased={() => queryClient.invalidateQueries({ queryKey: ["call-queue"] })}
      />
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
          await queryClient.invalidateQueries({ queryKey: ["blocked-phones"] });
        }}
      />
    </section>
  );
}

/**
 * "Liberar mesa": o garçom digita o código que aparece no celular de quem pediu. É a prova de que
 * ele viu o celular na mesa; quem pediu de casa não tem como mostrar a tela.
 */
function ReleaseDialog({
  call,
  onClose,
  onReleased,
}: {
  call: StaffCall | null;
  onClose: () => void;
  onReleased: () => Promise<unknown>;
}) {
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const r = t.releaseDialog;

  function close() {
    setCode("");
    setError(null);
    onClose();
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!call) return;
    if (!/^\d{4}$/.test(code)) {
      setError(r.invalid);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await releaseCall(call.id, code);
      await onReleased();
      close();
    } catch (e) {
      setError((e as Error).message);
      await onReleased(); // a fila mostra se o pedido foi cancelado ou respondido por outra pessoa
    } finally {
      setBusy(false);
    }
  }

  return (
    <Dialog open={call !== null} onOpenChange={(open) => !open && close()}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] gap-5 overflow-y-auto p-6 sm:max-w-md">
        <form onSubmit={submit} className="flex flex-col gap-5">
          <DialogHeader>
            <DialogTitle className="text-lg">{call ? r.title(call.tableLabel) : ""}</DialogTitle>
            <DialogDescription>{r.description}</DialogDescription>
          </DialogHeader>
          <div className="flex flex-col gap-2">
            <Label htmlFor="release-code" className="text-muted-foreground">
              {r.code}
            </Label>
            <Input
              id="release-code"
              value={code}
              onChange={(event) => setCode(event.target.value.replace(/\D/g, "").slice(0, 4))}
              inputMode="numeric"
              autoComplete="off"
              autoFocus
              placeholder={r.placeholder}
              aria-describedby="release-code-error"
              className="h-14 rounded-xl px-4 text-center font-mono text-2xl tracking-[0.5em] placeholder:font-sans placeholder:text-base placeholder:tracking-normal"
            />
            <p id="release-code-error" role="alert" className="min-h-5 text-sm text-destructive">
              {error ?? ""}
            </p>
          </div>
          <DialogFooter className="-mx-6 -mb-6 gap-2 px-6 pb-6">
            <DialogClose render={<Button type="button" variant="outline" size="lg" className="h-11 px-4" />}>
              {r.cancel}
            </DialogClose>
            <Button type="submit" size="lg" className="h-11 px-4" disabled={busy}>
              {busy ? t.releasing : r.confirm}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

const minutesFormat = new Intl.RelativeTimeFormat("pt-BR", { numeric: "auto" });

/** "agora", "há 5 minutos", "há 2 horas". */
function ago(iso: string, now: number) {
  const minutes = Math.round((Date.parse(iso) - now) / 60_000);
  if (minutes > -1) return t.blocked.now;
  if (minutes > -60) return minutesFormat.format(minutes, "minute");
  return minutesFormat.format(Math.round(minutes / 60), "hour");
}

/**
 * Os celulares bloqueados pela equipe, com o "Desbloquear". O bloqueio cancela os chamados do
 * celular, então ele some da fila: é aqui que se desfaz um bloqueio por engano. Só aparece quando
 * há algum; o tempo real avisa quando alguém bloqueia ou desbloqueia (em qualquer aparelho).
 */
function BlockedPhones({ now }: { now: number }) {
  const queryClient = useQueryClient();
  const blocked = useQuery({ queryKey: ["blocked-phones"], queryFn: fetchBlockedPhones, retry: false });
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const b = t.blocked;

  useEffect(
    () =>
      subscribeStaffEvents(
        (event) => {
          if (event.name === "ready" || event.name === "phone") {
            void queryClient.invalidateQueries({ queryKey: ["blocked-phones"] });
          }
        },
        () => {},
      ),
    [queryClient],
  );

  async function unblock(id: string) {
    setBusy(id);
    setError(null);
    try {
      await unblockPhone(id);
    } catch (e) {
      setError((e as Error).message);
    }
    await queryClient.invalidateQueries({ queryKey: ["blocked-phones"] });
    setBusy(null);
  }

  const phones = blocked.data ?? [];
  if (phones.length === 0) return null;

  return (
    <section aria-labelledby="bloqueados" className="flex flex-col gap-2.5 border-t border-border pt-4">
      <h3 id="bloqueados" className="font-semibold">
        {b.title} <span className="text-sm font-normal text-muted-foreground">{phones.length}</span>
      </h3>
      <p className="text-sm text-muted-foreground">{b.hint}</p>
      {error && (
        <p role="alert" className="text-sm text-destructive">
          {error}
        </p>
      )}
      <ul className="flex flex-col gap-2">
        {phones.map((phone) => (
          <li
            key={phone.id}
            aria-label={b.item(phone.tableLabel, phone.sectorName)}
            className="flex flex-wrap items-center gap-3 rounded-xl border border-destructive/30 bg-destructive/5 p-3"
          >
            <Ban aria-hidden className="size-5 shrink-0 text-destructive" />
            <span className="flex min-w-40 flex-1 flex-col">
              <span className="font-medium">{b.item(phone.tableLabel, phone.sectorName)}</span>
              <span className="text-sm text-muted-foreground">
                {b.by(phone.blockedByName, ago(phone.blockedAt, now))}
              </span>
            </span>
            <Button
              variant="outline"
              className="h-11"
              aria-label={b.unblockLabel(phone.tableLabel)}
              disabled={busy !== null}
              onClick={() => void unblock(phone.id)}
            >
              {busy === phone.id ? b.unblocking : b.unblock}
            </Button>
          </li>
        ))}
      </ul>
    </section>
  );
}
