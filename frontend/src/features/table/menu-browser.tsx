"use client";

import { Plus, Search } from "lucide-react";
import { useDeferredValue, useId, useState } from "react";
import { tablePage } from "@/messages/table.pt-BR";
import { aLaCarte } from "./carte";
import { MenuItemCard } from "./menu-item";
import { PassagemCard } from "./passagem-card";
import { PassagemCuts } from "./passagem-cuts";
import { ProductSheet } from "./product-sheet";
import { RodizioRound } from "./rodizio-round";
import type { MenuItem, PublicMenu } from "./table-data";
import { useTableOrders } from "./table-orders";

const t = tablePage.menu;

/** "Açaí com Granola" → "acai com granola": a busca não liga para acento nem maiúscula. */
function plain(text: string) {
  return text.normalize("NFD").replace(/\p{M}/gu, "").toLowerCase();
}

/**
 * O cardápio com a busca. A busca roda aqui, no celular, sobre o cardápio que já veio com a
 * página: sem ida à rede a cada letra (ADR 0006). As abas são âncoras para as seções.
 *
 * Cada produto disponível tem o "Adicionar", que abre o prato com a observação e a quantidade.
 * Se o restaurante desligou o pedido pelo celular, o botão some (o cardápio continua).
 *
 * Com o rodízio ativo na mesa, a aba Rodízio vem antes de tudo, com os produtos incluídos sem
 * preço e as rodadas. O estado vem dos pedidos da mesa, e o tempo real avisa quando o garçom ativa.
 */
export function MenuBrowser({ token, menu }: { token: string; menu: PublicMenu }) {
  const [search, setSearch] = useState("");
  const query = plain(useDeferredValue(search).trim());
  const inputId = useId();
  const [open, setOpen] = useState<MenuItem | null>(null);
  // Sem resposta, os botões ficam: o envio do pedido confere de novo.
  const { data: orders } = useTableOrders(token);
  const ordering = orders?.orderingEnabled ?? true;
  const rodizio = ordering ? orders?.rodizio : undefined;
  const included = new Set(orders?.rodizioProductIds ?? []);
  const rodizioItems = menu.categories.flatMap((c) => c.products).filter((p) => included.has(p.id));
  const rounds = orders?.orders.filter((o) => o.items.some((i) => i.rodizio)).length ?? 0;
  // O cardápio de sempre, sem o que é só do rodízio (que fica na aba Rodízio).
  const categories = aLaCarte(menu);

  const card = (item: MenuItem) => (
    <MenuItemCard
      key={item.id}
      item={item}
      reelsHref={item.media.length > 0 ? `/m/${token}/reels?produto=${encodeURIComponent(item.id)}` : undefined}
      action={
        ordering && (
          <button
            type="button"
            onClick={() => setOpen(item)}
            aria-label={t.addNamed(item.name)}
            className="flex h-10 shrink-0 items-center gap-1 rounded-full bg-(--brand) px-3.5 text-sm font-semibold text-white focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:ring-offset-2 focus-visible:ring-offset-(--mesa-bg) focus-visible:outline-none"
          >
            <Plus aria-hidden className="size-4" />
            {t.add}
          </button>
        )
      }
    />
  );

  const found: MenuItem[] = query
    ? categories.flatMap((c) =>
        c.products.filter((p) => plain(p.name).includes(query) || plain(p.description).includes(query)),
      )
    : [];

  return (
    <div className="flex flex-col gap-5">
      <div className="relative">
        <Search
          aria-hidden
          className="pointer-events-none absolute top-1/2 left-4 size-5 -translate-y-1/2 text-(--mesa-muted)"
        />
        <label htmlFor={inputId} className="sr-only">
          {t.search}
        </label>
        <input
          id={inputId}
          type="search"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder={t.searchPlaceholder}
          autoComplete="off"
          className="h-13 w-full rounded-2xl border border-(--mesa-stroke) bg-(--mesa-glass) pr-4 pl-12 text-base text-(--mesa-fg) outline-none placeholder:text-(--mesa-muted) focus-visible:ring-3 focus-visible:ring-(--brand)"
        />
      </div>

      {query ? (
        <section aria-live="polite" className="flex flex-col gap-2.5">
          <p className="text-sm text-(--mesa-muted)">{found.length ? t.results(found.length) : t.noResults(search.trim())}</p>
          {found.map(card)}
        </section>
      ) : (
        <>
          <nav aria-label={tablePage.categories} className="-mx-5 flex gap-2 overflow-x-auto px-5 pb-1">
            {rodizio && (
              <a
                href="#rodizio"
                className="flex h-10 shrink-0 items-center rounded-full bg-(--brand) px-4 text-sm font-semibold text-white focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:outline-none"
              >
                {tablePage.rodizio.tab}
              </a>
            )}
            {categories.map((c) => (
              <a
                key={c.id}
                href={`#categoria-${c.id}`}
                className="flex h-10 shrink-0 items-center rounded-full border border-(--mesa-stroke) bg-(--mesa-glass) px-4 text-sm font-semibold focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
              >
                {c.name}
              </a>
            ))}
          </nav>
          {rodizio?.type === "PASSAGEM" && (
            <>
              <PassagemCard token={token} rodizio={rodizio} />
              <PassagemCuts token={token} items={rodizioItems} orders={orders?.orders ?? []} />
            </>
          )}
          {rodizio?.type === "A_LA_MINUTE" && (
            <RodizioRound
              token={token}
              rodizio={rodizio}
              items={rodizioItems}
              rounds={rounds}
              limits={orders?.rodizioLimits}
            />
          )}
          {categories.length === 0 && !rodizio && <p className="text-(--mesa-muted)">{t.empty}</p>}
          {categories.map((c) => (
            <section
              key={c.id}
              id={`categoria-${c.id}`}
              aria-labelledby={`${inputId}-${c.id}`}
              className="flex scroll-mt-6 flex-col gap-2.5"
            >
              <h2 id={`${inputId}-${c.id}`} className="pt-1 text-xl font-bold tracking-tight">
                {c.name}
              </h2>
              {c.products.map(card)}
            </section>
          ))}
        </>
      )}
      <ProductSheet token={token} item={open} onClose={() => setOpen(null)} />
    </div>
  );
}
