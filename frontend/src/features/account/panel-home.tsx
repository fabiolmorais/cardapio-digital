"use client";

import { useQuery } from "@tanstack/react-query";
import { BookOpen, ChevronRight, LogOut, QrCode, Store, Users } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api/client";
import { useLogout } from "@/lib/auth/use-logout";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { canManageTeam } from "@/features/team/team-rules";
import { messages } from "@/messages/pt-BR";

const t = messages.panel;

const SHORTCUTS = [
  { href: "/admin/mesas", icon: QrCode, text: t.manage.floor },
  { href: "/admin/cardapio", icon: BookOpen, text: t.manage.menu },
  { href: "/admin/equipe", icon: Users, text: t.manage.team },
] as const;

async function fetchMe() {
  const { data, response } = await api.GET("/api/v1/me");
  if (!data) throw new Error(String(response.status));
  return data;
}

/**
 * Primeira tela depois do login: confirma no back (GET /api/v1/me) quem está logado.
 * Depois de um F5, recupera a sessão pelo cookie; sem sessão, volta para /entrar.
 */
export function PanelHome() {
  const { status } = useRequiredSession();
  const { leaving, logout } = useLogout();
  const me = useQuery({
    queryKey: ["me"],
    queryFn: fetchMe,
    enabled: status === "authenticated",
    retry: false,
  });

  if (status !== "authenticated" || me.isPending) {
    return <p className="text-muted-foreground">{t.loading}</p>;
  }

  if (me.isError) {
    return (
      <div className="flex flex-col items-start gap-4">
        <p role="alert" className="text-destructive">
          {t.error}
        </p>
        <Button variant="outline" onClick={logout} disabled={leaving}>
          {t.logout}
        </Button>
      </div>
    );
  }

  const { name, establishment } = me.data;

  return (
    <>
      <header className="flex items-start justify-between gap-4">
        <h1 className="text-3xl font-bold tracking-tight">{t.greeting(name)}</h1>
        <Button variant="outline" onClick={logout} disabled={leaving}>
          <LogOut aria-hidden />
          {t.logout}
        </Button>
      </header>
      <section className="flex items-center gap-4 rounded-2xl border border-border bg-card p-5">
        <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-primary/15 text-brand-text">
          <Store aria-hidden className="size-6" />
        </span>
        <div className="flex min-w-0 flex-col">
          <span className="text-sm text-muted-foreground">{t.workingAt}</span>
          <span className="truncate text-lg font-semibold">{establishment.name}</span>
          <span className="text-sm text-muted-foreground">
            {t.roleLabel}: {t.roles[establishment.role]}
          </span>
        </div>
      </section>
      {canManageTeam(establishment.role) && (
        <nav aria-label={t.shortcuts} className="flex flex-col gap-3">
          {SHORTCUTS.map(({ href, icon: Icon, text }) => (
            <Link
              key={href}
              href={href}
              className="flex min-h-16 items-center gap-4 rounded-2xl border border-border bg-card p-5 transition-colors hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
            >
              <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-secondary text-foreground">
                <Icon aria-hidden className="size-6" />
              </span>
              <span className="flex min-w-0 flex-1 flex-col">
                <span className="text-lg font-semibold">{text.title}</span>
                <span className="text-sm text-muted-foreground">{text.description}</span>
              </span>
              <ChevronRight aria-hidden className="size-5 text-muted-foreground" />
            </Link>
          ))}
        </nav>
      )}
      <p className="text-muted-foreground">{t.comingSoon}</p>
    </>
  );
}
