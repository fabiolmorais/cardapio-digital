"use client";

import { Plus, Search } from "lucide-react";
import Link from "next/link";
import { useDeferredValue, useEffect, useId, useRef, useState } from "react";
import { cn } from "cn";
import { tablePage } from "@/messages/table.pt-BR";
import { aLaCarte, showcase } from "./carte";
import { MenuItemCard } from "./menu-item";
import { PassagemCard } from "./passagem-card";
import { PassagemCuts } from "./passagem-cuts";
import { ProductSheet } from "./product-sheet";
import { RodizioRound } from "./rodizio-round";
import type { MenuItem, PublicMenu } from "./table-data";
import { RodizioInvite } from "./rodizio-invite";
import { useTableOrders } from "./table-orders";

const t = tablePage.menu;
// As pílulas na cor da marca, como no início: a selecionada cheia, as outras num tom leve.
const chip =
  "flex h-10 shrink-0 items-center rounded-full border px-4 text-sm font-semibold focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:outline-none";
const chipOff = "border-(--brand-text)/30 bg-(--brand)/15";

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
  // A categoria selecionada (branca): a que está na tela, ou a que o cliente tocou.
  const [current, setCurrent] = useState<string | null>(null);
  const chips = useRef<HTMLElement>(null);
  // Sem resposta, os botões ficam: o envio do pedido confere de novo.
  const { data: orders, refresh } = useTableOrders(token);
  const ordering = orders?.orderingEnabled ?? true;
  // A aba Rodízio é de quem está nele: o celular que chegou depois da ativação pergunta antes.
  const joined = orders?.rodizioParticipation === "JOINED";
  const rodizio = ordering && joined ? orders?.rodizio : undefined;
  const invite = ordering && orders?.rodizio && !joined ? orders.rodizio : undefined;
  const included = new Set(orders?.rodizioProductIds ?? []);
  const rodizioItems = menu.categories.flatMap((c) => c.products).filter((p) => included.has(p.id));
  const rounds = orders?.orders.filter((o) => o.items.some((i) => i.rodizio)).length ?? 0;
  // O cardápio de sempre, sem o que é só do rodízio (que fica na aba Rodízio).
  const categories = aLaCarte(menu);
  // A vitrine ("Mais pedidos" ou "Destaques") fica na tela da mesa: o primeiro filtro leva até ela.
  const top = showcase(menu);
  // Enquanto a lista rola até a categoria tocada, a rolagem não troca a selecionada.
  const pinnedUntil = useRef(0);

  // Vindo de uma categoria da página da mesa (#categoria-<id>): ela já chega selecionada.
  useEffect(() => {
    const id = window.location.hash.match(/^#categoria-(.+)$/)?.[1];
    if (!id || !categories.some((c) => c.id === id)) return;
    pinnedUntil.current = Date.now() + 1000;
    const frame = requestAnimationFrame(() => {
      setCurrent(id);
      document.getElementById(`categoria-${id}`)?.scrollIntoView();
    });
    return () => cancelAnimationFrame(frame);
    // Só ao abrir o cardápio.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Vindo de um destaque da página da mesa (#produto-<id>): rola até o prato e já o abre.
  useEffect(() => {
    function openFromHash() {
      const id = window.location.hash.match(/^#produto-(.+)$/)?.[1];
      const item = id && categories.flatMap((c) => c.products).find((p) => p.id === decodeURIComponent(id));
      if (!item) return;
      document.getElementById(`produto-${item.id}`)?.scrollIntoView({ block: "center" });
      if (ordering && item.available) setOpen(item);
      // Sem o #: fechar o prato e voltar a esta página não o abre de novo.
      window.history.replaceState(window.history.state, "", window.location.pathname + window.location.search);
    }
    // Um quadro depois: a lista já está na tela para a rolagem.
    const frame = requestAnimationFrame(openFromHash);
    window.addEventListener("hashchange", openFromHash);
    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener("hashchange", openFromHash);
    };
    // Só ao abrir o cardápio (e quando o # muda); o cardápio é o que veio com a página.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ordering]);

  const reels = (item: MenuItem) => `/m/${token}/reels?produto=${encodeURIComponent(item.id)}`;
  const card = (item: MenuItem) => (
    <MenuItemCard
      key={item.id}
      item={item}
      // Com vídeo, o Reels; sem foto ou só com fotos, o detalhe do prato (que leva ao Reels pela foto).
      reelsHref={item.media.some((m) => m.type === "VIDEO") ? reels(item) : undefined}
      onOpen={item.available ? () => setOpen(item) : undefined}
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

  // Rolando a lista, a categoria na faixa de cima da tela fica selecionada.
  useEffect(() => {
    if (query) return;
    const nodes = [...document.querySelectorAll<HTMLElement>("[data-categoria]")];
    const observer = new IntersectionObserver(
      (entries) => {
        if (Date.now() < pinnedUntil.current) return;
        const visible = entries
          .filter((e) => e.isIntersecting)
          .map((e) => (e.target as HTMLElement).dataset.categoria!);
        if (visible.length) setCurrent(visible[0]);
      },
      { rootMargin: "-25% 0px -65% 0px" },
    );
    nodes.forEach((node) => observer.observe(node));
    return () => observer.disconnect();
  }, [query, categories.length]);

  // A pílula selecionada aparece na faixa (rola de lado, sem mexer na página).
  useEffect(() => {
    const nav = chips.current;
    const chip = nav?.querySelector<HTMLElement>('[aria-current="true"]');
    if (nav && chip) nav.scrollTo({ left: chip.offsetLeft - 20, behavior: "smooth" });
  }, [current]);

  function goTo(event: React.MouseEvent<HTMLAnchorElement>, id: string) {
    const target = document.getElementById(id);
    if (!target) return;
    event.preventDefault();
    pinnedUntil.current = Date.now() + 1000;
    target.scrollIntoView({ behavior: "smooth", block: "start" });
    setCurrent(id.replace(/^categoria-/, ""));
  }

  const found: MenuItem[] = query
    ? categories.flatMap((c) =>
        c.products.filter((p) => plain(p.name).includes(query) || plain(p.description).includes(query)),
      )
    : [];

  return (
    <div className="flex flex-col gap-5">
      {invite && (
        <RodizioInvite
          token={token}
          rodizio={invite}
          participation={orders?.rodizioParticipation}
          onChange={() => void refresh()}
        />
      )}
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
          <p className="text-sm text-(--mesa-muted)">
            {found.length ? t.results(found.length) : t.noResults(search.trim())}
          </p>
          {found.map(card)}
        </section>
      ) : (
        <>
          <nav
            ref={chips}
            aria-label={tablePage.categories}
            className="sticky top-0 z-10 -mx-5 flex gap-2 overflow-x-auto bg-(--mesa-bg)/95 px-5 py-2 backdrop-blur [scrollbar-width:none]"
          >
            {rodizio && (
              <a href="#rodizio" className={cn(chip, chipOff)}>
                {tablePage.rodizio.tab}
              </a>
            )}
            {top.items.length > 0 && (
              <Link href={`/m/${token}#destaques`} className={cn(chip, chipOff)}>
                {top.mostOrdered ? tablePage.mostOrdered : tablePage.featured}
              </Link>
            )}
            {categories.map((c) => {
              const on = (current ?? categories[0]?.id) === c.id;
              return (
                <a
                  key={c.id}
                  href={`#categoria-${c.id}`}
                  onClick={(event) => goTo(event, `categoria-${c.id}`)}
                  aria-current={on ? "true" : undefined}
                  className={cn(chip, on ? "border-transparent bg-(--brand) text-white" : chipOff)}
                >
                  {c.name}
                </a>
              );
            })}
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
              data-categoria={c.id}
              aria-labelledby={`${inputId}-${c.id}`}
              className="flex scroll-mt-16 flex-col gap-2.5"
            >
              <h2 id={`${inputId}-${c.id}`} className="pt-1 text-xl font-bold tracking-tight">
                {c.name}
              </h2>
              {c.products.map(card)}
            </section>
          ))}
        </>
      )}
      <ProductSheet
        token={token}
        item={open}
        onClose={() => setOpen(null)}
        reelsHref={(item) => (item.media.length > 0 ? reels(item) : undefined)}
      />
    </div>
  );
}
