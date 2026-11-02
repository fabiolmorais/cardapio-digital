import { ChevronRight, ExternalLink, Star, UtensilsCrossed } from "lucide-react";
import Link from "next/link";
import { BrandLogo } from "@/components/brand-logo";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { TableActions } from "./table-actions";
import { TableShell } from "./table-shell";
import type { PublicMenu, PublicTable } from "./table-data";

const t = messages.tablePage;

/**
 * "Como podemos ajudar?" (desenho Main): a marca, a mesa, os botões (Chamar garçom e Pedir a
 * conta, a única parte com JavaScript), as categorias, os destaques e os links do restaurante.
 */
export function TableHome({ token, table, menu }: { token: string; table: PublicTable; menu: PublicMenu | null }) {
  const brand = table.brand;
  const featured = (menu?.categories ?? []).flatMap((c) => c.products).filter((p) => p.featured && p.available);
  const menuHref = `/m/${token}/cardapio`;

  return (
    <TableShell token={token} table={table} current="home">
      <header className="flex items-center gap-3">
        <BrandLogo logo={brand.logo?.small} name={table.establishmentName} color={brand.primary} alt="" className="size-12 text-[15px]" />
        <div className="flex min-w-0 flex-col">
          <span className="truncate text-lg font-semibold">{table.establishmentName}</span>
          <span className="text-sm text-(--mesa-muted)">{t.where(table.tableLabel, table.sectorName)}</span>
        </div>
      </header>

      <section className="flex flex-col gap-1.5">
        <p className="text-sm text-(--mesa-muted)">{t.welcome}</p>
        <h1 className="text-[34px] leading-[38px] font-bold tracking-tight">
          {t.headline}
          <br />
          <span className="text-(--brand-text)">{t.highlight}</span>
        </h1>
      </section>

      <TableActions token={token} />

      <Link
        href={menuHref}
        className="flex h-14 items-center justify-between rounded-2xl border border-(--mesa-stroke) bg-(--mesa-glass) px-5 font-semibold focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
      >
        {t.seeMenu}
        <ChevronRight aria-hidden className="size-5" />
      </Link>

      {menu && menu.categories.length > 0 && (
        <nav aria-label={t.categories} className="-mx-5 flex gap-2 overflow-x-auto px-5 pb-1">
          {menu.categories.map((c) => (
            <Link
              key={c.id}
              href={`${menuHref}#categoria-${c.id}`}
              className="flex h-10 shrink-0 items-center rounded-full border border-(--mesa-stroke) bg-(--mesa-glass) px-4 text-sm font-semibold focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
            >
              {c.name}
            </Link>
          ))}
        </nav>
      )}

      {featured.length > 0 && (
        <section aria-labelledby="destaques" className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <h2 id="destaques" className="text-xl font-bold tracking-tight">
              {t.featured}
            </h2>
            <Link href={menuHref} className="text-sm font-semibold text-(--brand-text)">
              {t.seeAll}
            </Link>
          </div>
          <ul className="-mx-5 flex gap-3 overflow-x-auto px-5 pb-1">
            {featured.map((item) => (
              <li key={item.id} className="w-40 shrink-0">
                <Link
                  href={`${menuHref}#produto-${item.id}`}
                  className="flex flex-col gap-2.5 rounded-[22px] border border-(--mesa-stroke) bg-(--mesa-glass) p-2 pb-3 focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
                >
                  <span className="flex h-28 items-center justify-center overflow-hidden rounded-2xl bg-(--mesa-stroke) text-(--mesa-muted)">
                    {item.photo ? (
                      // eslint-disable-next-line @next/next/no-img-element -- foto já no tamanho certo, vinda do R2
                      <img src={item.photo.medium} alt="" loading="lazy" decoding="async" className="size-full object-cover" />
                    ) : (
                      <UtensilsCrossed aria-hidden className="size-7" />
                    )}
                  </span>
                  <span className="flex flex-col gap-1 px-1">
                    <span className="line-clamp-1 font-semibold">{item.name}</span>
                    <span className="font-bold text-(--brand-text)">
                      {formatCents(item.promoPriceCents ?? item.priceCents)}
                    </span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      {(brand.instagram || brand.googleReviewUrl) && (
        <section className="flex flex-col gap-2">
          {brand.instagram && (
            <ExternalRow href={`https://instagram.com/${brand.instagram}`} icon={<span aria-hidden className="text-lg font-bold">@</span>}>
              {t.follow} · @{brand.instagram}
            </ExternalRow>
          )}
          {brand.googleReviewUrl && (
            <ExternalRow href={brand.googleReviewUrl} icon={<Star aria-hidden className="size-5" />}>
              {t.review}
            </ExternalRow>
          )}
        </section>
      )}
    </TableShell>
  );
}

/** Link para fora (Instagram, Google): outra aba, sem passar o endereço desta página (noreferrer). */
function ExternalRow({ href, icon, children }: { href: string; icon: React.ReactNode; children: React.ReactNode }) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className="flex h-13 items-center gap-3 rounded-2xl border border-(--mesa-stroke) bg-(--mesa-glass) px-4 font-medium focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
    >
      <span className="flex size-8 items-center justify-center text-(--brand-text)">{icon}</span>
      <span className="flex-1">{children}</span>
      <ExternalLink aria-hidden className="size-4 text-(--mesa-muted)" />
      <span className="sr-only">{t.newTab}</span>
    </a>
  );
}
