"use client";

import { useState } from "react";
import type { Schemas } from "@/lib/api/client";
import { messages } from "@/messages/pt-BR";
import { PlanCard } from "./plan-card";

const t = messages.home.plans;

/** A chave mensal/anual e os cartões dos planos (os planos chegam prontos do servidor). */
export function PlanBilling({ plans, heading }: { plans: Schemas["Plan"][]; heading: React.ReactNode }) {
  const [annual, setAnnual] = useState(false);
  const option =
    "h-10 rounded-[10px] px-4 text-sm font-semibold text-muted-foreground aria-pressed:bg-secondary aria-pressed:text-foreground";

  return (
    <>
      <div className="mb-10 flex flex-wrap items-end gap-5">
        <div className="mr-auto">{heading}</div>
        <div role="group" aria-label={t.billingLabel} className="inline-flex gap-1 rounded-[14px] border border-border bg-muted p-1">
          <button type="button" aria-pressed={!annual} onClick={() => setAnnual(false)} className={option}>
            {t.monthly}
          </button>
          <button type="button" aria-pressed={annual} onClick={() => setAnnual(true)} className={option}>
            {t.annual}
          </button>
        </div>
      </div>
      <ul className="grid grid-cols-[repeat(auto-fit,minmax(250px,1fr))] items-stretch gap-4">
        {plans.map((plan) => (
          <li key={plan.id}>
            <PlanCard plan={plan} annual={annual} />
          </li>
        ))}
      </ul>
    </>
  );
}
