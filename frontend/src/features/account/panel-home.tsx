"use client";

import { useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { CallQueue } from "@/features/calls/call-queue";
import { Deliveries, KitchenLink, WaitingOrders } from "@/features/orders/staff-orders";
import { PrintingLink } from "@/features/printing/printing-link";
import { PushCard } from "@/features/pwa/push-card";
import { OpenTabs } from "@/features/tabs/open-tabs";
import { api } from "@/lib/api/client";
import { useLogout } from "@/lib/auth/use-logout";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { messages } from "@/messages/pt-BR";

const t = messages.panel;

const today = new Intl.DateTimeFormat("pt-BR", { weekday: "long", day: "numeric", month: "long" });

async function fetchMe() {
  const { data, response } = await api.GET("/api/v1/me");
  if (!data) throw new Error(String(response.status));
  return data;
}

/**
 * O "Ao vivo" (desenho 7.1): confirma no back (GET /api/v1/me) quem está logado e mostra a
 * operação em colunas no computador: os chamados; os pedidos para confirmar e as comandas
 * abertas; as entregas e os avisos no celular. No celular, uma coluna só, na mesma ordem.
 * A cozinha vê os atalhos das telas dela primeiro. O menu (telas e gestão) fica na moldura.
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
      <header className="flex flex-col gap-1">
        <h1 className="text-[28px] leading-[34px] font-bold tracking-tight">{t.greeting(name)}</h1>
        <p className="text-sm text-muted-foreground first-letter:uppercase">{t.liveNow(today.format(new Date()))}</p>
      </header>
      {establishment.role === "KITCHEN" ? (
        <div className="grid items-start gap-5 lg:grid-cols-2">
          <div className="flex flex-col gap-4">
            <KitchenLink />
            <PrintingLink />
          </div>
          <CallQueue role={establishment.role} />
        </div>
      ) : (
        <div className="grid items-start gap-5 lg:grid-cols-2 xl:grid-cols-3">
          <div className="flex min-w-0 flex-col gap-5">
            <CallQueue role={establishment.role} />
          </div>
          <div className="flex min-w-0 flex-col gap-5">
            <WaitingOrders />
            <OpenTabs />
          </div>
          <div className="flex min-w-0 flex-col gap-5 lg:col-span-2 xl:col-span-1">
            <Deliveries />
            <PushCard />
          </div>
        </div>
      )}
    </>
  );
}
