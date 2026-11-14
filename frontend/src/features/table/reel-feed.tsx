"use client";

import { Pause } from "lucide-react";
import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import type { Schemas } from "@/lib/api/client";
import { formatCents } from "@/lib/format";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";

const t = tablePage.reels;

/**
 * Quantas telas ao redor da atual têm o vídeo montado de verdade. As outras mostram só a capa:
 * cem vídeos na página seriam cem downloads começando no 4G.
 */
const WINDOW_BEFORE = 1;
const WINDOW_AFTER = 2;
/** Quanto tempo uma foto fica na tela antes de passar para a próxima mídia. */
const PHOTO_SECONDS = 5;

export type ReelMedia = Schemas["PublicMenuMedia"];

/**
 * Uma tela do Reels: um prato do cardápio ou uma opção (o sabor da pizza; complemento da ADR 0010).
 *
 * @param price o preço embaixo do nome; null quando não há o que mostrar (a rodada do rodízio)
 */
export type ReelEntry = {
  id: string;
  name: string;
  description?: string;
  media: ReelMedia[];
  badges: { label: string; dark?: boolean }[];
  price: { cents: number; from?: boolean; strikeCents?: number | null } | null;
};

/**
 * O Reels (desenho Reels): uma tela por prato (ou opção), deslizando para cima; as mídias de cada
 * um, deslizando para o lado ("1 de 3"). O vídeo da tela atual toca sozinho (sem som, até o cliente
 * ligar); tocar pausa. O primeiro vídeo da próxima tela já vai carregando, para não ficar preto ao
 * deslizar. Depois da última tela vem a primeira de novo: a rolagem parece não ter fim.
 *
 * @param action o botão embaixo do preço de cada tela ("Adicionar", "Escolher")
 * @param swipeHint o "deslize para cima" das telas (some na última, quando não há volta)
 */
export function ReelFeed({
  entries,
  startIndex,
  muted,
  action,
  swipeHint,
}: {
  entries: ReelEntry[];
  startIndex: number;
  muted: boolean;
  action: (entry: ReelEntry) => ReactNode;
  swipeHint: string;
}) {
  const [active, setActive] = useState(startIndex);
  const sections = useRef<(HTMLElement | null)[]>([]);
  const scroller = useRef<HTMLDivElement>(null);
  // Rolagem infinita: depois da última tela vem uma cópia da primeira. Ao chegar nela, a tela
  // salta (sem animação) para a primeira de verdade.
  const loops = entries.length > 1;

  // Abre na tela que o cliente tocou.
  useEffect(() => {
    sections.current[startIndex]?.scrollIntoView({ behavior: "instant", block: "start" });
  }, [startIndex]);

  // A tela atual é a que ocupa a maior parte dela.
  useEffect(() => {
    const observer = new IntersectionObserver(
      (observed) => {
        for (const entry of observed) {
          if (!entry.isIntersecting) continue;
          const index = Number((entry.target as HTMLElement).dataset.index);
          if (index === entries.length) {
            // A cópia da primeira: volta ao começo de verdade, no mesmo quadro.
            scroller.current?.scrollTo({ top: 0, behavior: "instant" });
            setActive(0);
          } else {
            setActive(index);
          }
        }
      },
      { threshold: 0.6 },
    );
    sections.current.forEach((section) => section && observer.observe(section));
    return () => observer.disconnect();
  }, [entries.length]);

  return (
    <div ref={scroller} className="h-dvh snap-y snap-mandatory overflow-y-auto overscroll-none [scrollbar-width:none]">
      {entries.map((entry, index) => (
        <section
          key={entry.id}
          ref={(el) => {
            sections.current[index] = el;
          }}
          data-index={index}
          aria-label={entry.name}
          className="relative h-dvh snap-start snap-always overflow-hidden"
        >
          <Reel
            entry={entry}
            active={index === active}
            mounted={
              (index >= active - WINDOW_BEFORE && index <= active + WINDOW_AFTER) ||
              // Na última, a primeira já fica pronta: é para ela que a rolagem volta.
              (loops && index === 0 && active >= entries.length - 1 - WINDOW_AFTER)
            }
            preloadFirst={index === active + 1}
            muted={muted}
            action={action(entry)}
            hint={!loops && index === entries.length - 1 ? null : swipeHint}
          />
        </section>
      ))}
      {loops && (
        // A cópia da primeira tela, só para a passagem da última para ela parecer contínua: o
        // leitor de tela não a vê, e nada nela é clicável (a tela salta para a original).
        <section
          ref={(el) => {
            sections.current[entries.length] = el;
          }}
          data-index={entries.length}
          aria-hidden
          inert
          className="relative h-dvh snap-start snap-always overflow-hidden"
        >
          <Reel
            entry={entries[0]}
            active={false}
            mounted={active >= entries.length - 1 - WINDOW_AFTER}
            preloadFirst={active === entries.length - 1}
            muted={muted}
            action={action(entries[0])}
            hint={swipeHint}
          />
        </section>
      )}
    </div>
  );
}

function Reel({
  entry,
  active,
  mounted,
  preloadFirst,
  muted,
  action,
  hint,
}: {
  entry: ReelEntry;
  active: boolean;
  mounted: boolean;
  preloadFirst: boolean;
  muted: boolean;
  action: ReactNode;
  hint: string | null;
}) {
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const [expanded, setExpanded] = useState(false);
  /** Quanto da mídia à vista já passou (0 a 1): enche a barra do topo, como no Instagram. */
  const [progress, setProgress] = useState(0);
  const strip = useRef<HTMLDivElement>(null);
  const videos = useRef<(HTMLVideoElement | null)[]>([]);
  const media = entry.media;
  const several = media.length > 1;

  // Só o vídeo da tela atual, na mídia que está à vista, toca. Os outros param e voltam ao começo.
  useEffect(() => {
    videos.current.forEach((video, i) => {
      if (!video) return;
      if (active && i === index) {
        if (paused) video.pause();
        else
          void video.play().catch(() => {
            // O navegador recusou tocar sozinho (economia de dados, por exemplo): fica parado.
          });
      } else {
        video.pause();
        video.currentTime = 0;
      }
    });
  }, [active, index, paused, mounted]);

  /** Mostra a mídia `target` (rolando a faixa: o índice segue a rolagem). */
  function show(target: number) {
    const el = strip.current;
    if (!el) return;
    el.scrollTo({ left: target * el.clientWidth, behavior: "smooth" });
  }
  /** A próxima mídia (da última, volta à primeira). */
  function next() {
    show((index + 1) % media.length);
  }
  /** A anterior; na primeira, recomeça ela. */
  function previous() {
    if (index === 0) {
      setProgress(0);
      const video = videos.current[0];
      if (video) video.currentTime = 0;
    } else show(index - 1);
  }

  /**
   * Uma função de ref por vídeo, a mesma a cada renderização: uma função nova faria o React soltar
   * e prender o vídeo de novo, e a pausa da saída (abaixo) pararia o vídeo a cada atualização da
   * barra de progresso.
   */
  const videoRefs = useMemo(
    () =>
      media.map((_, i) => (el: HTMLVideoElement | null) => {
        videos.current[i] = el;
        // Ao sair da tela (fechar o Reels), o vídeo para: a página fica guardada escondida
        // para voltar rápido, e o som seguiria tocando pelo cardápio.
        return () => {
          el?.pause();
          if (videos.current[i] === el) videos.current[i] = null;
        };
      }),
    [media],
  );

  // A foto à vista enche a barra em PHOTO_SECONDS e passa para a próxima mídia.
  const photoShown = active && several && !paused && !media[index]?.video;
  useEffect(() => {
    if (!photoShown) return;
    const step = 100;
    const timer = setInterval(() => setProgress((p) => Math.min(1, p + step / 1000 / PHOTO_SECONDS)), step);
    return () => clearInterval(timer);
  }, [photoShown, index]);
  useEffect(() => {
    if (photoShown && progress >= 1) next();
    // next() lê o índice da renderização atual, de propósito.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [photoShown, progress]);

  // Outra mídia à vista: a barra dela começa do zero.
  const [shownIndex, setShownIndex] = useState(index);
  if (shownIndex !== index) {
    setShownIndex(index);
    setProgress(0);
  }

  // Saiu da tela: volta para a primeira mídia e despausa, para quem voltar ver do começo. (O
  // estado se ajusta já na renderização, como o React recomenda; o efeito só rola a faixa.)
  const [wasActive, setWasActive] = useState(active);
  if (wasActive !== active) {
    setWasActive(active);
    if (!active) {
      setPaused(false);
      setIndex(0);
      setProgress(0);
    }
  }
  useEffect(() => {
    if (!active) strip.current?.scrollTo({ left: 0 });
  }, [active]);

  function onScroll() {
    const el = strip.current;
    if (!el) return;
    setIndex(Math.round(el.scrollLeft / el.clientWidth));
  }

  const current = media[index];

  return (
    <>
      <div
        ref={strip}
        onScroll={onScroll}
        className="flex h-full snap-x snap-mandatory overflow-x-auto overscroll-x-contain [scrollbar-width:none]"
      >
        {media.map((m, i) => (
          <div key={i} className="relative h-full w-full shrink-0 snap-center">
            {m.video ? (
              mounted ? (
                <video
                  ref={videoRefs[i]}
                  src={m.video.url}
                  poster={m.video.poster ?? undefined}
                  // O vídeo à vista carrega; o primeiro da próxima tela também, para não ficar preto.
                  preload={(active && i === index) || (preloadFirst && i === 0) ? "auto" : "none"}
                  muted={muted}
                  // Com uma mídia só, o vídeo repete; com várias, ao acabar passa para a próxima.
                  loop={!several}
                  playsInline
                  onTimeUpdate={(event) => {
                    const video = event.currentTarget;
                    if (active && i === index && video.duration) setProgress(video.currentTime / video.duration);
                  }}
                  onEnded={() => {
                    if (active && i === index && several) next();
                  }}
                  onClick={() => setPaused((p) => !p)}
                  aria-label={paused ? t.play : t.pause}
                  className="size-full object-cover"
                />
              ) : (
                m.video.poster && (
                  // eslint-disable-next-line @next/next/no-img-element -- capa do vídeo, vinda do serviço de vídeo
                  <img src={m.video.poster} alt="" className="size-full object-cover" />
                )
              )
            ) : (
              m.image && (
                // eslint-disable-next-line @next/next/no-img-element -- foto grande, vinda do R2
                <img
                  src={m.image.large}
                  alt=""
                  loading={active || preloadFirst ? "eager" : "lazy"}
                  decoding="async"
                  // Com várias mídias, tocar na foto pausa a passagem, como no vídeo.
                  onClick={several ? () => setPaused((p) => !p) : undefined}
                  className="size-full object-cover"
                />
              )
            )}
          </div>
        ))}
      </div>

      {/* O degradê que deixa o texto legível sobre qualquer vídeo. */}
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 bg-[linear-gradient(180deg,rgba(0,0,0,.55)_0%,rgba(0,0,0,0)_22%,rgba(0,0,0,0)_48%,rgba(0,0,0,.88)_100%)]"
      />

      {/* Como no Instagram: tocar perto da borda direita passa para a próxima mídia; da esquerda, volta. */}
      {several && active && (
        <>
          <button
            type="button"
            aria-label={t.previous}
            onClick={previous}
            className="absolute top-20 bottom-72 left-0 w-1/4 focus-visible:ring-3 focus-visible:ring-white focus-visible:outline-none focus-visible:ring-inset"
          />
          <button
            type="button"
            aria-label={t.next}
            onClick={next}
            className="absolute top-20 right-0 bottom-72 w-1/4 focus-visible:ring-3 focus-visible:ring-white focus-visible:outline-none focus-visible:ring-inset"
          />
        </>
      )}

      {(several || current?.video) && (
        <div className="pointer-events-none absolute inset-x-4 top-4 flex gap-1" aria-hidden>
          {media.map((_, i) => (
            <span key={i} className="h-[3px] flex-1 overflow-hidden rounded-full bg-white/35">
              <span
                className="block h-full rounded-full bg-white transition-[width] duration-200 ease-linear"
                style={{ width: `${i < index ? 100 : i === index ? Math.round(progress * 100) : 0}%` }}
              />
            </span>
          ))}
        </div>
      )}
      {media.length > 1 && <span className="sr-only">{t.mediaOf(index + 1, media.length)}</span>}

      {(current?.video || several) && paused && active && (
        <span className="pointer-events-none absolute inset-0 flex items-center justify-center">
          <span className="flex size-18 items-center justify-center rounded-full bg-black/45">
            <Pause aria-hidden className="size-8" />
            <span className="sr-only">{t.paused}</span>
          </span>
        </span>
      )}

      <div className="absolute inset-x-4 bottom-7 flex flex-col gap-3.5">
        {entry.badges.length > 0 && (
          <div className="flex gap-2">
            {entry.badges.map((badge) => (
              <span
                key={badge.label}
                className={cn(
                  "rounded-full px-3 py-1 text-xs font-semibold",
                  badge.dark ? "bg-black/50" : "bg-white/15",
                )}
              >
                {badge.label}
              </span>
            ))}
          </div>
        )}
        <h2 className="text-3xl font-bold tracking-tight drop-shadow">{entry.name}</h2>
        {entry.description && (
          <p className="max-w-xs text-white/85">
            <span className={cn(!expanded && "line-clamp-2")}>{entry.description}</span>
            {entry.description.length > 90 && (
              <button
                type="button"
                onClick={() => setExpanded((e) => !e)}
                className="ml-1 rounded font-semibold text-white underline-offset-2 hover:underline focus-visible:ring-3 focus-visible:ring-white focus-visible:outline-none"
              >
                {expanded ? t.readLess : t.readMore}
              </button>
            )}
          </p>
        )}
        <div className="flex min-h-16 items-center gap-3 rounded-[22px] border border-white/15 bg-black/35 py-2 pr-2 pl-4.5 backdrop-blur-xl">
          <span className="flex flex-1 flex-col">
            {entry.price &&
              (entry.price.from ? (
                <span className="text-xs text-white/70">{tablePage.choices.from}</span>
              ) : (
                entry.price.strikeCents != null && (
                  <s className="text-xs text-white/70">{formatCents(entry.price.strikeCents)}</s>
                )
              ))}
            {entry.price && <span className="text-xl font-bold">{formatCents(entry.price.cents)}</span>}
          </span>
          {action}
        </div>
        {hint && <span className="text-center text-xs text-white/70">{hint}</span>}
      </div>
    </>
  );
}
