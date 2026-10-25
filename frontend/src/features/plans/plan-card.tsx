import { Check } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { Schemas } from "@/lib/api/client";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";

const t = messages.plans;

export function PlanCard({ plan }: { plan: Schemas["Plan"] }) {
  const items = [
    t.tables(plan.maxTables),
    t.users(plan.maxUsers),
    t.sectors(plan.maxSectors),
    t.included,
    ...plan.features.map((feature) => t.features[feature]),
    t.history(plan.historyDays),
    t.support[plan.support],
  ];

  return (
    <Card data-testid={`plan-${plan.code}`} className="h-full">
      <CardHeader>
        <CardTitle className="text-xl">{plan.name}</CardTitle>
        {plan.monthlyPriceCents !== null && plan.annualPriceCents !== null ? (
          <div>
            <p className="text-3xl font-semibold">
              {formatCents(plan.monthlyPriceCents)}
              <span className="text-base font-normal text-muted-foreground">{t.perMonth}</span>
            </p>
            <p className="text-sm text-muted-foreground">
              {t.perYear(formatCents(plan.annualPriceCents))}
            </p>
          </div>
        ) : (
          <p className="text-3xl font-semibold">{t.customPricing}</p>
        )}
      </CardHeader>
      <CardContent>
        <ul className="space-y-2 text-sm">
          {items.map((item) => (
            <li key={item} className="flex gap-2">
              <Check aria-hidden className="mt-0.5 size-4 shrink-0 text-primary" />
              <span>{item}</span>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
}
