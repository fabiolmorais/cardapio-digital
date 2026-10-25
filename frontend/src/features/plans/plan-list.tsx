import { connection } from "next/server";
import { api } from "@/lib/api/client";
import { messages } from "@/messages/pt-BR";
import { PlanCard } from "./plan-card";

/** Busca os planos na API a cada requisição (renderizado no servidor). */
export async function PlanList() {
  await connection();
  const { data, error } = await api
    .GET("/api/v1/plans", { params: { query: { size: 20 } } })
    .catch(() => ({ data: undefined, error: true }));

  if (error || !data) {
    return (
      <p role="alert" className="text-muted-foreground">
        {messages.home.plansError}
      </p>
    );
  }

  return (
    <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
      {data.content.map((plan) => (
        <li key={plan.id}>
          <PlanCard plan={plan} />
        </li>
      ))}
    </ul>
  );
}
