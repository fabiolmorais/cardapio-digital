"use client";

import { TriangleAlert } from "lucide-react";
import Link from "next/link";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { canSeeSubscription, useSubscription } from "./subscription-api";

const t = messages.subscription.meter;

/**
 * "Plano Essencial · 18 de 20 mesas", com a barra (desenhos 12.1 e 12.5). Some se o plano não
 * limita, ou para quem não vê a assinatura. Cheio, a barra fica âmbar.
 */
export function PlanMeter({ kind, role, used: counted }: { kind: "tables" | "users"; role: string; used?: number }) {
  const { data } = useSubscription(canSeeSubscription(role));
  if (!data) return null;
  const max = kind === "tables" ? data.plan.maxTables : data.plan.maxUsers;
  if (max === null || max === undefined) return null;
  // A tela pode passar a conta que ela já tem na mão (a lista de mesas): fica certa na hora.
  const used = counted ?? (kind === "tables" ? data.usage.tables : data.usage.users);
  const full = used >= max;
  const percent = max === 0 ? 100 : Math.min(100, Math.round((used / max) * 100));
  return (
    <div className="flex w-56 flex-col gap-2">
      <div className="flex items-baseline justify-between gap-3 text-[13px]">
        <span className="text-muted-foreground">{kind === "tables" ? t.plan(data.plan.name) : t.users}</span>
        <span className="font-semibold tabular-nums">{t.count(kind, used, max)}</span>
      </div>
      <div
        role="meter"
        aria-label={kind === "tables" ? t.tablesLabel : t.usersLabel}
        aria-valuemin={0}
        aria-valuemax={max}
        aria-valuenow={used}
        className="h-1.5 overflow-hidden rounded-full bg-muted"
      >
        <div
          className={cn("h-full rounded-full", full ? "bg-amber-400" : "bg-primary")}
          style={{ width: `${percent}%` }}
        />
      </div>
    </div>
  );
}

/**
 * O aviso do desenho 12.5: no limite do plano, antes de a pessoa tentar adicionar e levar o erro.
 * Para adicionar mais gente, ela desliga alguém ou muda de plano.
 */
export function PlanLimitBanner({ kind, role }: { kind: "tables" | "users"; role: string }) {
  const { data } = useSubscription(canSeeSubscription(role));
  if (!data) return null;
  const max = kind === "tables" ? data.plan.maxTables : data.plan.maxUsers;
  const used = kind === "tables" ? data.usage.tables : data.usage.users;
  if (max === null || max === undefined || used < max) return null;
  return (
    <p
      role="status"
      className="flex items-start gap-2.5 rounded-xl border border-amber-400/30 bg-amber-400/10 px-4 py-3 text-sm text-amber-100"
    >
      <TriangleAlert aria-hidden className="mt-0.5 size-4 shrink-0 text-amber-400" />
      <span>
        {t.limit(kind, max, data.plan.name)}{" "}
        <Link href="/admin/assinatura" className="font-semibold text-amber-300 underline underline-offset-4">
          {t.changePlan}
        </Link>
      </span>
    </p>
  );
}
