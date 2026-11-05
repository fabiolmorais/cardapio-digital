"use client";

import { Check, CreditCard, Minus } from "lucide-react";
import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { PlanChooser } from "./plan-chooser";
import { canSeeSubscription, daysUntil, formatDate, useSubscription, type Subscription } from "./subscription-api";

const t = messages.subscription;

/** A tela "Assinatura" do admin: o plano, a situação, as datas e o uso de cada limite. */
export function SubscriptionPage() {
  const { status, session } = useRequiredSession();
  const access = status === "authenticated" ? session.establishment : null;
  const allowed = access !== null && canSeeSubscription(access.role);
  const subscription = useSubscription(allowed);

  return (
    <div className="flex max-w-3xl flex-col gap-6 px-4 py-6 sm:px-7">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">{t.title}</h1>
        <p className="text-sm text-muted-foreground">{t.subtitle}</p>
      </header>
      {access && !allowed ? (
        <p role="alert" className="text-muted-foreground">
          {messages.team.noPermission}
        </p>
      ) : subscription.isError ? (
        <p role="alert" className="text-destructive">
          {t.loadError}
        </p>
      ) : !subscription.data || !access ? (
        <p className="text-muted-foreground">{t.loading}</p>
      ) : (
        <Overview subscription={subscription.data} timeZone={access.timeZone} isOwner={access.role === "OWNER"} />
      )}
    </div>
  );
}

function Overview({
  subscription,
  timeZone,
  isOwner,
}: {
  subscription: Subscription;
  timeZone: string;
  isOwner: boolean;
}) {
  const { plan, standing, coveredUntil, graceEndsAt, usage } = subscription;
  // Só monta com a assinatura carregada (no navegador): a pré-renderização do Next não lê o relógio.
  const [now] = useState(() => Date.now());

  const date = (iso: string) => formatDate(iso, timeZone);
  const situation = {
    TRIAL: t.trialUntil(date(coveredUntil), t.daysLeft(daysUntil(coveredUntil, now, timeZone))),
    ACTIVE: t.paidUntil(date(coveredUntil)),
    PENDING: t.pending(date(graceEndsAt)),
    SUSPENDED: t.suspended(date(graceEndsAt)),
  }[standing];
  const videos = plan.features.includes("PRODUCT_VIDEOS");

  return (
    <>
      <section
        aria-label={t.plan(plan.name)}
        className="flex flex-col gap-3 rounded-2xl border border-border bg-card p-5"
      >
        <div className="flex flex-wrap items-center gap-3">
          <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-primary/15 text-brand-text">
            <CreditCard aria-hidden className="size-6" />
          </span>
          <h2 className="mr-auto text-xl font-semibold">{t.plan(plan.name)}</h2>
          <Badge variant={standing === "SUSPENDED" ? "destructive" : standing === "PENDING" ? "outline" : "secondary"}>
            {t.standing[standing]}
          </Badge>
        </div>
        <p role="status" className={cn(standing === "SUSPENDED" && "text-destructive")}>
          {situation}
        </p>
        {standing === "TRIAL" && <p className="text-sm text-muted-foreground">{t.trialPlan}</p>}
      </section>

      <section aria-label={t.usage} className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-5">
        <h2 className="text-lg font-semibold">{t.usage}</h2>
        <UsageRow label={t.tables} used={usage.tables} max={plan.maxTables} />
        <UsageRow label={t.users} used={usage.users} max={plan.maxUsers} />
        <UsageRow label={t.sectors} used={usage.sectors} max={plan.maxSectors} />
        <div className="flex items-center justify-between gap-4">
          <span>{t.videos}</span>
          <span className={cn("flex items-center gap-1.5 text-sm", videos ? "text-success" : "text-muted-foreground")}>
            {videos ? <Check aria-hidden className="size-4" /> : <Minus aria-hidden className="size-4" />}
            {videos ? t.videosIncluded : t.videosNotIncluded}
          </span>
        </div>
      </section>

      <PlanChooser subscription={subscription} isOwner={isOwner} timeZone={timeZone} />
    </>
  );
}

function UsageRow({ label, used, max }: { label: string; used: number; max: number | null }) {
  const full = max !== null && used >= max;
  const percent = max === null || max === 0 ? 0 : Math.min(100, Math.round((used / max) * 100));
  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex items-center justify-between gap-4">
        <span>{label}</span>
        <span className={cn("text-sm tabular-nums", full ? "font-semibold text-destructive" : "text-muted-foreground")}>
          {t.of(used, max)}
        </span>
      </div>
      {max !== null && (
        <div aria-hidden className="h-2 overflow-hidden rounded-full bg-muted">
          <div
            className={cn("h-full rounded-full", full ? "bg-destructive" : "bg-primary")}
            style={{ width: `${percent}%` }}
          />
        </div>
      )}
    </div>
  );
}
