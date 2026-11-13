"use client";

import { Volume2, VolumeX } from "lucide-react";
import { disableSound, enableSound } from "@/features/calls/alert-sound";
import { useSoundOn } from "@/features/calls/use-live-calls";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { TONE } from "./live-ui";

const tl = messages.panel.live;

/**
 * "Som ligado" (verde) ou "Som desligado": a tag do topo (Ao vivo e cozinha), que também liga e
 * desliga. Ligar precisa ser num toque: é a regra do navegador para tocar som.
 */
export function SoundTag() {
  const soundOn = useSoundOn();
  const Icon = soundOn ? Volume2 : VolumeX;
  return (
    <button
      type="button"
      aria-pressed={soundOn}
      aria-label={soundOn ? tl.soundOnLabel : tl.soundOffLabel}
      onClick={() => void (soundOn ? disableSound() : enableSound())}
      className={cn(
        "inline-flex h-9 items-center gap-1.5 rounded-full px-3 text-[13px] font-semibold focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
        soundOn ? TONE.ok : TONE.neutral,
      )}
    >
      <Icon aria-hidden className="size-4" />
      {soundOn ? tl.soundOn : tl.soundOff}
    </button>
  );
}
