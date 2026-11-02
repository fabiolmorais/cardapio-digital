"use client";

import { Search } from "lucide-react";
import { useDeferredValue, useId, useState } from "react";
import { messages } from "@/messages/pt-BR";
import { MenuItemCard } from "./menu-item";
import type { MenuItem, PublicMenu } from "./table-data";

const t = messages.tablePage.menu;

/** "Açaí com Granola" → "acai com granola": a busca não liga para acento nem maiúscula. */
function plain(text: string) {
  return text.normalize("NFD").replace(/\p{M}/gu, "").toLowerCase();
}

/**
 * O cardápio com a busca. A busca roda aqui, no celular, sobre o cardápio que já veio com a
 * página: sem ida à rede a cada letra (ADR 0006). As abas são âncoras para as seções.
 */
export function MenuBrowser({ menu }: { menu: PublicMenu }) {
  const [search, setSearch] = useState("");
  const query = plain(useDeferredValue(search).trim());
  const inputId = useId();

  const found: MenuItem[] = query
    ? menu.categories.flatMap((c) =>
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
          {found.map((item) => (
            <MenuItemCard key={item.id} item={item} />
          ))}
        </section>
      ) : (
        <>
          <nav aria-label={messages.tablePage.categories} className="-mx-5 flex gap-2 overflow-x-auto px-5 pb-1">
            {menu.categories.map((c) => (
              <a
                key={c.id}
                href={`#categoria-${c.id}`}
                className="flex h-10 shrink-0 items-center rounded-full border border-(--mesa-stroke) bg-(--mesa-glass) px-4 text-sm font-semibold focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
              >
                {c.name}
              </a>
            ))}
          </nav>
          {menu.categories.map((c) => (
            <section
              key={c.id}
              id={`categoria-${c.id}`}
              aria-labelledby={`${inputId}-${c.id}`}
              className="flex scroll-mt-6 flex-col gap-2.5"
            >
              <h2 id={`${inputId}-${c.id}`} className="pt-1 text-xl font-bold tracking-tight">
                {c.name}
              </h2>
              {c.products.map((item) => (
                <MenuItemCard key={item.id} item={item} />
              ))}
            </section>
          ))}
        </>
      )}
    </div>
  );
}
