"use client";

import { Pause, Plus, ShoppingBag, Volume2, VolumeX, X } from "lucide-react";
import Link from "next/link";
import { BrandLogo } from "@/components/brand-logo";
import { useEffect, useMemo, useRef, useState } from "react";
import { formatCents } from "@/lib/format";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";
import { useCart } from "./cart";
import { aLaCarte } from "./carte";
import { currentPriceCents, ProductSheet } from "./product-sheet";
import type { MenuItem, PublicMenu } from "./table-data";
import { listOrders } from "./table-orders";

const t = tablePage.reels;

/**
 * Quantos pratos ao redor do atual têm o vídeo montado de verdade. Os outros mostram só a capa:
 * cem vídeos na página seriam cem downloads começando no 4G.
 */
const WINDOW_BEFORE = 1;
const WINDOW_AFTER = 2;
/** Quanto tempo uma foto fica na tela antes de passar para a próxima mídia do prato. */
const PHOTO_SECONDS = 5;

/**
 * O Reels do cardápio (desenho Reels): os pratos com foto ou vídeo, um por tela, deslizando
 * para cima; as mídias de cada prato, deslizando para o lado ("1 de 3"). O vídeo do prato na
 * tela toca sozinho (sem som, até o cliente ligar); tocar pausa. O primeiro vídeo do próximo
 * prato já vai carregando, para não ficar tela preta ao deslizar.
 */
export function Reels({
  token,
  menu,
  startId,
  establishmentName,
  logo,
  brandColor,
  backHref,
}: {
  token: string;
  menu: PublicMenu;
  startId: string | null;
  establishmentName: string;
  /** A logo pequena do restaurante; sem ela, as iniciais na cor da marca. */
  logo?: string;
  brandColor: string;
  /** Para onde o "Fechar" volta: o cardápio ou, vindo de um destaque, o início da mesa. */
  backHref: string;
}) {
  // O Reels é do cardápio de sempre: o que é só do rodízio não tem preço para mostrar nem para adicionar.
  const items = aLaCarte(menu)
    .flatMap((c) => c.products)
    .filter((p) => p.media.length > 0);
  const startIndex = Math.max(
    0,
    items.findIndex((p) => p.id === startId),
  );
  const [active, setActive] = useState(startIndex);
  const [muted, setMuted] = useState(true);
  const [ordering, setOrdering] = useState(true);
  const [adding, setAdding] = useState<MenuItem | null>(null);
  const { count, totalCents } = useCart(token);
  const sections = useRef<(HTMLElement | null)[]>([]);
  const scroller = useRef<HTMLDivElement>(null);
  // Rolagem infinita: depois do último prato vem uma cópia do primeiro. Ao chegar nela, a tela
  // salta (sem animação) para o primeiro de verdade, e a rolagem parece não ter fim.
  const loops = items.length > 1;

  // Abre no prato que o cliente tocou no cardápio.
  useEffect(() => {
    sections.current[startIndex]?.scrollIntoView({ behavior: "instant", block: "start" });
  }, [startIndex]);

  // O prato na tela é o que ocupa a maior parte dela.
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue;
          const index = Number((entry.target as HTMLElement).dataset.index);
          if (index === items.length) {
            // A cópia do primeiro: volta ao começo de verdade, no mesmo quadro.
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
  }, [items.length]);

  // O restaurante desligou o pedido pelo celular? O "Adicionar" some (o Reels continua).
  useEffect(() => {
    let cancelled = false;
    listOrders(token)
      .then((orders) => {
        if (!cancelled) setOrdering(orders.orderingEnabled);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [token]);

  const back = backHref;
  const round =
    "flex size-11 items-center justify-center rounded-full border border-white/20 bg-black/40 text-white backdrop-blur-md focus-visible:ring-3 focus-visible:ring-white focus-visible:outline-none";

  return (
    <div className="fixed inset-0 bg-black text-white">
      <div
        ref={scroller}
        className="h-dvh snap-y snap-mandatory overflow-y-auto overscroll-none [scrollbar-width:none]"
      >
        {items.length === 0 && <p className="p-6 text-white/80">{t.empty}</p>}
        {items.map((item, index) => (
          <section
            key={item.id}
            ref={(el) => {
              sections.current[index] = el;
            }}
            data-index={index}
            aria-label={item.name}
            className="relative h-dvh snap-start snap-always overflow-hidden"
          >
            <ProductReel
              item={item}
              active={index === active}
              mounted={
                (index >= active - WINDOW_BEFORE && index <= active + WINDOW_AFTER) ||
                // No último, o primeiro já fica pronto: é para ele que a rolagem volta.
                (loops && index === 0 && active >= items.length - 1 - WINDOW_AFTER)
              }
              preloadFirst={index === active + 1}
              muted={muted}
              canAdd={ordering && item.available}
              onAdd={() => setAdding(item)}
              isLast={!loops && index === items.length - 1}
            />
          </section>
        ))}
        {loops && (
          // A cópia do primeiro prato, só para a passagem do último para ele parecer contínua: o
          // leitor de tela não a vê, e nada nela é clicável (a tela salta para o original).
          <section
            ref={(el) => {
              sections.current[items.length] = el;
            }}
            data-index={items.length}
            aria-hidden
            inert
            className="relative h-dvh snap-start snap-always overflow-hidden"
          >
            <ProductReel
              item={items[0]}
              active={false}
              mounted={active >= items.length - 1 - WINDOW_AFTER}
              preloadFirst={active === items.length - 1}
              muted={muted}
              canAdd={ordering && items[0].available}
              onAdd={() => {}}
              isLast={false}
            />
          </section>
        )}
      </div>

      {/* Por cima de tudo: a marca, o fechar, o som e o pedido. */}
      <div className="pointer-events-none fixed inset-x-0 top-0 flex items-center justify-between gap-3 px-4 pt-9">
        <span className="flex min-w-0 items-center gap-2.5">
          <BrandLogo logo={logo} name={establishmentName} color={brandColor} alt="" className="size-8 text-xs" />
          <span className="truncate text-sm font-semibold drop-shadow">{establishmentName}</span>
        </span>
        <Link href={back} aria-label={t.close} className={cn(round, "pointer-events-auto")}>
          <X aria-hidden className="size-5" />
        </Link>
      </div>
      <div className="fixed right-3.5 bottom-64 flex flex-col items-center gap-1 text-[11px] font-semibold">
        <button
          type="button"
          aria-label={muted ? t.soundOn : t.soundOff}
          aria-pressed={!muted}
          onClick={() => setMuted((m) => !m)}
          className={round}
        >
          {muted ? <VolumeX aria-hidden className="size-5" /> : <Volume2 aria-hidden className="size-5" />}
        </button>
        <span aria-hidden>{t.sound}</span>
      </div>
      {count > 0 && (
        <Link
          href={`/m/${token}/carrinho`}
          className="fixed top-22 right-4 flex h-10 items-center gap-2 rounded-full bg-(--brand) px-3.5 text-sm font-semibold text-white shadow-lg focus-visible:ring-3 focus-visible:ring-white focus-visible:outline-none"
        >
          <ShoppingBag aria-hidden className="size-4" />
          {t.seeCart} · {formatCents(totalCents)}
        </Link>
      )}

      {/* O "Adicionar" abre o detalhe do prato (3.2); tocar na foto dele volta ao vídeo. */}
      <ProductSheet token={token} item={adding} onClose={() => setAdding(null)} onPhoto={() => {}} />
    </div>
  );
}

function ProductReel({
  item,
  active,
  mounted,
  preloadFirst,
  muted,
  canAdd,
  onAdd,
  isLast,
}: {
  item: MenuItem;
  active: boolean;
  mounted: boolean;
  preloadFirst: boolean;
  muted: boolean;
  canAdd: boolean;
  onAdd: () => void;
  isLast: boolean;
}) {
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const [expanded, setExpanded] = useState(false);
  /** Quanto da mídia à vista já passou (0 a 1): enche a barra do topo, como no Instagram. */
  const [progress, setProgress] = useState(0);
  const strip = useRef<HTMLDivElement>(null);
  const videos = useRef<(HTMLVideoElement | null)[]>([]);
  const media = item.media;
  const several = media.length > 1;

  // Só o vídeo do prato na tela, na mídia que está à vista, toca. Os outros param e voltam ao começo.
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

  /** Mostra a mídia `target` do prato (rolando a faixa: o índice segue a rolagem). */
  function show(target: number) {
    const el = strip.current;
    if (!el) return;
    el.scrollTo({ left: target * el.clientWidth, behavior: "smooth" });
  }
  /** A próxima mídia do prato (da última, volta à primeira). */
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

  const price = currentPriceCents(item);
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
                  // O vídeo à vista carrega; o primeiro do próximo prato também, para não ficar tela preta.
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
        {(item.featured || !item.available) && (
          <div className="flex gap-2">
            {item.featured && (
              <span className="rounded-full bg-white/15 px-3 py-1 text-xs font-semibold">{t.featured}</span>
            )}
            {!item.available && (
              <span className="rounded-full bg-black/50 px-3 py-1 text-xs font-semibold">{t.soldOut}</span>
            )}
          </div>
        )}
        <h2 className="text-3xl font-bold tracking-tight drop-shadow">{item.name}</h2>
        {item.description && (
          <p className="max-w-xs text-white/85">
            <span className={cn(!expanded && "line-clamp-2")}>{item.description}</span>
            {item.description.length > 90 && (
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
        <div className="flex items-center gap-3 rounded-[22px] border border-white/15 bg-black/35 py-2 pr-2 pl-4.5 backdrop-blur-xl">
          <span className="flex flex-1 flex-col">
            {item.promoPriceCents != null && <s className="text-xs text-white/70">{formatCents(item.priceCents)}</s>}
            <span className="text-xl font-bold">{formatCents(price)}</span>
          </span>
          {canAdd && (
            <button
              type="button"
              onClick={onAdd}
              aria-label={`${t.add} ${item.name}`}
              className="flex h-12 items-center gap-1.5 rounded-2xl bg-(--brand) px-5 font-semibold text-white focus-visible:ring-3 focus-visible:ring-white focus-visible:outline-none"
            >
              <Plus aria-hidden className="size-4" />
              {t.add}
            </button>
          )}
        </div>
        {!isLast && <span className="text-center text-xs text-white/70">{t.swipeHint}</span>}
      </div>
    </>
  );
}
