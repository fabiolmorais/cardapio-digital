"use client";

import { useQuery } from "@tanstack/react-query";
import { Bell, BellOff } from "lucide-react";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import {
  disablePush,
  enablePush,
  fetchPushSettings,
  PushPermissionDenied,
  pushStatus,
  syncPush,
  type PushStatus,
} from "@/lib/pwa/push";
import { messages } from "@/messages/pt-BR";

const t = messages.pwa;

/**
 * "Avisos no celular": liga o web push deste aparelho para os chamados e os pedidos a confirmar,
 * no navegador e no back. Some se o servidor está sem as chaves VAPID.
 */
export function PushCard() {
  const settings = useQuery({ queryKey: ["push-settings"], queryFn: fetchPushSettings, retry: false });
  const [status, setStatus] = useState<PushStatus | null>(null);
  const [busy, setBusy] = useState(false);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    void pushStatus().then((current) => {
      setStatus(current);
      // Quem entrou agora neste aparelho passa a receber os avisos que já estavam ligados.
      if (current === "on") syncPush().catch(() => {});
    });
  }, []);

  if (!settings.data?.enabled || !settings.data.publicKey || status === null) return null;
  const publicKey = settings.data.publicKey;

  async function enable() {
    setBusy(true);
    setFailed(false);
    try {
      await enablePush(publicKey);
      setStatus("on");
    } catch (error) {
      if (error instanceof PushPermissionDenied) setStatus(await pushStatus());
      else setFailed(true);
    } finally {
      setBusy(false);
    }
  }

  async function disable() {
    setBusy(true);
    try {
      await disablePush();
    } finally {
      setStatus(await pushStatus());
      setBusy(false);
    }
  }

  const on = status === "on";
  const text = {
    on: t.push.on,
    off: t.push.off,
    denied: t.push.denied,
    "needs-install": t.push.needsInstall,
    unsupported: t.push.unsupported,
  }[status];

  return (
    <section aria-label={t.push.title} className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-5">
      <div className="flex items-start gap-4">
        <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-primary/15 text-brand-text">
          {on ? <Bell aria-hidden className="size-6" /> : <BellOff aria-hidden className="size-6" />}
        </span>
        <div className="flex min-w-0 flex-col gap-1">
          <h2 className="text-lg font-semibold">{t.push.title}</h2>
          <p role="status" className="text-sm text-muted-foreground">
            {text}
          </p>
          {failed && (
            <p role="alert" className="text-sm text-destructive">
              {t.push.error}
            </p>
          )}
        </div>
      </div>
      {status === "off" && (
        <Button size="lg" className="h-12" onClick={() => void enable()} disabled={busy}>
          <Bell aria-hidden />
          {busy ? t.push.enabling : t.push.enable}
        </Button>
      )}
      {on && (
        <Button variant="outline" className="self-start" onClick={() => void disable()} disabled={busy}>
          {t.push.disable}
        </Button>
      )}
    </section>
  );
}
