import { api, type Schemas } from "@/lib/api/client";

export type KitchenItem = Schemas["KitchenItem"];
export type WaitingOrder = Schemas["WaitingOrder"];
export type ItemStep = "start" | "ready" | "deliver";

/** A mensagem do back (Problem Details) ou o código HTTP. */
function problem(error: unknown, status: number): Error {
  const detail = (error as { detail?: unknown } | undefined)?.detail;
  return new Error(typeof detail === "string" ? detail : String(status));
}

/**
 * A fila da cozinha: os itens a caminho, do mais antigo ao mais novo. Com `status`, só esses
 * ("READY" é a lista de entregas do garçom). Uma página de 100 basta para o salão.
 */
export async function fetchKitchenQueue(status?: KitchenItem["status"]): Promise<KitchenItem[]> {
  const { data, error, response } = await api.GET("/api/v1/order-items", {
    params: { query: { page: 0, size: 100, ...(status && { status: [status] }) } },
  });
  if (!data) throw problem(error, response.status);
  return data.content;
}

/** Em preparo, pronto ou entregue. */
export async function stepItem(id: string, step: ItemStep): Promise<void> {
  const path = { params: { path: { id } } };
  const { error, response } =
    step === "start"
      ? await api.POST("/api/v1/order-items/{id}/start", path)
      : step === "ready"
        ? await api.POST("/api/v1/order-items/{id}/ready", path)
        : await api.POST("/api/v1/order-items/{id}/deliver", path);
  if (!response.ok) throw problem(error, response.status);
}

/** Cancela, com o motivo que o cliente vê. */
export async function cancelItem(id: string, reason: string): Promise<void> {
  const { error, response } = await api.POST("/api/v1/order-items/{id}/cancel", {
    params: { path: { id } },
    body: { reason },
  });
  if (!response.ok) throw problem(error, response.status);
}

/** Os pedidos esperando a confirmação do garçom. */
export async function fetchWaitingOrders(): Promise<WaitingOrder[]> {
  const { data, error, response } = await api.GET("/api/v1/orders/awaiting-approval", {
    params: { query: { page: 0, size: 100 } },
  });
  if (!data) throw problem(error, response.status);
  return data.content;
}

export async function reviewOrder(id: string, approve: boolean): Promise<void> {
  const path = { params: { path: { id } } };
  const { error, response } = approve
    ? await api.POST("/api/v1/orders/{id}/approve", path)
    : await api.POST("/api/v1/orders/{id}/reject", path);
  if (!response.ok) throw problem(error, response.status);
}
