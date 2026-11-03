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
export async function closeTab(id: string, paymentMethod: PaymentMethod): Promise<StaffTab> {
  const { data, error, response } = await api.POST("/api/v1/tabs/{id}/close", {
    params: { path: { id } },
    body: { paymentMethod },
  });
  if (!data) throw problem(error, response.status);
  return data;
}
