import { api, type Schemas } from "@/lib/api/client";

export type CounterTab = Schemas["OpenTab"];

/** A mensagem do back (Problem Details) ou o código HTTP. */
function problem(error: unknown, status: number): Error {
  const detail = (error as { detail?: unknown } | undefined)?.detail;
  return new Error(typeof detail === "string" ? detail : String(status));
}

/** As comandas abertas de um balcão, da mais antiga para a mais nova. */
export async function fetchCounterTabs(counterId: string): Promise<CounterTab[]> {
  const { data, error, response } = await api.GET("/api/v1/counter-tabs", {
    params: { query: { counterId, size: 100 } },
  });
  if (!data) throw problem(error, response.status);
  return data.content;
}

/** Abre a comanda de um cliente do balcão (ADR 0015). */
export async function openCounterTab(counterId: string, name: string, card: string): Promise<CounterTab> {
  const { data, error, response } = await api.POST("/api/v1/counter-tabs", {
    body: { counterId, name, card: card || undefined },
  });
  if (!data) throw problem(error, response.status);
  return data;
}

/** Corrige o nome ou o cartão da comanda aberta. */
export async function renameCounterTab(tabId: string, name: string, card: string): Promise<CounterTab> {
  const { data, error, response } = await api.PATCH("/api/v1/counter-tabs/{id}", {
    params: { path: { id: tabId } },
    body: { name, card: card || undefined },
  });
  if (!data) throw problem(error, response.status);
  return data;
}

/** A chave da lista de comandas de um balcão (o tempo real invalida todas com "counter-tabs"). */
export function counterTabKey(counterId: string) {
  return ["counter-tabs", counterId] as const;
}
