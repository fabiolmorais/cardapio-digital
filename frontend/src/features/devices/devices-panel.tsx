"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Clock, KeyRound, QrCode, Smartphone, Tablet, Tv } from "lucide-react";
import { useEffect, useRef, useState, type FormEvent } from "react";
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
import { messages } from "@/messages/pt-BR";
import {
  fetchDevices,
  inviteDevice,
  linkDevice,
  unlinkDevice,
  type Device,
  type DeviceInvite,
  type DeviceType,
} from "./device-api";

const t = messages.devices;

const TYPES: DeviceType[] = ["WAITER", "KITCHEN", "TV"];
const ICONS = { WAITER: Smartphone, KITCHEN: Tablet, TV: Tv } as const;
/** Enquanto algum aparelho ainda não buscou o token, a lista se atualiza sozinha. */
const WAITING_REFRESH_MS = 4_000;

const relative = new Intl.RelativeTimeFormat("pt-BR", { numeric: "auto" });

function seenAgo(iso: string): string {
  const seconds = Math.round((Date.parse(iso) - Date.now()) / 1000);
  if (seconds > -60) return relative.format(0, "second");
  const minutes = Math.round(seconds / 60);
  if (minutes > -60) return relative.format(minutes, "minute");
  const hours = Math.round(minutes / 60);
  if (hours > -24) return relative.format(hours, "hour");
  return relative.format(Math.round(hours / 24), "day");
}

/** Aparelhos do restaurante: lista, vincular (por QR Code ou pelo código) e desvincular. */
export function DevicesPanel() {
  const queryClient = useQueryClient();
  const [unlinking, setUnlinking] = useState<Device | null>(null);

  const devices = useQuery({
    queryKey: ["devices"],
    queryFn: fetchDevices,
    refetchInterval: (query) =>
      query.state.data?.content.some((device) => !device.lastSeenAt) ? WAITING_REFRESH_MS : false,
  });

  return (
    <aside
      aria-labelledby="devices-title"
      className="flex w-full flex-col gap-4 rounded-2xl border border-border bg-card p-5 xl:max-w-sm"
    >
      <div className="flex flex-col gap-1">
        <div className="flex items-center justify-between">
          <h2 id="devices-title" className="font-semibold">
            {t.title}
          </h2>
          {devices.data && <span className="text-sm text-muted-foreground">{devices.data.totalElements}</span>}
        </div>
        <p className="text-sm text-muted-foreground">{t.description}</p>
      </div>

      {devices.isPending ? (
        <p className="text-sm text-muted-foreground">{t.loading}</p>
      ) : devices.isError ? (
        <p role="alert" className="text-sm text-destructive">
          {t.loadError}
        </p>
      ) : devices.data.content.length === 0 ? (
        <p className="text-sm text-muted-foreground">{t.empty}</p>
      ) : (
        <ul className="flex flex-col">
          {devices.data.content.map((device) => {
            const Icon = ICONS[device.type];
            return (
              <li key={device.id} className="flex items-center gap-3 border-b border-border py-3 last:border-0">
                <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-secondary">
                  <Icon aria-hidden className="size-4.5" />
                </span>
                <span className="flex min-w-0 flex-1 flex-col">
                  <span className="truncate font-medium">{device.name}</span>
                  <span className="text-sm text-muted-foreground">
                    {t.types[device.type]} · {device.lastSeenAt ? t.seen(seenAgo(device.lastSeenAt)) : t.waiting}
                  </span>
                </span>
                <Button
                  variant="ghost"
                  className="h-10 px-3 text-destructive hover:text-destructive"
                  aria-label={t.unlinkLabel(device.name)}
                  onClick={() => setUnlinking(device)}
                >
                  {t.unlink}
                </Button>
              </li>
            );
          })}
        </ul>
      )}

      <div aria-hidden className="h-px bg-border" />
      <LinkDeviceForm onLinked={() => queryClient.invalidateQueries({ queryKey: ["devices"] })} />

      <UnlinkDialog device={unlinking} onOpenChange={(open) => !open && setUnlinking(null)} />
    </aside>
  );
}

type Method = "qr" | "code";
const METHODS: { value: Method; Icon: typeof QrCode }[] = [
  { value: "qr", Icon: QrCode },
  { value: "code", Icon: KeyRound },
];

function LinkDeviceForm({ onLinked }: { onLinked: () => void }) {
  const formRef = useRef<HTMLFormElement>(null);
  const [method, setMethod] = useState<Method>("qr");
  const [invite, setInvite] = useState<DeviceInvite | null>(null);
  const mutation = useMutation({
    mutationFn: async (form: FormData) => {
      const name = String(form.get("name") ?? "").trim();
      const type = String(form.get("type") ?? "WAITER") as DeviceType;
      if (method === "qr") return { kind: "qr" as const, invite: await inviteDevice({ name, type }) };
      const code = String(form.get("code") ?? "").trim();
      return { kind: "code" as const, device: await linkDevice({ code, name, type }) };
    },
    onSuccess: (result) => {
      onLinked();
      if (result.kind === "qr") {
        setInvite(result.invite);
        return;
      }
      formRef.current?.reset();
    },
  });

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    mutation.mutate(new FormData(event.currentTarget));
  }

  const linked = mutation.data?.kind === "code" ? mutation.data.device : null;

  return (
    <>
      <form ref={formRef} onSubmit={onSubmit} className="flex flex-col gap-4" aria-labelledby="link-device-title">
        <h3 id="link-device-title" className="text-sm font-semibold">
          {t.form.title}
        </h3>

        <fieldset className="grid grid-cols-2 gap-2">
          <legend className="sr-only">{t.form.method}</legend>
          {METHODS.map(({ value, Icon }) => (
            <label
              key={value}
              className="flex min-h-12 cursor-pointer items-start gap-2.5 rounded-xl border border-border bg-background/40 px-3 py-2.5 transition-colors has-checked:border-ring has-checked:bg-secondary has-focus-visible:ring-3 has-focus-visible:ring-ring/50"
            >
              <input
                type="radio"
                name="method"
                value={value}
                checked={method === value}
                onChange={() => {
                  setMethod(value);
                  mutation.reset();
                }}
                className="mt-0.5 size-4 shrink-0 accent-primary"
              />
              <span className="flex min-w-0 flex-col gap-0.5">
                <span className="flex items-center gap-1.5 text-sm font-medium">
                  <Icon aria-hidden className="size-4 text-muted-foreground" />
                  {t.form.methods[value].name}
                </span>
                <span className="text-xs text-muted-foreground">{t.form.methods[value].hint}</span>
              </span>
            </label>
          ))}
        </fieldset>

        {method === "code" && (
          <div className="flex flex-col gap-2">
            <Label htmlFor="device-code" className="text-muted-foreground">
              {t.form.code}
            </Label>
            <Input
              id="device-code"
              name="code"
              inputMode="numeric"
              autoComplete="off"
              pattern="\d{6}"
              minLength={6}
              maxLength={6}
              required
              placeholder={t.form.codePlaceholder}
              aria-describedby="device-code-hint"
              className="h-12 rounded-xl px-4 font-mono text-lg tracking-[0.3em] placeholder:font-sans placeholder:text-base placeholder:tracking-normal"
            />
            <span id="device-code-hint" className="text-xs text-muted-foreground">
              {t.form.codeHint}
            </span>
          </div>
        )}

        <div className="flex flex-col gap-2">
          <Label htmlFor="device-name" className="text-muted-foreground">
            {t.form.name}
          </Label>
          <Input
            id="device-name"
            name="name"
            required
            maxLength={60}
            autoComplete="off"
            placeholder={t.form.namePlaceholder}
            className="h-12 rounded-xl px-4 text-base"
          />
        </div>

        <fieldset className="flex flex-col gap-2">
          <legend className="mb-2 text-sm font-medium text-muted-foreground">{t.form.type}</legend>
          {TYPES.map((type) => {
            const Icon = ICONS[type];
            return (
              <label
                key={type}
                className="flex min-h-12 cursor-pointer items-center gap-3 rounded-xl border border-border bg-background/40 px-3 py-2 transition-colors has-checked:border-ring has-checked:bg-secondary has-focus-visible:ring-3 has-focus-visible:ring-ring/50"
              >
                <input
                  type="radio"
                  name="type"
                  value={type}
                  defaultChecked={type === "WAITER"}
                  className="size-4 accent-primary"
                />
                <Icon aria-hidden className="size-4 text-muted-foreground" />
                <span className="flex flex-col">
                  <span className="text-sm font-medium">{t.types[type]}</span>
                  <span className="text-xs text-muted-foreground">{t.typeDescriptions[type]}</span>
                </span>
              </label>
            );
          })}
        </fieldset>

        <p role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive empty:min-h-0">
          {mutation.error?.message ?? ""}
        </p>
        {linked && (
          <p role="status" className="text-sm text-success">
            {t.form.success(linked.name)}
          </p>
        )}

        <Button type="submit" size="xl" disabled={mutation.isPending}>
          {method === "qr"
            ? mutation.isPending
              ? t.form.generatingQr
              : t.form.showQr
            : mutation.isPending
              ? t.form.submitting
              : t.form.submit}
        </Button>
      </form>

      <InviteDialog
        invite={invite}
        onRenew={() => formRef.current && mutation.mutate(new FormData(formRef.current))}
        renewing={mutation.isPending}
        onDone={() => {
          setInvite(null);
          formRef.current?.reset();
          mutation.reset();
        }}
      />
    </>
  );
}

function formatRemaining(ms: number): string {
  const total = Math.max(0, Math.ceil(ms / 1000));
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, "0")}`;
}

/**
 * O QR Code do convite, grande, para o aparelho novo escanear. Acompanha a lista até o aparelho
 * aparecer; fechado antes disso (ou vencido), desvincula o aparelho criado: o QR deixa de valer
 * e a lista não fica com um "Aguardando o aparelho" esquecido.
 */
function InviteDialog({
  invite,
  onRenew,
  renewing,
  onDone,
}: {
  invite: DeviceInvite | null;
  onRenew: () => void;
  renewing: boolean;
  onDone: () => void;
}) {
  const queryClient = useQueryClient();
  const [now, setNow] = useState(() => Date.now());
  // O prazo no relógio deste computador: o do servidor pode estar um pouco diferente.
  const [deadline, setDeadline] = useState<{ id: string; at: number } | null>(null);
  const id = invite?.device.id ?? null;

  const devices = useQuery({ queryKey: ["devices"], queryFn: fetchDevices, enabled: false });
  const scanned = devices.data?.content.find((d) => d.id === id)?.lastSeenAt != null;

  useEffect(() => {
    if (!invite) return;
    const frame = requestAnimationFrame(() => {
      const ttl = Date.parse(invite.expiresAt) - Date.parse(invite.device.linkedAt);
      setDeadline({ id: invite.device.id, at: Date.now() + (ttl > 0 ? ttl : 5 * 60_000) });
      setNow(Date.now());
    });
    return () => cancelAnimationFrame(frame);
  }, [invite]);

  const remaining = deadline && deadline.id === id ? deadline.at - now : null;
  const expired = remaining !== null && remaining <= 0;

  // A contagem regressiva. Quem percebe o aparelho escaneando é a lista, que se atualiza sozinha
  // enquanto há aparelho aguardando (e o do convite está).
  useEffect(() => {
    if (!invite || scanned || expired) return;
    const tick = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(tick);
  }, [invite, scanned, expired]);

  /** Cancela o convite que ninguém usou: o aparelho criado sai da lista e o QR deixa de valer. */
  function discard() {
    if (invite && !scanned) {
      void unlinkDevice(invite.device.id)
        .catch(() => {})
        .finally(() => queryClient.invalidateQueries({ queryKey: ["devices"] }));
    }
  }

  function close() {
    discard();
    onDone();
  }

  function renew() {
    discard();
    onRenew();
  }

  return (
    <Dialog open={invite !== null} onOpenChange={(open) => !open && close()}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] gap-5 overflow-y-auto p-6 sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="text-lg">{invite ? t.qr.title(invite.device.name) : ""}</DialogTitle>
          <DialogDescription>{t.qr.description}</DialogDescription>
        </DialogHeader>
        {invite &&
          (scanned ? (
            <p role="status" className="text-success">
              {t.qr.linked(invite.device.name)}
            </p>
          ) : expired ? (
            <div className="flex flex-col items-start gap-3">
              <p role="alert" className="text-destructive">
                {t.qr.expired}
              </p>
              <Button size="lg" className="h-11 px-4" disabled={renewing} onClick={renew}>
                {renewing ? t.form.generatingQr : t.qr.newQr}
              </Button>
            </div>
          ) : (
            <div className="flex flex-col items-center gap-3">
              {/* Fundo branco sempre: o leitor precisa do contraste, também no tema escuro. */}
              {/* eslint-disable-next-line @next/next/no-img-element -- SVG gerado pelo back, sem otimização */}
              <img
                src={`data:image/svg+xml;charset=utf-8,${encodeURIComponent(invite.qrSvg)}`}
                alt={t.qr.alt}
                width={240}
                height={240}
                data-testid="device-invite-qr"
                data-url={invite.url}
                className="size-60 rounded-xl bg-white p-2"
              />
              {remaining !== null && (
                <span className="flex items-center gap-1.5 text-sm text-muted-foreground">
                  <Clock aria-hidden className="size-3.5" />
                  {t.qr.validFor(formatRemaining(remaining))}
                </span>
              )}
              <p role="status" className="text-sm text-muted-foreground">
                {t.qr.waiting}
              </p>
            </div>
          ))}
        <DialogFooter className="-mx-6 -mb-6 gap-2 px-6 pb-6">
          <DialogClose render={<Button variant={scanned ? "default" : "outline"} size="lg" className="h-11 px-4" />}>
            {scanned ? t.qr.done : t.qr.close}
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function UnlinkDialog({ device, onOpenChange }: { device: Device | null; onOpenChange: (open: boolean) => void }) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: (id: string) => unlinkDevice(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["devices"] });
      onOpenChange(false);
    },
  });

  function handleOpenChange(next: boolean) {
    if (!next) mutation.reset();
    onOpenChange(next);
  }

  return (
    <Dialog open={device !== null} onOpenChange={handleOpenChange}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] gap-5 overflow-y-auto p-6 sm:max-w-md" role="alertdialog">
        <DialogHeader>
          <DialogTitle className="text-lg">{device ? t.confirm.title(device.name) : ""}</DialogTitle>
          <DialogDescription>{t.confirm.description}</DialogDescription>
        </DialogHeader>
        <p role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
          {mutation.error?.message ?? ""}
        </p>
        <DialogFooter className="-mx-6 -mb-6 gap-2 px-6 pb-6">
          <DialogClose render={<Button variant="outline" size="lg" className="h-11 px-4" />}>
            {t.confirm.cancel}
          </DialogClose>
          <Button
            variant="destructive"
            size="lg"
            className="h-11 px-4"
            disabled={mutation.isPending}
            onClick={() => device && mutation.mutate(device.id)}
          >
            {mutation.isPending ? t.confirm.confirming : t.confirm.confirm}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
