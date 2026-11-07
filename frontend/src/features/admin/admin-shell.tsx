"use client";

import {
  BellRing,
  BookOpen,
  ChefHat,
  CreditCard,
  Flame,
  LogOut,
  Palette,
  Printer,
  QrCode,
  Receipt,
  ShieldCheck,
  Smartphone,
  Tv,
  Users,
  UtensilsCrossed,
  type LucideIcon,
} from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Suspense, type ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { SubscriptionBanner } from "@/features/subscription/subscription-banner";
import { canManageTeam, type Role } from "@/features/team/team-rules";
import { useLogout } from "@/lib/auth/use-logout";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { initials } from "@/lib/initials";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";

const t = messages.admin;

type Item = { href: string; label: string; icon: LucideIcon };

const LIVE: Item[] = [{ href: "/painel", label: t.nav.panel, icon: BellRing }];

/** As telas de tela cheia: a TV, a cozinha, os passadores, a impressão e o app no celular. */
const SCREENS: Item[] = [
  { href: "/painel/tv", label: t.nav.tv, icon: Tv },
  { href: "/painel/cozinha", label: t.nav.kitchen, icon: ChefHat },
  { href: "/painel/passadores", label: t.nav.passagem, icon: Flame },
  { href: "/painel/impressao", label: t.nav.printing, icon: Printer },
  { href: "/painel/instalar", label: t.nav.install, icon: Smartphone },
];

/** A gestão do restaurante: só para o dono e o gerente. */
const MANAGEMENT: Item[] = [
  { href: "/admin/mesas", label: t.nav.floor, icon: QrCode },
  { href: "/admin/cardapio", label: t.nav.menu, icon: BookOpen },
  { href: "/admin/rodizios", label: t.nav.rodizios, icon: UtensilsCrossed },
  { href: "/admin/marca", label: t.nav.brand, icon: Palette },
  { href: "/admin/presenca", label: t.nav.presence, icon: ShieldCheck },
  { href: "/admin/equipe", label: t.nav.team, icon: Users },
  { href: "/admin/comandas", label: t.nav.tabs, icon: Receipt },
  { href: "/admin/assinatura", label: t.nav.subscription, icon: CreditCard },
];

/**
 * A moldura do painel e da gestão (desenho 7.1): o menu à esquerda (no celular, em cima, rolando
 * para o lado), com o "Ao vivo", as telas e, para o dono e o gerente, a gestão; embaixo, quem está
 * logado e o "Sair". Confirma a sessão (sem ela, manda para /entrar). A tela de dentro é sempre
 * renderizada e mostra o próprio "carregando" até a sessão chegar.
 */
export function AdminShell({ children }: { children: ReactNode }) {
  const { status, session } = useRequiredSession();
  const { leaving, logout } = useLogout();

  const establishment = status === "authenticated" ? session.establishment : null;
  const role = establishment?.role as Role | undefined;

  return (
    <div className="flex min-h-dvh flex-col md:flex-row">
      <aside
        aria-label={t.menuLabel}
        className="flex shrink-0 flex-col gap-4 border-b border-border bg-card px-4 py-4 md:sticky md:top-0 md:h-dvh md:w-64 md:overflow-y-auto md:border-r md:border-b-0 md:py-6"
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
            <span className="text-sm text-muted-foreground">{t.staffPanel}</span>
          </span>
        </div>
        {/* O endereço só existe quando a página abre (rotas como /admin/cardapio/produtos/[id]):
            o menu espera por ele num Suspense, já desenhado, só sem o item aceso. */}
        <Suspense fallback={<ShellNav pathname={null} role={role} />}>
          <ActiveShellNav role={role} />
        </Suspense>
        <div className="flex flex-wrap items-center gap-3 border-border px-2 md:mt-auto md:border-t md:pt-4">
          {session && (
            <>
              <span
                aria-hidden
                className="flex size-9 shrink-0 items-center justify-center rounded-full bg-secondary text-xs font-bold"
              >
                {initials(session.userName)}
              </span>
              <span className="flex min-w-0 flex-1 flex-col">
                <span className="truncate text-sm font-semibold">{session.userName}</span>
                <span className="text-sm text-muted-foreground">
                  {messages.panel.roleLabel}: {messages.panel.roles[session.establishment.role]}
                </span>
              </span>
            </>
          )}
          <Button
            variant="ghost"
            className="h-11 shrink-0 gap-2 px-3 md:w-full md:justify-start"
            onClick={logout}
            disabled={leaving}
          >
            <LogOut aria-hidden />
            {t.logout}
          </Button>
        </div>
      </aside>
      <main className="min-w-0 flex-1">
        <SubscriptionBanner className="mx-4 mt-4 sm:mx-7" />
        {children}
      </main>
    </div>
  );
}

function ActiveShellNav({ role }: { role: Role | undefined }) {
  return <ShellNav pathname={usePathname()} role={role} />;
}

/** "/painel" acende só nele mesmo e na comanda (as telas de /painel/… têm item próprio). */
function isActive(pathname: string | null, href: string) {
  if (pathname === null) return false;
  if (href === "/painel") return pathname === "/painel" || pathname.startsWith("/painel/comandas/");
  return pathname === href || pathname.startsWith(`${href}/`);
}

function ShellNav({ pathname, role }: { pathname: string | null; role: Role | undefined }) {
  const groups: { label: string | null; items: Item[] }[] = [
    { label: null, items: LIVE },
    { label: t.screens, items: SCREENS },
    ...(role && canManageTeam(role) ? [{ label: t.management, items: MANAGEMENT }] : []),
  ];
  return (
    <nav aria-label={t.navigation} className="flex gap-1 overflow-x-auto md:flex-col">
      {groups.map(({ label, items }) => (
        <div key={label ?? "live"} className="flex shrink-0 gap-1 md:flex-col">
          {label && (
            <span className="hidden px-3 pt-4 pb-1.5 text-[11px] font-semibold tracking-[.4px] text-muted-foreground uppercase md:block">
              {label}
            </span>
          )}
          {items.map(({ href, label: text, icon: Icon }) => {
            const active = isActive(pathname, href);
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
                {text}
              </Link>
            );
          })}
        </div>
      ))}
    </nav>
  );
}
