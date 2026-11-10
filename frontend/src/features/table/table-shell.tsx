import { BookOpen, ClipboardList, House, PauseCircle } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";
import { tableThemeVars } from "@/lib/table-theme";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { CartBar } from "./cart-bar";
import { StartSession } from "./start-session";
import type { PublicTable } from "./table-data";

const t = messages.tablePage;

/**
 * A moldura das telas da mesa: as cores da marca (tema escuro ou claro), a sessão do celular e
 * as abas de baixo, com a barra "Ver pedido" em cima delas quando o carrinho tem algo (no
 * cardápio e na tela inicial). Componente do servidor: só a barra do carrinho roda no navegador.
 */
export function TableShell({
  token,
  table,
  current,
  ambient,
  children,
}: {
  token: string;
  table: PublicTable;
  current: "home" | "menu" | "orders" | "cart";
  /** Uma foto para o fundo do topo, desfocada (o início da mesa, desenho 2.1). */
  ambient?: string;
  children: ReactNode;
}) {
  const brand = table.brand;
  return (
    <div
      style={tableThemeVars(brand.theme, brand.primary, brand.accent)}
      className="relative isolate flex min-h-dvh flex-1 flex-col overflow-x-clip bg-(--mesa-bg) text-(--mesa-fg)"
    >
      {ambient && (
        <div aria-hidden className="pointer-events-none absolute inset-x-0 top-0 -z-10 h-[460px] overflow-hidden">
          {/* eslint-disable-next-line @next/next/no-img-element -- só o fundo desfocado, foto já vinda do R2 */}
          <img
            src={ambient}
            alt=""
            decoding="async"
            className={cn(
              "absolute -inset-x-16 -top-16 h-[420px] w-[calc(100%+8rem)] max-w-none object-cover blur-[48px] saturate-[1.3]",
              brand.theme === "DARK" ? "opacity-40" : "opacity-25",
            )}
          />
          <div className="absolute inset-0 bg-linear-to-b from-transparent to-(--mesa-bg) to-[92%]" />
        </div>
      )}
      <StartSession token={token} />
      {/* Embaixo, espaço para as abas (e para a barra do carrinho, onde ela aparece). */}
      <main
        className={cn(
          "mx-auto flex w-full max-w-md flex-1 flex-col gap-6 px-5 pt-6 pb-28",
          (current === "home" || current === "menu") && "pb-44",
        )}
      >
        {/* Assinatura suspensa: a página abre (o cardápio vale como vitrine), mas não chama nem pede. */}
        {!table.serving && (
          <section
            role="status"
            className="flex items-start gap-3 rounded-2xl border border-(--mesa-stroke) bg-(--mesa-glass) p-4"
          >
            <PauseCircle aria-hidden className="mt-0.5 size-5 shrink-0 text-(--brand-text)" />
            <span className="flex flex-col gap-1">
              <span className="font-semibold">{t.paused.title}</span>
              <span className="text-sm text-(--mesa-muted)">{t.paused.text}</span>
            </span>
          </section>
        )}
        {children}
      </main>
      {/* As abas flutuando sobre a página, em vidro (desenho 2.1), com a barra do carrinho em cima. */}
      <nav
        aria-label={t.nav.label}
        className="fixed inset-x-0 bottom-0 z-10 px-4 pb-[max(env(safe-area-inset-bottom),16px)]"
      >
        {table.serving && (current === "home" || current === "menu") && <CartBar token={token} />}
        <div className="mx-auto max-w-md rounded-3xl border border-(--mesa-stroke) bg-(--mesa-bar) shadow-[0_12px_32px_rgba(0,0,0,.45)] backdrop-blur-xl">
          <div className="grid h-[68px] grid-cols-3">
            <Tab href={`/m/${token}`} on={current === "home"} icon={<House aria-hidden className="size-5" />}>
              {t.nav.home}
            </Tab>
            <Tab
              href={`/m/${token}/cardapio`}
              on={current === "menu"}
              icon={<BookOpen aria-hidden className="size-5" />}
            >
              {t.nav.menu}
            </Tab>
            <Tab
              href={`/m/${token}/pedidos`}
              on={current === "orders"}
              icon={<ClipboardList aria-hidden className="size-5" />}
            >
              {t.nav.orders}
            </Tab>
          </div>
        </div>
      </nav>
    </div>
  );
}

function Tab({ href, on, icon, children }: { href: string; on: boolean; icon: ReactNode; children: ReactNode }) {
  return (
    <Link
      href={href}
      // Sem pré-carregar as outras abas: o código delas viria junto ao abrir a página, e a mesa
      // tem orçamento de 200 KB de JavaScript. A troca de aba busca na hora (é rápida no 4G).
      prefetch={false}
      aria-current={on ? "page" : undefined}
      className={cn(
        "flex min-h-16 flex-col items-center justify-center gap-1 text-xs font-semibold text-(--mesa-muted) focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none focus-visible:ring-inset",
        on && "text-(--brand-text)",
      )}
    >
      {icon}
      {children}
    </Link>
  );
}
