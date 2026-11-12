import { Play, Users, UtensilsCrossed } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";
import { formatCents } from "@/lib/format";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";
import { discountPercent } from "./discount";
import type { MenuItem } from "./table-data";

const t = tablePage.menu;

/**
 * Um produto na lista do cardápio: a foto, o nome, a descrição e o preço (com o "de/por" na
 * promoção). Esgotado aparece em cinza, com "Esgotado hoje". Sem hooks: serve tanto no servidor
 * quanto dentro do navegador do cardápio (componente do cliente), que põe o "Adicionar" em `action`.
 * Com `reelsHref`, a foto abre o Reels do prato (e um selo ▶ avisa quando há vídeo).
 */
export function MenuItemCard({
  item,
  action,
  reelsHref,
  onOpen,
  price,
  selected = false,
}: {
  item: MenuItem;
  action?: ReactNode;
  reelsHref?: string;
  /** Sem vídeo: tocar na foto ou no nome abre o detalhe do prato (desenho 3.2). */
  onOpen?: () => void;
  /** No lugar do preço (a aba Rodízio não mostra preço). */
  price?: ReactNode;
  /** Escolhido na rodada do rodízio: o cartão fica na cor da marca (desenho 6.2). */
  selected?: boolean;
}) {
  const hasVideo = item.media.some((m) => m.type === "VIDEO");
  const off = discountPercent(item);
  return (
    <article
      id={`produto-${item.id}`}
      aria-disabled={!item.available || undefined}
      className={cn(
        "flex scroll-mt-24 gap-3.5 rounded-[22px] border border-(--mesa-stroke) bg-(--mesa-glass) p-2.5 transition-colors",
        selected && "border-(--brand-text)/60 bg-(--brand)/15",
      )}
    >
      {reelsHref ? (
        <Link
          href={reelsHref}
          prefetch={false}
          aria-label={t.openReels(item.name)}
          className="relative flex shrink-0 rounded-2xl focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
        >
          <span
            className={cn(
              "size-24 shrink-0 overflow-hidden rounded-2xl bg-(--mesa-stroke)",
              !item.available && "opacity-45 grayscale",
            )}
          >
            {item.photo ? (
              // eslint-disable-next-line @next/next/no-img-element -- miniatura já no tamanho certo, vinda do R2
              <img
                src={item.photo.thumb}
                alt=""
                width={96}
                height={96}
                loading="lazy"
                decoding="async"
                className="size-full object-cover"
              />
            ) : (
              <span className="flex size-full items-center justify-center text-(--mesa-muted)">
                <UtensilsCrossed aria-hidden className="size-6" />
                <span className="sr-only">{t.noPhoto}</span>
              </span>
            )}
          </span>
          {hasVideo && (
            <span className="absolute right-1.5 bottom-1.5 flex size-7 items-center justify-center rounded-full bg-black/60 text-white">
              <Play aria-hidden className="size-3.5 fill-current" />
            </span>
          )}
        </Link>
      ) : onOpen ? (
        <button
          type="button"
          onClick={onOpen}
          aria-label={t.openDetail(item.name)}
          className="flex shrink-0 rounded-2xl focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
        >
          <span
            className={cn(
              "size-24 shrink-0 overflow-hidden rounded-2xl bg-(--mesa-stroke)",
              !item.available && "opacity-45 grayscale",
            )}
          >
            {item.photo ? (
              // eslint-disable-next-line @next/next/no-img-element -- miniatura já no tamanho certo, vinda do R2
              <img
                src={item.photo.thumb}
                alt=""
                width={96}
                height={96}
                loading="lazy"
                decoding="async"
                className="size-full object-cover"
              />
            ) : (
              <span className="flex size-full items-center justify-center text-(--mesa-muted)">
                <UtensilsCrossed aria-hidden className="size-6" />
                <span className="sr-only">{t.noPhoto}</span>
              </span>
            )}
          </span>
        </button>
      ) : (
        <span
          className={cn(
            "size-24 shrink-0 overflow-hidden rounded-2xl bg-(--mesa-stroke)",
            !item.available && "opacity-45 grayscale",
          )}
        >
          {item.photo ? (
            // eslint-disable-next-line @next/next/no-img-element -- miniatura já no tamanho certo, vinda do R2
            <img
              src={item.photo.thumb}
              alt=""
              width={96}
              height={96}
              loading="lazy"
              decoding="async"
              className="size-full object-cover"
            />
          ) : (
            <span className="flex size-full items-center justify-center text-(--mesa-muted)">
              <UtensilsCrossed aria-hidden className="size-6" />
              <span className="sr-only">{t.noPhoto}</span>
            </span>
          )}
        </span>
      )}
      <div className="flex min-w-0 flex-1 flex-col gap-1 pt-0.5">
        <span className="flex items-center gap-2">
          <h3 className={cn("font-semibold", !item.available && "text-(--mesa-muted)")}>{item.name}</h3>
          {item.available && off !== null && price === undefined && (
            <span className="rounded-md bg-(--brand) px-1.5 py-0.5 text-[11px] font-bold text-white">{t.off(off)}</span>
          )}
        </span>
        {item.description && <p className="line-clamp-2 text-sm text-(--mesa-muted)">{item.description}</p>}
        {item.serves != null && (
          <span className="flex items-center gap-1 text-xs font-medium text-(--mesa-info)">
            <Users aria-hidden className="size-3.5" />
            {t.serves(item.serves)}
          </span>
        )}
        <span className="mt-auto flex flex-wrap items-end justify-between gap-2 pt-1">
          {item.available && price !== undefined ? (
            price
          ) : !item.available ? (
            <span className="rounded-full bg-(--mesa-danger-soft) px-2.5 py-0.5 text-xs font-semibold text-(--mesa-danger)">
              {t.soldOut}
            </span>
          ) : item.promoPriceCents != null ? (
            // O "de" em cima, pequeno, e o "por" embaixo: cabe ao lado do "Adicionar".
            <span className="flex flex-col">
              <s className="text-xs text-(--mesa-muted)">
                <span className="sr-only">{t.normalPrice} </span>
                {formatCents(item.priceCents)}
              </s>
              <span className="font-bold text-(--brand-text)">
                <span className="sr-only">{t.promoPrice} </span>
                {formatCents(item.promoPriceCents)}
              </span>
            </span>
          ) : (
            <span className="font-bold">{formatCents(item.priceCents)}</span>
          )}
          {item.available && action}
        </span>
      </div>
    </article>
  );
}
