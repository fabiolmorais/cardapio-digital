import { api, type Schemas } from "@/lib/api/client";

export type OpenTab = Schemas["OpenTab"];
export type StaffTab = Schemas["StaffTab"];
export type PaymentMethod = NonNullable<StaffTab["paymentMethod"]>;

/** A mensagem do back (Problem Details) ou o código HTTP. */
function problem(error: unknown, status: number): Error {
  const detail = (error as { detail?: unknown } | undefined)?.detail;
  return new Error(typeof detail === "string" ? detail : String(status));
}

/** As comandas abertas, da mais antiga à mais nova (uma página de 100 basta para o salão). */
export async function fetchOpenTabs(): Promise<OpenTab[]> {
  const { data, error, response } = await api.GET("/api/v1/tabs/open", { params: { query: { page: 0, size: 100 } } });
  if (!data) throw problem(error, response.status);
  return data.content;
}

export async function fetchTab(id: string): Promise<StaffTab> {
  const { data, error, response } = await api.GET("/api/v1/tabs/{id}", { params: { path: { id } } });
  if (!data) throw problem(error, response.status);
  return data;
}

/** Fecha depois do pagamento. 409 traz a mensagem do back (os itens a caminho, por exemplo). */
/** Fecha a comanda; sem forma de pagamento quando a mesa já pagou tudo adiantado. */
export async function closeTab(
  id: string,
  paymentMethod: PaymentMethod | undefined,
  service?: { serviceAccepted: boolean; serviceCustomCents?: number },
): Promise<StaffTab> {
  const { data, error, response } = await api.POST("/api/v1/tabs/{id}/close", {
    params: { path: { id } },
    body: { paymentMethod, ...service },
  });
  if (!data) throw problem(error, response.status);
  return data;
}

export type MovedTab = Schemas["MovedTab"];

/** Muda a comanda para outra mesa; se a mesa tem comanda aberta, as duas se juntam (ADR 0012). */
export async function moveTab(id: string, tableId: string): Promise<MovedTab> {
  const { data, error, response } = await api.POST("/api/v1/tabs/{id}/move", {
    params: { path: { id } },
    body: { tableId },
  });
  if (!data) throw problem(error, response.status);
  return data;
}

/** Quantas pessoas estão na mesa (fora do rodízio), informado pelo garçom; null se ninguém informou. */
export async function fetchTabPeople(id: string): Promise<number | null> {
  const { data, error, response } = await api.GET("/api/v1/tabs/{id}/people", { params: { path: { id } } });
  if (!data) throw problem(error, response.status);
  return data.people ?? null;
}

export async function saveTabPeople(id: string, people: number | null): Promise<number | null> {
  const { data, error, response } = await api.PUT("/api/v1/tabs/{id}/people", {
    params: { path: { id } },
    body: { people: people ?? undefined },
  });
  if (!data) throw problem(error, response.status);
  return data.people ?? null;
}
