"use client";

import { AlertTriangle } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { useSession } from "@/lib/auth/use-session";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { canSeeSubscription, daysUntil, formatDate, useSubscription, type Subscription } from "./subscription-api";

const t = messages.subscription;

/** No teste, o aviso aparece nos 3 últimos dias. */
const TRIAL_WARNING_DAYS = 3;

/**
 * O aviso do topo do admin e do painel: o fim do teste chegando, a assinatura pendente (na
 * tolerância) ou suspensa. Só para dono e gerente; com tudo em dia, não aparece.
 */
export function SubscriptionBanner({ className }: { className?: string }) {
  const state = useSession();
  const access = state.status === "authenticated" ? state.session.establishment : null;
  const subscription = useSubscription(access !== null && canSeeSubscription(access.role));
  if (!access || !subscription.data) return null;
  return <Banner subscription={subscription.data} timeZone={access.timeZone} className={className} />;
}

/** Só monta com a assinatura carregada (no navegador): a pré-renderização do Next não lê o relógio. */
function Banner({
  subscription,
  timeZone,
  className,
}: {
  subscription: Subscription;
  timeZone: string;
  className?: string;
}) {
  const [now] = useState(() => Date.now());
  const { standing, coveredUntil, graceEndsAt } = subscription;
  const days = daysUntil(coveredUntil, now, timeZone);

  let text: string | null = null;
  if (standing === "TRIAL" && days <= TRIAL_WARNING_DAYS) text = t.banner.trialEnding(days);
  if (standing === "PENDING") text = t.banner.pending(formatDate(graceEndsAt, timeZone));
  if (standing === "SUSPENDED") text = t.banner.suspended;
  if (!text) return null;

  return (
    <div
      role="status"
      className={cn(
        "flex items-start gap-3 rounded-xl border px-4 py-3 text-sm",
        standing === "SUSPENDED"
          ? "border-destructive/40 bg-destructive/10 text-destructive"
          : "border-primary/40 bg-primary/10 text-foreground",
        className,
      )}
    >
      <AlertTriangle aria-hidden className="mt-0.5 size-4 shrink-0" />
      <span className="flex flex-1 flex-wrap items-center gap-x-3 gap-y-1">
        <span className="mr-auto">{text}</span>
        <Link href="/admin/assinatura" className="font-semibold underline underline-offset-4">
          {t.see}
        </Link>
      </span>
    </div>
  );
}
