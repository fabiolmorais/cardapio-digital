import { connection } from "next/server";
import type { ReactNode } from "react";
import { api } from "@/lib/api/client";
import { messages } from "@/messages/pt-BR";
import { PlanBilling } from "./plan-billing";

/** Busca os planos na API a cada requisição (renderizado no servidor). */
export async function PlanList({ heading }: { heading: ReactNode }) {
  await connection();
  const { data, error } = await api
    .GET("/api/v1/plans", { params: { query: { size: 20 } } })
    .catch(() => ({ data: undefined, error: true }));

  if (error || !data) {
    return (
      <>
        <div className="mb-10">{heading}</div>
        <p role="alert" className="text-muted-foreground">
          {messages.home.plansError}
        </p>
      </>
    );
  }

  return <PlanBilling plans={data.content} heading={heading} />;
}
