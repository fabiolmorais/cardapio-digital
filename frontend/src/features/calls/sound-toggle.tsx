"use client";

import { Volume2, VolumeX } from "lucide-react";
import { Button } from "@/components/ui/button";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { disableSound, enableSound } from "./alert-sound";
import { useSoundOn } from "./use-live-calls";

const t = messages.panel.calls;

/**
 * Liga e desliga o alerta sonoro (chamados, cozinha, Modo TV). Ligar precisa ser num toque: é a
 * regra do navegador para tocar som. O botão diz o que vai acontecer ao tocar.
 */
export function SoundToggle({ className, size }: { className?: string; size?: "default" | "lg" }) {
  const soundOn = useSoundOn();
  const Icon = soundOn ? VolumeX : Volume2;
  return (
    <Button
      variant="outline"
      size={size}
      className={cn(className)}
      aria-pressed={soundOn}
      data-sound-toggle
      onClick={() => void (soundOn ? disableSound() : enableSound())}
    >
      <Icon aria-hidden />
      {soundOn ? t.disableSound : t.enableSound}
    </Button>
  );
}
