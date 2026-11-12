"use client";

import { useQuery } from "@tanstack/react-query";
import { Bell, BellOff } from "lucide-react";
import { useEffect, useState } from "react";
import Link from "next/link";
import { TONE } from "@/features/live/live-ui";
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
import { cn } from "cn";

const t = messages.pwa;

/**
 * Os avisos no celular, como tag no topo do Ao vivo, ao lado do "Som ligado" (desenho 7.1): liga
 * e desliga o web push deste aparelho (chamados e pedidos a confirmar), no navegador e no back,
 * com um toque. Bloqueado pelo navegador, a tag diz isso e explica no toque; no iPhone sem o app
 * instalado, leva para "Instalar o app". Some se o servidor está sem as chaves VAPID ou se o
 * navegador não recebe avisos.
 */
export function PushTag() {
  const settings = useQuery({ queryKey: ["push-settings"], queryFn: fetchPushSettings, retry: false });
  const [status, setStatus] = useState<PushStatus | null>(null);
  const [busy, setBusy] = useState(false);
  const [failed, setFailed] = useState(false);
  const [explain, setExplain] = useState(false);

  useEffect(() => {
    void pushStatus().then((current) => {
      setStatus(current);
      // Quem entrou agora neste aparelho passa a receber os avisos que já estavam ligados.
      if (current === "on") syncPush().catch(() => {});
    });
  }, []);

  if (!settings.data?.enabled || !settings.data.publicKey || status === null || status === "unsupported") {
    return null;
  }
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

  const tag =
    "inline-flex h-9 items-center gap-1.5 rounded-full px-3 text-[13px] font-semibold focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none disabled:opacity-60";

  if (status === "needs-install") {
    return (
      <Link href="/painel/instalar" title={t.push.needsInstall} className={cn(tag, TONE.neutral)}>
        <BellOff aria-hidden className="size-4" />
        {t.push.tagInstall}
      </Link>
    );
  }

  if (status === "denied") {
    return (
      <span className="relative">
        <button
          type="button"
          aria-expanded={explain}
          aria-controls="avisos-bloqueados"
          onClick={() => setExplain((v) => !v)}
          className={cn(tag, TONE.danger)}
        >
          <BellOff aria-hidden className="size-4" />
          {t.push.tagDenied}
        </button>
        {explain && (
          <span
            id="avisos-bloqueados"
            role="status"
            className="absolute top-11 right-0 z-10 w-72 rounded-xl border border-border bg-popover p-3 text-[13px] text-muted-foreground shadow-lg"
          >
            {t.push.denied}
          </span>
        )}
      </span>
    );
  }

  const on = status === "on";
  return (
    <button
      type="button"
      aria-pressed={on}
      aria-label={on ? t.push.tagOnLabel : t.push.tagOffLabel}
      title={failed ? t.push.error : on ? t.push.on : t.push.off}
      disabled={busy}
      onClick={() => void (on ? disable() : enable())}
      className={cn(tag, failed ? TONE.danger : on ? TONE.ok : TONE.neutral)}
    >
      {on ? <Bell aria-hidden className="size-4" /> : <BellOff aria-hidden className="size-4" />}
      {busy ? t.push.enabling : failed ? t.push.tagFailed : on ? t.push.tagOn : t.push.tagOff}
    </button>
  );
}
