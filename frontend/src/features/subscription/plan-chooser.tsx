"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check } from "lucide-react";
import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { api, type Schemas } from "@/lib/api/client";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { formatDate, type Subscription } from "./subscription-api";

const t = messages.subscription;
const tp = messages.plans;

type Cycle = Schemas["PlanRequestRequest"]["billingCycle"];
type Plan = Schemas["Plan"];

async function fetchPlans(): Promise<Plan[]> {
  const { data, response } = await api.GET("/api/v1/plans", { params: { query: { page: 0, size: 20 } } });
  if (!data) throw new Error(String(response.status));
  return data.content;
}

async function requestPlan(planCode: string, billingCycle: Cycle): Promise<Subscription> {
  const { data, error, response } = await api.PUT("/api/v1/subscription/plan-request", {
    body: { planCode, billingCycle },
  });
  if (data) return data;
  const problem = error as { detail?: string } | undefined;
  throw new Error(response.status === 422 && problem?.detail ? problem.detail : t.pickError);
}

/**
 * "Assinar ou mudar de plano": os planos com o preço no ciclo escolhido. O dono escolhe; a
 * escolha fica à espera do pagamento (combinado por fora até a cobrança automática).
 */
export function PlanChooser({
  subscription,
  isOwner,
  timeZone,
}: {
  subscription: Subscription;
  isOwner: boolean;
  timeZone: string;
}) {
  const queryClient = useQueryClient();
  const plans = useQuery({ queryKey: ["plans"], queryFn: fetchPlans, retry: false });
  const request = subscription.request;
  const [cycle, setCycle] = useState<Cycle>(request?.billingCycle ?? "MONTHLY");
  const pick = useMutation({
    mutationFn: (planCode: string) => requestPlan(planCode, cycle),
    onSuccess: (updated) => queryClient.setQueryData(["subscription"], updated),
  });

  return (
    <section aria-label={t.choose} className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-5">
      <div className="flex flex-col gap-1">
        <h2 className="text-lg font-semibold">{t.choose}</h2>
        <p className="text-sm text-muted-foreground">{isOwner ? t.chooseHint : t.ownerOnly}</p>
      </div>

      {request && (
        <p role="status" className="rounded-xl bg-primary/10 px-4 py-3 text-sm">
          {t.requested(request.planName, t.cycleNames[request.billingCycle], formatDate(request.requestedAt, timeZone))}
        </p>
      )}

      <div role="group" aria-label={t.cycleLabel} className="flex gap-2">
        {(["MONTHLY", "ANNUAL"] as const).map((c) => (
          <Button
            key={c}
            variant={cycle === c ? "default" : "outline"}
            aria-pressed={cycle === c}
            className="h-10"
            onClick={() => setCycle(c)}
          >
            {t.cycles[c]}
          </Button>
        ))}
      </div>

      {plans.isError && (
        <p role="alert" className="text-sm text-destructive">
          {t.plansError}
        </p>
      )}
      {plans.data && (
        <ul className="grid gap-3 sm:grid-cols-2">
          {plans.data.map((plan) => {
            const price = cycle === "MONTHLY" ? plan.monthlyPriceCents : plan.annualPriceCents;
            const current = plan.code === subscription.plan.code;
            const chosen = request?.planCode === plan.code && request.billingCycle === cycle;
            return (
              <li
                key={plan.id}
                aria-label={plan.name}
                className={cn("flex flex-col gap-3 rounded-xl border p-4", chosen ? "border-primary" : "border-border")}
              >
                <div className="flex flex-wrap items-center gap-2">
                  <span className="mr-auto text-lg font-semibold">{plan.name}</span>
                  {current && <Badge variant="secondary">{t.current}</Badge>}
                  {chosen && (
                    <Badge>
                      <Check aria-hidden />
                      {t.chosen}
                    </Badge>
                  )}
                </div>
                {price === null ? (
                  <p className="text-sm text-muted-foreground">{t.byQuote}</p>
                ) : (
                  <p className="text-2xl font-semibold">
                    {formatCents(price)}
                    <span className="text-sm font-normal text-muted-foreground">
                      {cycle === "MONTHLY" ? t.perMonth : t.perYear}
                    </span>
                  </p>
                )}
                <ul className="flex flex-col gap-1 text-sm text-muted-foreground">
                  <li>{tp.tables(plan.maxTables)}</li>
                  <li>{tp.users(plan.maxUsers)}</li>
                  <li>{tp.sectors(plan.maxSectors)}</li>
                  {plan.features.includes("PRODUCT_VIDEOS") && <li>{tp.features.PRODUCT_VIDEOS}</li>}
                </ul>
                {isOwner && price !== null && !chosen && (
                  <Button
                    variant="outline"
                    className="mt-auto h-11"
                    disabled={pick.isPending}
                    onClick={() => pick.mutate(plan.code)}
                  >
                    {pick.isPending && pick.variables === plan.code ? t.picking : t.pick(plan.name)}
                  </Button>
                )}
              </li>
            );
          })}
        </ul>
      )}
      {pick.isError && (
        <p role="alert" className="text-sm text-destructive">
          {pick.error.message}
        </p>
      )}
    </section>
  );
}
