"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Smartphone, Tablet, Tv } from "lucide-react";
import { useRef, useState, type FormEvent } from "react";
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
import { fetchDevices, linkDevice, unlinkDevice, type Device, type DeviceType } from "./device-api";

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

/** Aparelhos do restaurante: lista, vincular pelo código e desvincular. */
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

function LinkDeviceForm({ onLinked }: { onLinked: () => void }) {
  const formRef = useRef<HTMLFormElement>(null);
  const mutation = useMutation({
    mutationFn: (form: FormData) =>
      linkDevice({
        code: String(form.get("code") ?? "").trim(),
        name: String(form.get("name") ?? "").trim(),
        type: String(form.get("type") ?? "WAITER") as DeviceType,
      }),
    onSuccess: () => {
      formRef.current?.reset();
      onLinked();
    },
  });

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    mutation.mutate(new FormData(event.currentTarget));
  }

  return (
    <form ref={formRef} onSubmit={onSubmit} className="flex flex-col gap-4" aria-labelledby="link-device-title">
      <h3 id="link-device-title" className="text-sm font-semibold">
        {t.form.title}
      </h3>

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
      {mutation.isSuccess && (
        <p role="status" className="text-sm text-success">
          {t.form.success(mutation.data.name)}
        </p>
      )}

      <Button type="submit" size="xl" disabled={mutation.isPending}>
        {mutation.isPending ? t.form.submitting : t.form.submit}
      </Button>
    </form>
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
          <DialogClose render={<Button variant="outline" size="lg" className="h-11 px-4" />}>{t.confirm.cancel}</DialogClose>
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
