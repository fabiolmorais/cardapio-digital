"use client";

import { Check, Volume2, VolumeX, X } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";
import type { OptionGroup } from "./choices";
import { ReelFeed, type ReelEntry } from "./reel-feed";

const t = tablePage.choices;
const tr = tablePage.reels;

type Option = OptionGroup["items"][number];

/**
 * O Reels das opções de um grupo (os sabores da pizza; complemento da ADR 0010): as opções com
 * foto ou vídeo, uma por tela, como o Reels do cardápio. Embaixo, o preço naquele prato e o
 * "Escolher", que marca a opção (com as mesmas regras da lista) e continua no Reels.
 * Carregado só quando o cliente toca numa foto: o cardápio abre sem este código.
 */
export default function OptionReels({
  group,
  startId,
  priceOf,
  isPicked,
  canPick,
  onToggle,
  onClose,
}: {
  group: OptionGroup;
  startId: string;
  /** O preço da opção neste prato; null quando não mostra (a rodada do rodízio, ou sem preço). */
  priceOf: (option: Option) => number | null;
  isPicked: (option: Option) => boolean;
  canPick: (option: Option) => boolean;
  onToggle: (option: Option) => void;
  onClose: () => void;
}) {
  const [muted, setMuted] = useState(true);
  const close = useRef<HTMLButtonElement>(null);
  const options = useMemo(() => group.items.filter((o) => o.media.length > 0), [group]);
  const byId = new Map(options.map((o) => [o.id, o]));
  const entries: ReelEntry[] = options.map((o) => {
    const price = priceOf(o);
    return {
      id: o.id,
      name: o.name,
      description: o.description,
      media: o.media,
      badges: o.available ? [] : [{ label: t.soldOut, dark: true }],
      price: price === null ? null : { cents: price },
    };
  });

  // O foco entra no Reels (o leitor de tela anuncia o "Voltar"), e o Esc fecha só ele.
  useEffect(() => {
    close.current?.focus();
    function onKey(event: KeyboardEvent) {
      if (event.key !== "Escape") return;
      event.preventDefault();
      event.stopPropagation();
      onClose();
    }
    window.addEventListener("keydown", onKey, true);
    return () => window.removeEventListener("keydown", onKey, true);
  }, [onClose]);

  const round =
    "flex size-11 items-center justify-center rounded-full border border-white/20 bg-black/40 text-white backdrop-blur-md focus-visible:ring-3 focus-visible:ring-white focus-visible:outline-none";

  return (
    <div role="region" aria-label={t.reelsOf(group.title)} className="fixed inset-0 z-50 bg-black text-white">
      {/* Num <dialog> da mesa, o "fixed" cobre o próprio diálogo, que ocupa a tela toda. */}
      <ReelFeed
        entries={entries}
        startIndex={Math.max(
          0,
          options.findIndex((o) => o.id === startId),
        )}
        muted={muted}
        swipeHint={t.swipeHint}
        action={(entry) => {
          const option = byId.get(entry.id)!;
          const picked = isPicked(option);
          return (
            <button
              type="button"
              aria-pressed={picked}
              aria-label={picked ? t.unchooseLabel(option.name) : t.chooseLabel(option.name)}
              disabled={!picked && !canPick(option)}
              onClick={() => onToggle(option)}
              className={cn(
                "flex h-12 items-center gap-1.5 rounded-2xl px-5 font-semibold focus-visible:ring-3 focus-visible:ring-white focus-visible:outline-none disabled:opacity-50",
                picked ? "bg-white text-black" : "bg-(--brand) text-white",
              )}
            >
              {picked && <Check aria-hidden className="size-4" />}
              {picked ? t.chosen : t.choose}
            </button>
          );
        }}
      />
      <div className="pointer-events-none fixed inset-x-0 top-0 flex items-center justify-between gap-3 px-4 pt-9">
        <span className="truncate text-sm font-semibold drop-shadow">{group.title}</span>
        <button
          ref={close}
          type="button"
          onClick={onClose}
          aria-label={t.backToChoice}
          className={cn(round, "pointer-events-auto")}
        >
          <X aria-hidden className="size-5" />
        </button>
      </div>
      <div className="fixed right-3.5 bottom-64 flex flex-col items-center gap-1 text-[11px] font-semibold">
        <button
          type="button"
          aria-label={muted ? tr.soundOn : tr.soundOff}
          aria-pressed={!muted}
          onClick={() => setMuted((m) => !m)}
          className={round}
        >
          {muted ? <VolumeX aria-hidden className="size-5" /> : <Volume2 aria-hidden className="size-5" />}
        </button>
        <span aria-hidden>{tr.sound}</span>
      </div>
    </div>
  );
}
