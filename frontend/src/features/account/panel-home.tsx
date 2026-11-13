"use client";

import { Tv } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { Button, buttonVariants } from "@/components/ui/button";
import { CallQueue } from "@/features/calls/call-queue";
import { useLiveCalls } from "@/features/calls/use-live-calls";
import { useMinuteClock } from "@/features/live/clock";
import { sectorsOf } from "@/features/live/live-api";
import { SoundGate } from "@/features/live/sound-gate";
import { SoundTag } from "@/features/live/sound-tag";
import { useLiveFloor, useStaffConnected } from "@/features/live/use-live-floor";
import { KITCHEN_KEY, KitchenLink, OrdersColumn, ReadyColumn } from "@/features/orders/staff-orders";
import { fetchKitchenQueue } from "@/features/orders/orders-api";
import { useLiveOrders } from "@/features/orders/use-live-orders";
import { PrintingLink } from "@/features/printing/printing-link";
import { PushTag } from "@/features/pwa/push-tag";
import { canManageTeam } from "@/features/team/team-rules";
import { useMe } from "./use-me";
import { formatCents } from "@/lib/format";
import { useLogout } from "@/lib/auth/use-logout";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";

const t = messages.panel;
const tl = t.live;

const dateFormat = new Intl.DateTimeFormat("pt-BR", { weekday: "long", day: "numeric", month: "long" });
const timeFormat = new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit" });

/**
 * O "Ao vivo" (desenho 7.1): no topo, a data e a hora, o filtro por setor, o "Som ligado" e o
 * "Abrir Modo TV"; depois os números do dia (no celular, só para o dono e o gerente) e as três colunas (Chamados, Pedidos e Prontos para
 * entregar). No celular, uma coluna só, na mesma ordem. A cozinha vê os atalhos dela primeiro.
 * Confirma no back (GET /api/v1/me) quem está logado.
 */
export function PanelHome() {
  const { status } = useRequiredSession();
  const { leaving, logout } = useLogout();
  const me = useMe(status === "authenticated");

  if (status !== "authenticated" || me.isPending) {
    return <p className="px-4 py-6 text-muted-foreground sm:px-7">{t.loading}</p>;
  }

  if (me.isError) {
    return (
      <div className="flex flex-col items-start gap-4 px-4 py-6 sm:px-7">
        <p role="alert" className="text-destructive">
          {t.error}
        </p>
        <Button variant="outline" onClick={logout} disabled={leaving}>
          {t.logout}
        </Button>
      </div>
    );
  }

  return <Live role={me.data.establishment.role} />;
}

type Role = "OWNER" | "MANAGER" | "STAFF" | "KITCHEN";

function Live({ role }: { role: Role }) {
  const [sector, setSector] = useState<string | null>(null);
  const floor = useLiveFloor();
  const { queue } = useLiveCalls();
  const kitchen = useLiveOrders(KITCHEN_KEY, () => fetchKitchenQueue());
  const connected = useStaffConnected();
  const now = useMinuteClock();
  const kitchenRole = role === "KITCHEN";

  const tables = floor.data?.tables ?? [];
  const sectors = sectorsOf(tables);
  // O setor escolhido pode sumir (desativado em outra tela): aí volta para "Todos".
  const current = sector !== null && sectors.includes(sector) ? sector : null;
  const inSector = (name: string) => current === null || name === current;
  const shownTables = tables.filter((table) => inSector(table.sectorName));
  const occupied = shownTables.filter((table) => table.state !== "FREE").length;
  const openCalls = (queue.data ?? []).filter((call) => inSector(call.sectorName)).length;
  const cooking = (kitchen.list.data ?? []).filter((i) => i.status !== "READY" && inSector(i.sectorName));
  const cookingCount = cooking.reduce((sum, i) => sum + i.quantity, 0);
  const revenue = floor.data?.revenueTodayCents;

  const date = now ? dateFormat.format(now) : "";
  const time = now ? timeFormat.format(now) : "";

  return (
    <>
      <header className="flex flex-wrap items-center gap-3 border-b border-border px-4 py-4 sm:px-7">
        <div className="mr-auto flex flex-col gap-0.5">
          <h1 className="text-[28px] leading-[34px] font-bold tracking-tight">{tl.title}</h1>
          <span role="status" className="min-h-[18px] text-[13px] text-muted-foreground first-letter:uppercase">
            {now && (connected ? tl.subtitle(date, time) : tl.reconnecting(date, time))}
          </span>
        </div>
        {sectors.length > 1 && (
          <div
            role="group"
            aria-label={tl.sectors}
            className="flex flex-wrap gap-1.5 rounded-[14px] border border-border bg-card p-1"
          >
            {[null, ...sectors].map((name) => (
              <button
                key={name ?? "todos"}
                type="button"
                aria-pressed={current === name}
                onClick={() => setSector(name)}
                className={cn(
                  "h-9 rounded-[10px] px-3 text-[13px] font-semibold text-muted-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                  current === name && "bg-foreground text-background",
                )}
              >
                {name ?? tl.all}
              </button>
            ))}
          </div>
        )}
        <SoundTag />
        <PushTag />
        {canManageTeam(role) && (
          <Link href="/painel/tv" className={cn(buttonVariants({ variant: "outline" }), "h-11 gap-2")}>
            <Tv aria-hidden />
            {t.calls.openTv}
          </Link>
        )}
      </header>

      <div className="flex flex-col gap-5 px-4 py-6 sm:px-7">
        {kitchenRole && (
          <div className="grid gap-4 lg:grid-cols-2">
            <KitchenLink />
            <PrintingLink />
          </div>
        )}

        {/* No celular, os números do dia são só para o dono e o gerente: o garçom e a cozinha vão
            direto aos chamados e pedidos. No computador, todos veem. */}
        <section
          aria-label={tl.summary}
          className={cn("grid grid-cols-2 gap-3 lg:grid-cols-4", !canManageTeam(role) && "max-md:hidden")}
        >
          <Kpi label={tl.occupied}>
            {floor.data ? occupied : "–"}{" "}
            <span className="text-sm font-normal text-muted-foreground">{tl.of(shownTables.length)}</span>
          </Kpi>
          <Kpi label={tl.openCalls} className="text-brand-text">
            {queue.data ? openCalls : "–"}
          </Kpi>
          <Kpi label={tl.kitchenItems}>{kitchen.list.data ? cookingCount : "–"}</Kpi>
          {revenue !== undefined && revenue !== null ? (
            <Kpi label={tl.revenue} hint={tl.revenueHint} className="font-mono">
              {formatCents(revenue)}
            </Kpi>
          ) : (
            <Kpi
              label={tl.averageWait}
              hint={floor.data?.averageWaitSeconds == null ? tl.noWait : undefined}
              className="font-mono"
            >
              {floor.data?.averageWaitSeconds == null ? "–" : minutes(floor.data.averageWaitSeconds)}
            </Kpi>
          )}
        </section>

        <div className="grid items-start gap-4 lg:grid-cols-2 xl:grid-cols-3">
          <CallQueue role={role} sector={current} />
          <OrdersColumn sector={current} canReview={!kitchenRole} />
          <ReadyColumn sector={current} canDeliver={!kitchenRole} />
        </div>
      </div>
      <SoundGate />
    </>
  );
}

/** 130 s → "2:10". */
function minutes(seconds: number) {
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`;
}

function Kpi({
  label,
  hint,
  className,
  children,
}: {
  label: string;
  hint?: string;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-1.5 rounded-[20px] border border-border bg-card px-[18px] py-4">
      <span className="text-[13px] text-muted-foreground">{label}</span>
      <span className={cn("text-[28px] leading-[34px] font-bold tracking-tight", className)}>{children}</span>
      {hint && <span className="text-[13px] text-muted-foreground/80">{hint}</span>}
    </div>
  );
}
