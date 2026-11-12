"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Ban } from "lucide-react";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { Button } from "@/components/ui/button";
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
import { Badge, ColumnHead, TableBox, Timer, elapsed, waitLevel } from "@/features/live/live-ui";
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
import { fetchOpenTabs, fetchTab } from "@/features/tabs/tabs-api";
import { PaymentDialog } from "./payment-dialog";
import { useLiveCalls } from "./use-live-calls";

const t = messages.panel.calls;
type Role = "OWNER" | "MANAGER" | "STAFF" | "KITCHEN";

/** O relógio da tela, de segundo em segundo, para os cronômetros andarem sozinhos. */
export function useNow() {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);
  return now;
}

export { elapsed };

/** Mesa e setor juntos, como o desenho pede ("Salão principal" logo abaixo do tipo do chamado). */
export function callDetail(call: StaffCall) {
  const unlock = call.type === "UNLOCK";
  return [
    call.sectorName,
    unlock ? t.notPresent : call.paymentMethod && t.payment[call.paymentMethod],
    call.presenceMethod && t.presence[call.presenceMethod],
  ]
    .filter(Boolean)
    .join(" · ");
}

/**
 * A coluna "Chamados" do Ao vivo (desenho 7.1): um cartão por chamado, do que espera há mais
 * tempo ao mais novo, com a mesa, o tipo, o setor, o cronômetro na cor do tempo e os botões
 * (Aceitar, Liberar, Concluir, Receber pagamento). A cozinha só acompanha. Com `sector`, só os
 * chamados daquele setor. Atualiza na hora, pelo tempo real, e toca o alerta a cada chamado novo.
 */
export function CallQueue({ role, sector = null }: { role: Role; sector?: string | null }) {
  const queryClient = useQueryClient();
  const { queue } = useLiveCalls(useCallback(() => playChime(), []));
  const now = useNow();
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [blocking, setBlocking] = useState<StaffCall | null>(null);
  const [releasing, setReleasing] = useState<StaffCall | null>(null);
  const [paying, setPaying] = useState<StaffCall | null>(null);
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

  /**
   * "Quero o rodízio": se a mesa ainda não está no rodízio, o diálogo ativa; se já está (alguém
   * chegou depois e quer participar), ele abre no "Ajustar pessoas" com o rodízio da comanda.
   */
  async function openRodizio(call: StaffCall) {
    let current: RodizioTarget["current"] = null;
    try {
      const open = (await fetchOpenTabs()).find((tab) => tab.tableId === call.tableId);
      if (open) current = (await fetchTab(open.id)).rodizio ?? null;
    } catch {
      // Sem resposta: abre na ativação, e o back avisa se a mesa já está no rodízio.
    }
    setActivating({ tableId: call.tableId, tableLabel: call.tableLabel, requestedAt: call.createdAt, current });
  }

  const calls = (queue.data ?? []).filter((call) => sector === null || call.sectorName === sector);

  return (
    <section aria-labelledby="chamados" className="flex min-w-0 flex-col rounded-[20px] border border-border bg-card">
      <ColumnHead id="chamados" title={t.title} count={calls.length} hint={t.order} tone="accent" />
      <span className="sr-only">{t.open(calls.length)}</span>
      <div className="flex flex-col gap-2.5 px-3 pb-3">
        {!canAct && <p className="px-1 text-sm text-muted-foreground">{t.kitchenOnly}</p>}
        {error && (
          <p role="alert" className="px-1 text-sm text-destructive">
            {error}
          </p>
        )}

        {queue.isPending ? (
          <p className="px-1 text-sm text-muted-foreground">{t.loading}</p>
        ) : queue.isError && !queue.data ? (
          <p role="alert" className="px-1 text-sm text-destructive">
            {t.loadError}
          </p>
        ) : calls.length === 0 ? (
          <p className="px-1 pb-2 text-sm text-muted-foreground">{t.empty}</p>
        ) : (
          <ul className="flex flex-col gap-2.5">
            {calls.map((call) => {
              const waited = (now - Date.parse(call.createdAt)) / 1000;
              const pending = call.status === "PENDING";
              const level = waitLevel(waited, !pending);
              const title = `${t.types[call.type]} · ${t.table} ${call.tableLabel}`;
              const unlock = call.type === "UNLOCK";
              // "Quero o rodízio" se resolve ativando: o chamado termina sozinho na ativação.
              const rodizio = call.type === "RODIZIO";
              const activate = rodizio && canAct && (
                <Button
                  size="sm"
                  className="h-9 flex-1"
                  disabled={busy !== null}
                  onClick={() => void openRodizio(call)}
                >
                  {t.activateRodizio}
                </Button>
              );
              return (
                <li
                  key={call.id}
                  aria-label={title}
                  className={cn(
                    "flex flex-col gap-3 rounded-2xl border border-border bg-muted p-3.5",
                    pending &&
                      level === "late" &&
                      "border-destructive/45 bg-[linear-gradient(180deg,rgb(248_113_113/10%),transparent_60%)]",
                    pending && unlock && "border-brand-text/50 ring-3 ring-primary/15",
                  )}
                >
                  <div className="flex items-center gap-3">
                    <TableBox label={call.tableLabel} />
                    <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                      <span className="font-semibold">{t.types[call.type]}</span>
                      <span className="truncate text-[13px] text-muted-foreground">{callDetail(call)}</span>
                    </span>
                    <Timer level={level}>
                      <span className="sr-only">{t.waitingFor(elapsed(waited))}</span>
                      <span aria-hidden>{elapsed(waited)}</span>
                    </Timer>
                  </div>
                  {pending && canAct && !unlock && (
                    <div className="flex gap-2">
                      {activate}
                      <Button
                        size="sm"
                        variant={rodizio ? "outline" : "default"}
                        className="h-9 flex-1"
                        disabled={busy !== null}
                        onClick={() => act(call.id, acceptCall)}
                      >
                        {busy === call.id ? t.accepting : t.accept}
                      </Button>
                    </div>
                  )}
                  {pending && canAct && unlock && (
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        className="h-9 flex-1"
                        disabled={busy !== null}
                        onClick={() => setReleasing(call)}
                      >
                        {t.release}
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        className="h-9 border-destructive/35 bg-transparent text-destructive"
                        disabled={busy !== null}
                        onClick={() => act(call.id, declineCall)}
                      >
                        {t.decline}
                      </Button>
                    </div>
                  )}
                  {!pending && (
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge tone="warn" className="mr-auto">
                        {t.inProgress(call.acceptedByName)}
                      </Badge>
                      {activate}
                      {canAct && call.type === "BILL" && (
                        <Button size="sm" className={GREEN} disabled={busy !== null} onClick={() => setPaying(call)}>
                          {t.receive}
                        </Button>
                      )}
                      {canAct && call.type !== "BILL" && (
                        <Button
                          size="sm"
                          className={GREEN}
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
                      className="flex w-fit items-center gap-1.5 text-[13px] text-muted-foreground underline-offset-4 hover:text-destructive hover:underline focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none disabled:opacity-60"
                    >
                      <Ban aria-hidden className="size-3.5" />
                      {t.block}
                    </button>
                  )}
                </li>
              );
            })}
          </ul>
        )}

        {canAct && <BlockedPhones now={now} />}
      </div>

      <RodizioDialog target={activating} onClose={() => setActivating(null)} />
      <PaymentDialog
        call={paying}
        onClose={() => setPaying(null)}
        onDone={() => queryClient.invalidateQueries({ queryKey: ["call-queue"] })}
      />
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

/** O verde do "Concluir" e do "Entregue" (o botão de terminar, no desenho). */
export const GREEN = "h-9 bg-[#15803d] text-white hover:bg-[#166534]";

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
