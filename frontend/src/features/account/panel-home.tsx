"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { LogOut, Store } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api/client";
import { endSession, restoreSession } from "@/lib/auth/session";
import { useSession } from "@/lib/auth/use-session";
import { messages } from "@/messages/pt-BR";

const t = messages.panel;

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
  const router = useRouter();
  const { status } = useSession();
  const queryClient = useQueryClient();
  const [leaving, setLeaving] = useState(false);
  const me = useQuery({
    queryKey: ["me"],
    queryFn: fetchMe,
    enabled: status === "authenticated",
    retry: false,
  });

  useEffect(() => {
    if (status === "unknown") void restoreSession();
    if (status === "anonymous") {
      queryClient.clear();
      router.replace("/entrar");
    }
  }, [status, router, queryClient]);

  async function logout() {
    setLeaving(true);
    await endSession();
    queryClient.clear();
    // Recarrega a página inteira: nada desta conta (dados, telas guardadas) fica na memória
    // para a próxima pessoa que usar o aparelho.
    window.location.replace("/entrar");
  }

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
      <p className="text-muted-foreground">{t.comingSoon}</p>
    </>
  );
}
