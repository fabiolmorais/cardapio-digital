"use client";

import {
  BellRing,
  BookOpen,
  ChefHat,
  CreditCard,
  Flame,
  LayoutGrid,
  LogOut,
  Menu,
  Palette,
  Printer,
  QrCode,
  Receipt,
  ShieldCheck,
  Smartphone,
  Tv,
  Users,
  UtensilsCrossed,
  X,
  type LucideIcon,
} from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Suspense, useState, type ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { useLiveCount } from "@/features/live/use-live-count";
import { SubscriptionBanner } from "@/features/subscription/subscription-banner";
import { canManageTeam, type Role } from "@/features/team/team-rules";
import { useLogout } from "@/lib/auth/use-logout";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { initials } from "@/lib/initials";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";

const t = messages.admin;

/** O item do menu; `roles`: só para esses papéis (sem a lista, para todos). */
type Item = { href: string; label: string; icon: LucideIcon; roles?: readonly Role[] };

const MANAGERS = ["OWNER", "MANAGER"] as const;
const MANAGERS_AND_STAFF = ["OWNER", "MANAGER", "STAFF"] as const;

const LIVE: Item[] = [
  { href: "/painel", label: t.nav.panel, icon: BellRing },
  { href: "/painel/mesas", label: t.nav.floorLive, icon: LayoutGrid, roles: [...MANAGERS_AND_STAFF] },
  { href: "/painel/comandas", label: t.nav.openTabs, icon: Receipt, roles: [...MANAGERS_AND_STAFF] },
];

/**
 * As telas de tela cheia, cada uma para quem a usa: o Modo TV o dono ou o gerente liga na TV; a
 * cozinha e a impressão ficam com a cozinha (e o balcão); os passadores são garçons do churrasco.
 */
const SCREENS: Item[] = [
  { href: "/painel/tv", label: t.nav.tv, icon: Tv, roles: MANAGERS },
  { href: "/painel/cozinha", label: t.nav.kitchen, icon: ChefHat, roles: [...MANAGERS, "KITCHEN"] },
  { href: "/painel/passadores", label: t.nav.passagem, icon: Flame, roles: [...MANAGERS, "STAFF"] },
  { href: "/painel/impressao", label: t.nav.printing, icon: Printer, roles: [...MANAGERS, "KITCHEN"] },
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
  // No celular, o menu fica recolhido atrás do botão (hambúrguer); no computador, sempre aberto.
  const [open, setOpen] = useState(false);
  const close = () => setOpen(false);

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
          <span className="flex min-w-0 flex-1 flex-col">
            <span className="truncate font-semibold">{establishment?.name ?? t.loading}</span>
            <span className="text-sm text-muted-foreground">{t.staffPanel}</span>
          </span>
          <Button
            variant="ghost"
            size="icon-lg"
            className="size-11 md:hidden"
            aria-label={t.openMenu}
            aria-expanded={open}
            aria-controls="menu-do-painel"
            onClick={() => setOpen((v) => !v)}
          >
            {open ? <X aria-hidden className="size-6" /> : <Menu aria-hidden className="size-6" />}
          </Button>
        </div>
        <div id="menu-do-painel" className={cn("flex-1 flex-col gap-4 md:flex", open ? "flex" : "hidden")}>
          {/* O endereço só existe quando a página abre (rotas como /admin/cardapio/produtos/[id]):
            o menu espera por ele num Suspense, já desenhado, só sem o item aceso. */}
          <Suspense fallback={<ShellNav pathname={null} role={role} onNavigate={close} />}>
            <ActiveShellNav role={role} onNavigate={close} />
          </Suspense>
          <div className="flex flex-wrap items-center gap-3 border-t border-border px-2 pt-4 md:mt-auto">
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
        </div>
      </aside>
      <main className="min-w-0 flex-1">
        <SubscriptionBanner className="mx-4 mt-4 sm:mx-7" />
        {children}
      </main>
    </div>
  );
}

function ActiveShellNav({ role, onNavigate }: { role: Role | undefined; onNavigate: () => void }) {
  return <ShellNav pathname={usePathname()} role={role} onNavigate={onNavigate} />;
}

/** "/painel" acende só nele mesmo (as telas de /painel/… têm item próprio). */
function isActive(pathname: string | null, href: string) {
  if (pathname === null) return false;
  if (href === "/painel") return pathname === "/painel";
  return pathname === href || pathname.startsWith(`${href}/`);
}

function ShellNav({
  pathname,
  role,
  onNavigate,
}: {
  pathname: string | null;
  role: Role | undefined;
  onNavigate: () => void;
}) {
  const allowed = (item: Item) => !item.roles || (role !== undefined && item.roles.includes(role));
  const live = useLiveCount(role);
  const groups: { label: string | null; items: Item[] }[] = [
    { label: null, items: LIVE.filter(allowed) },
    { label: t.screens, items: SCREENS.filter(allowed) },
    ...(role && canManageTeam(role) ? [{ label: t.management, items: MANAGEMENT }] : []),
  ];
  return (
    <nav aria-label={t.navigation} className="flex flex-col gap-1">
      {groups.map(({ label, items }) => (
        <div key={label ?? "live"} className="flex flex-col gap-1">
          {label && (
            <span className="px-3 pt-4 pb-1.5 text-[11px] font-semibold tracking-[.4px] text-muted-foreground uppercase">
              {label}
            </span>
          )}
          {items.map(({ href, label: text, icon: Icon }) => {
            const active = isActive(pathname, href);
            return (
              <Link
                key={href}
                href={href}
                onClick={onNavigate}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "flex h-11 shrink-0 items-center gap-3 rounded-xl px-3 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                  active && "bg-secondary text-foreground",
                )}
              >
                <Icon aria-hidden className="size-4" />
                {text}
                {href === "/painel" && live > 0 && (
                  <span
                    aria-label={t.nav.liveCount(live)}
                    className="ml-auto flex h-5.5 min-w-5.5 items-center justify-center rounded-full bg-primary px-1.5 text-xs font-bold text-primary-foreground"
                  >
                    {live}
                  </span>
                )}
              </Link>
            );
          })}
        </div>
      ))}
    </nav>
  );
}
