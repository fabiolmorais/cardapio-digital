import { BookOpen, House } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";
import { tableThemeVars } from "@/lib/table-theme";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { StartSession } from "./start-session";
import type { PublicTable } from "./table-data";

const t = messages.tablePage;

/**
 * A moldura das telas da mesa: as cores da marca (tema escuro ou claro), a sessão do celular e
 * as abas de baixo. Componente do servidor: chega pronto, sem JavaScript próprio.
 */
export function TableShell({
  token,
  table,
  current,
  children,
}: {
  token: string;
  table: PublicTable;
  current: "home" | "menu";
  children: ReactNode;
}) {
  const brand = table.brand;
  return (
    <div
      style={tableThemeVars(brand.theme, brand.primary, brand.accent)}
      className="flex min-h-dvh flex-1 flex-col bg-(--mesa-bg) text-(--mesa-fg)"
    >
      <StartSession token={token} />
      <main className="mx-auto flex w-full max-w-md flex-1 flex-col gap-6 px-5 pt-6 pb-28">{children}</main>
      <nav
        aria-label={t.nav.label}
        className="fixed inset-x-0 bottom-0 border-t border-(--mesa-stroke) bg-(--mesa-bg)/95 backdrop-blur"
      >
        <div className="mx-auto grid max-w-md grid-cols-2">
          <Tab href={`/m/${token}`} on={current === "home"} icon={<House aria-hidden className="size-5" />}>
            {t.nav.home}
          </Tab>
          <Tab href={`/m/${token}/cardapio`} on={current === "menu"} icon={<BookOpen aria-hidden className="size-5" />}>
            {t.nav.menu}
          </Tab>
        </div>
      </nav>
    </div>
  );
}

function Tab({ href, on, icon, children }: { href: string; on: boolean; icon: ReactNode; children: ReactNode }) {
  return (
    <Link
      href={href}
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
