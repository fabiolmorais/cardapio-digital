"use client";

import { BookOpen, Home, LogOut, QrCode, Users } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Suspense, type ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { useLogout } from "@/lib/auth/use-logout";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";

const t = messages.admin;

const NAV = [
  { href: "/painel", label: t.nav.panel, icon: Home },
  { href: "/admin/mesas", label: t.nav.floor, icon: QrCode },
  { href: "/admin/cardapio", label: t.nav.menu, icon: BookOpen },
  { href: "/admin/equipe", label: t.nav.team, icon: Users },
] as const;

function initials(name: string) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}

/**
 * Moldura das telas de gestão: menu à esquerda (no celular, em cima) e a tela à direita.
 * Confirma a sessão (sem ela, manda para /entrar). A tela de dentro é sempre renderizada
 * e mostra o próprio "carregando" até a sessão chegar.
 */
export function AdminShell({ children }: { children: ReactNode }) {
  const { status, session } = useRequiredSession();
  const { leaving, logout } = useLogout();

  const establishment = status === "authenticated" ? session.establishment : null;

  return (
    <div className="flex min-h-dvh flex-col md:flex-row">
      <aside
        aria-label={t.management}
        className="flex shrink-0 flex-col gap-4 border-b border-border bg-card px-4 py-4 md:w-64 md:border-r md:border-b-0 md:py-6"
      >
        <div className="flex items-center gap-3 px-2">
          <span
            aria-hidden
            className="flex size-9 shrink-0 items-center justify-center rounded-[10px] bg-primary text-[13px] font-bold text-primary-foreground"
          >
            {establishment ? initials(establishment.name) : ""}
          </span>
          <span className="flex min-w-0 flex-col">
            <span className="truncate font-semibold">{establishment?.name ?? t.loading}</span>
            <span className="text-sm text-muted-foreground">{t.management}</span>
          </span>
        </div>
        {/* O endereço só existe quando a página abre (rotas como /admin/cardapio/produtos/[id]):
            o menu espera por ele num Suspense, já desenhado, só sem o item aceso. */}
        <Suspense fallback={<AdminNav pathname={null} />}>
          <ActiveAdminNav />
        </Suspense>
        <Button variant="ghost" className="h-11 justify-start gap-3 px-3 md:mt-auto" onClick={logout} disabled={leaving}>
          <LogOut aria-hidden />
          {t.logout}
        </Button>
      </aside>
      <main className="min-w-0 flex-1">{children}</main>
    </div>
  );
}

function ActiveAdminNav() {
  return <AdminNav pathname={usePathname()} />;
}

function AdminNav({ pathname }: { pathname: string | null }) {
  return (
    <nav className="flex gap-1 overflow-x-auto md:flex-col">
      {NAV.map(({ href, label, icon: Icon }) => {
        // As telas de dentro (como /admin/cardapio/produtos/…) também acendem o item.
        const active = pathname !== null && (pathname === href || pathname.startsWith(`${href}/`));
        return (
          <Link
            key={href}
            href={href}
            aria-current={active ? "page" : undefined}
            className={cn(
              "flex h-11 shrink-0 items-center gap-3 rounded-xl px-3 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
              active && "bg-secondary text-foreground",
            )}
          >
            <Icon aria-hidden className="size-4" />
            {label}
          </Link>
        );
      })}
    </nav>
  );
}
