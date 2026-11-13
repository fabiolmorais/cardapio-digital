"use client";

import { Volume2 } from "lucide-react";
import { useState, useSyncExternalStore } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { enableSound, soundPreference } from "@/features/calls/alert-sound";
import { useSoundOn } from "@/features/calls/use-live-calls";
import { messages } from "@/messages/pt-BR";

const t = messages.panel.live.soundGate;
/** "Agora não" vale até fechar a aba: ao abrir o painel de novo, o aviso volta. */
const DISMISSED = "ohchefe:sound-gate-dismissed";

const noop = () => () => {};

/** Já escolheu o som neste aparelho (ligado ou desligado) ou respondeu "Agora não" nesta aba. */
function wasDismissed() {
  if (soundPreference() !== null) return true;
  try {
    return sessionStorage.getItem(DISMISSED) === "1";
  } catch {
    return false;
  }
}

/**
 * "Ativar alertas sonoros" (desenho 7.4): na primeira vez que o painel abre neste aparelho, o
 * aviso pede o toque que o navegador exige para tocar som. Depois de escolher (no aviso ou na tag
 * do som), não pergunta mais: o som volta sozinho no primeiro toque (restoreSound). "Agora não"
 * fecha até a aba ser fechada.
 */
export function SoundGate() {
  const soundOn = useSoundOn();
  // No servidor não há sessionStorage: lá conta como fechado, e o aviso só abre no navegador.
  const stored = useSyncExternalStore(noop, wasDismissed, () => true);
  const [dismissed, setDismissed] = useState(false);
  const open = !soundOn && !stored && !dismissed;

  function later() {
    try {
      sessionStorage.setItem(DISMISSED, "1");
    } catch {
      // Sem sessionStorage (aba privada): o aviso fecha só nesta tela.
    }
    setDismissed(true);
  }

  return (
    <Dialog open={open} onOpenChange={(next) => !next && later()}>
      <DialogContent
        className="flex flex-col items-center gap-5 p-8 text-center sm:max-w-[520px]"
        showCloseButton={false}
      >
        <span className="relative flex size-26 items-center justify-center">
          <span aria-hidden className="absolute inset-0 animate-ping rounded-full border-2 border-brand-text/55" />
          <span className="flex size-26 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-[0_16px_40px_rgb(204_63_28/45%)]">
            <Volume2 aria-hidden className="size-11" />
          </span>
        </span>
        <DialogTitle className="text-[28px] leading-[34px] font-bold tracking-tight">{t.title}</DialogTitle>
        <DialogDescription className="text-sm text-muted-foreground">{t.text}</DialogDescription>
        <Button className="h-13 w-full text-[15px]" onClick={() => void enableSound()}>
          {t.start}
        </Button>
        <Button variant="ghost" className="h-11 w-full text-muted-foreground" onClick={later}>
          {t.later}
        </Button>
        <span className="text-[13px] text-muted-foreground/80">{t.hint}</span>
      </DialogContent>
    </Dialog>
  );
}
