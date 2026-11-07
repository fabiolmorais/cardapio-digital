import { Check } from "lucide-react";
import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import type { Schemas } from "@/lib/api/client";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";

const t = messages.plans;
const h = messages.home.plans;

/** O plano do teste grátis (14 dias, decidido no back-end): ganha destaque na vitrine. */
const TRIAL_PLAN_CODE = "pro";

export function PlanCard({ plan, annual }: { plan: Schemas["Plan"]; annual: boolean }) {
  const items = [
    t.tables(plan.maxTables),
    t.users(plan.maxUsers),
    t.sectors(plan.maxSectors),
    t.included,
    ...plan.features.map((feature) => t.features[feature]),
    t.history(plan.historyDays),
    t.support[plan.support],
  ];
  const trial = plan.code === TRIAL_PLAN_CODE;
  const priced = plan.monthlyPriceCents !== null && plan.annualPriceCents !== null;

  return (
    <article
      data-testid={`plan-${plan.code}`}
      aria-label={plan.name}
      className={cn(
        "flex h-full flex-col gap-4.5 rounded-3xl border border-border bg-muted p-6",
        trial && "border-brand-text/55 shadow-[0_0_0_3px_rgba(240,87,58,.14)]",
      )}
    >
      <div className="flex items-center justify-between gap-2">
        <h3 className="text-xl font-semibold">{plan.name}</h3>
        {trial ? (
          <span className="rounded-full bg-primary/15 px-2.5 py-0.5 text-xs font-semibold text-brand-text">{h.trialBadge}</span>
        ) : null}
      </div>
      <div className="flex min-h-16 flex-col gap-1">
        {priced ? (
          <>
            <p>
              <span className="text-4xl font-extrabold tracking-tight">
                {formatCents(annual ? plan.annualPriceCents! : plan.monthlyPriceCents!)}
              </span>
              <span className="text-[15px] text-muted-foreground">{annual ? h.perYearSuffix : t.perMonth}</span>
            </p>
            <p className="text-sm text-muted-foreground">
              {annual
                ? h.annualEquivalent(formatCents(Math.round(plan.annualPriceCents! / 12)))
                : t.perYear(formatCents(plan.annualPriceCents!))}
            </p>
          </>
        ) : (
          <>
            <p className="text-4xl font-extrabold tracking-tight">{t.customPricing}</p>
            <p className="text-sm text-muted-foreground">{h.customNote}</p>
          </>
        )}
      </div>
      <ul className="flex flex-1 flex-col gap-2.5 text-sm">
        {items.map((item) => (
          <li key={item} className="flex gap-2.5">
            <Check aria-hidden className="mt-0.5 size-4 shrink-0 text-brand-text" strokeWidth={2.6} />
            <span>{item}</span>
          </li>
        ))}
      </ul>
      {priced ? (
        <Link
          href="/cadastro"
          className={cn(buttonVariants({ size: "xl", variant: trial ? "default" : "secondary" }), "w-full")}
        >
          {h.start}
        </Link>
      ) : null}
    </article>
  );
}
