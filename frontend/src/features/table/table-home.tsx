import { ExternalLink, Plus, Star, UtensilsCrossed } from "lucide-react";
import Link from "next/link";
import { BrandLogo } from "@/components/brand-logo";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { aLaCarte, showcase } from "./carte";
import { RodizioEntry } from "./rodizio-entry";
import { TableActions } from "./table-actions";
import { TableShell } from "./table-shell";
import type { PublicMenu, PublicTable } from "./table-data";

const t = messages.tablePage;
const chip =
  "flex h-10 shrink-0 items-center rounded-full px-4 text-sm font-semibold focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none";

/**
 * O início da mesa (desenho 2.1): a marca e a mesa (com "No salão" quando a presença está
 * confirmada), o "Peça sem esperar", os botões (Chamar garçom e Pedir a conta, a parte com
 * JavaScript), as categorias com ícone, o rodízio, os destaques e os links do restaurante, sobre a
 * foto de um prato desfocada.
 */
export function TableHome({
  token,
  table,
  menu,
}: {
  token: string;
  table: PublicTable;
  menu: PublicMenu | null;
}) {
  const brand = table.brand;
  const categories = menu ? aLaCarte(menu) : [];
  const products = categories.flatMap((c) => c.products);
  const top = menu ? showcase(menu) : { items: [], mostOrdered: false };
  const featured = top.items;
  const showcaseTitle = top.mostOrdered ? t.mostOrdered : t.featured;
  const menuHref = `/m/${token}/cardapio`;
  // O fundo: a foto do primeiro destaque (ou de qualquer prato), desfocada.
  const ambient = (
    featured.find((p) => p.photo) ?? products.find((p) => p.photo)
  )?.photo?.medium;

  const header = (
    <header className="flex min-w-0 items-center gap-3">
      <BrandLogo
        logo={brand.logo?.small}
        name={table.establishmentName}
        color={brand.primary}
        alt=""
        className="size-[46px] text-[15px]"
      />
      <div className="flex min-w-0 flex-col gap-0.5">
        <span className="truncate font-semibold">
          {table.establishmentName}
        </span>
        <span className="text-[13px] text-(--mesa-muted)">
          {t.where(table.tableLabel, table.sectorName)}
        </span>
      </div>
    </header>
  );
  const intro = (
    <section className="flex flex-col gap-1.5">
      <p className="text-[13px] text-(--mesa-muted)">{t.welcome}</p>
      <h1 className="text-[34px] leading-[38px] font-bold tracking-[-0.8px]">
        {t.headline}
        <br />
        <span className="text-(--brand-text)">{t.highlight}</span>
      </h1>
    </section>
  );

  return (
    <TableShell token={token} table={table} current="home" ambient={ambient}>
      {/* Atendimento pausado: sem o "Peça sem esperar" e sem os botões de chamar e pedir. */}
      {table.serving ? (
        <TableActions
          token={token}
          header={header}
          intro={intro}
          arrival={{
            name: table.establishmentName,
            where: t.where(table.tableLabel, table.sectorName),
            logo: (
              <BrandLogo
                logo={brand.logo?.small}
                name={table.establishmentName}
                color={brand.primary}
                alt=""
                className="size-full"
              />
            ),
          }}
        />
      ) : (
        header
      )}

      {categories.length > 0 && (
        <nav
          aria-label={t.categories}
          className="-mx-5 flex gap-2.5 overflow-x-auto px-5 pb-1 [scrollbar-width:none]"
        >
          {/* Só os nomes (ícones não servem a todo tipo de restaurante), com a cor da marca para dar vida. */}
          {featured.length > 0 && (
            <a
              href="#destaques"
              className={cn(chip, "bg-(--brand) text-white")}
            >
              {showcaseTitle}
            </a>
          )}
          {categories.map((c) => (
            <Link
              key={c.id}
              href={`${menuHref}#categoria-${c.id}`}
              className={cn(
                chip,
                "border border-(--brand-text)/30 bg-(--brand)/15",
              )}
            >
              {c.name}
            </Link>
          ))}
        </nav>
      )}

      {table.serving && <RodizioEntry token={token} photo={ambient} />}

      {featured.length > 0 && (
        <section
          aria-labelledby="destaques"
          className="flex scroll-mt-6 flex-col gap-3"
        >
          <div className="flex items-center justify-between">
            <h2
              id="destaques"
              className="scroll-mt-6 text-xl font-semibold tracking-[-0.3px]"
            >
              {showcaseTitle}
            </h2>
            <Link
              href={menuHref}
              className="text-[13px] font-semibold text-(--brand-text)"
            >
              {t.seeMenu}
            </Link>
          </div>
          <ul className="-mx-5 flex gap-3 overflow-x-auto px-5 pb-1 [scrollbar-width:none]">
            {featured.map((item) => (
              <li key={item.id} className="w-[168px] shrink-0">
                {/* Com foto ou vídeo, abre o Reels do prato; sem, abre o prato no cardápio. */}
                <Link
                  href={
                    item.media.length > 0
                      ? `/m/${token}/reels?produto=${encodeURIComponent(item.id)}&de=inicio`
                      : `${menuHref}#produto-${item.id}`
                  }
                  prefetch={false}
                  className="flex flex-col gap-2.5 rounded-[22px] border border-(--mesa-stroke) bg-(--mesa-glass) p-2 pb-3 focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
                >
                  <span className="flex h-[120px] items-center justify-center overflow-hidden rounded-2xl bg-(--mesa-stroke) text-(--mesa-muted)">
                    {item.photo ? (
                      // eslint-disable-next-line @next/next/no-img-element -- foto já no tamanho certo, vinda do R2
                      <img
                        src={item.photo.medium}
                        alt=""
                        loading="lazy"
                        decoding="async"
                        className="size-full object-cover"
                      />
                    ) : (
                      <UtensilsCrossed aria-hidden className="size-7" />
                    )}
                  </span>
                  <span className="flex flex-col gap-1.5 px-1">
                    <span className="line-clamp-1 font-semibold">
                      {item.name}
                    </span>
                    <span className="flex items-center justify-between gap-2">
                      <span className="font-bold">
                        {formatCents(item.promoPriceCents ?? item.priceCents)}
                      </span>
                      <span
                        aria-hidden
                        className="flex size-9 items-center justify-center rounded-xl bg-(--brand) text-white shadow-[0_6px_16px_rgba(0,0,0,.3)]"
                      >
                        <Plus className="size-[18px]" strokeWidth={2.4} />
                      </span>
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
            <ExternalRow
              href={`https://instagram.com/${brand.instagram}`}
              icon={
                <span aria-hidden className="text-lg font-bold">
                  @
                </span>
              }
            >
              {t.follow} · @{brand.instagram}
            </ExternalRow>
          )}
          {brand.googleReviewUrl && (
            <ExternalRow
              href={brand.googleReviewUrl}
              icon={<Star aria-hidden className="size-5" />}
            >
              {t.review}
            </ExternalRow>
          )}
        </section>
      )}
    </TableShell>
  );
}

/** Link para fora (Instagram, Google): outra aba, sem passar o endereço desta página (noreferrer). */
function ExternalRow({
  href,
  icon,
  children,
}: {
  href: string;
  icon: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className="flex h-13 items-center gap-3 rounded-2xl border border-(--mesa-stroke) bg-(--mesa-glass) px-4 font-medium focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
    >
      <span className="flex size-8 items-center justify-center text-(--brand-text)">
        {icon}
      </span>
      <span className="flex-1">{children}</span>
      <ExternalLink aria-hidden className="size-4 text-(--mesa-muted)" />
      <span className="sr-only">{t.newTab}</span>
    </a>
  );
}
