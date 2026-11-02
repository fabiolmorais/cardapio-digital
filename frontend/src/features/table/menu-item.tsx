import { UtensilsCrossed } from "lucide-react";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { discountPercent, type MenuItem } from "./table-data";

const t = messages.tablePage.menu;

/**
 * Um produto na lista do cardápio: a foto, o nome, a descrição e o preço (com o "de/por" na
 * promoção). Esgotado aparece em cinza, com "Esgotado hoje". Sem hooks: serve tanto no servidor
 * quanto dentro do navegador do cardápio (componente do cliente).
 */
export function MenuItemCard({ item }: { item: MenuItem }) {
  const off = discountPercent(item);
  return (
    <article
      id={`produto-${item.id}`}
      aria-disabled={!item.available || undefined}
      className="flex scroll-mt-24 gap-3.5 rounded-[22px] border border-(--mesa-stroke) bg-(--mesa-glass) p-2.5"
    >
      <span className={cn("size-24 shrink-0 overflow-hidden rounded-2xl bg-(--mesa-stroke)", !item.available && "opacity-45 grayscale")}>
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
      <div className="flex min-w-0 flex-1 flex-col gap-1 pt-0.5">
        <span className="flex items-center gap-2">
          <h3 className={cn("font-semibold", !item.available && "text-(--mesa-muted)")}>{item.name}</h3>
          {item.available && off !== null && (
            <span className="rounded-md bg-(--brand) px-1.5 py-0.5 text-[11px] font-bold text-white">{t.off(off)}</span>
          )}
        </span>
        {item.description && <p className="line-clamp-2 text-sm text-(--mesa-muted)">{item.description}</p>}
        <span className="mt-auto pt-1">
          {!item.available ? (
            <span className="rounded-md border border-(--mesa-stroke) px-2 py-0.5 text-xs font-semibold text-(--mesa-muted)">
              {t.soldOut}
            </span>
          ) : item.promoPriceCents != null ? (
            <span className="flex items-baseline gap-2">
              <span className="font-bold text-(--brand-text)">
                <span className="sr-only">{t.promoPrice} </span>
                {formatCents(item.promoPriceCents)}
              </span>
              <s className="text-sm text-(--mesa-muted)">
                <span className="sr-only">{t.normalPrice} </span>
                {formatCents(item.priceCents)}
              </s>
            </span>
          ) : (
            <span className="font-bold">{formatCents(item.priceCents)}</span>
          )}
        </span>
      </div>
    </article>
  );
}
