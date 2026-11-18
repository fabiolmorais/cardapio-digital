import { api, type Schemas } from "@/lib/api/client";

export type StaffOrderSheet = Schemas["StaffOrderSheet"];
export type AlreadyOrderedItem = Schemas["AlreadyOrderedItem"];
export type PlaceStaffOrder = Schemas["PlaceStaffOrderRequest"];

/** A mesa já pediu estes itens: o garçom tira do pedido ou confirma que é outro (ADR 0011). */
export class DuplicateOrderError extends Error {
  constructor(
    message: string,
    readonly duplicates: AlreadyOrderedItem[],
  ) {
    super(message);
  }
}

/** A mensagem do back (Problem Details) ou o código HTTP. */
function problem(error: unknown, status: number): Error {
  const detail = (error as { detail?: unknown } | undefined)?.detail;
  return new Error(typeof detail === "string" ? detail : String(status));
}

/** Se o lançamento está liberado, o que a mesa já pediu e o rodízio dela. */
export async function fetchStaffOrderSheet(tableId: string): Promise<StaffOrderSheet> {
  const { data, error, response } = await api.GET("/api/v1/tables/{id}/staff-order", {
    params: { path: { id: tableId } },
  });
  if (!data) throw problem(error, response.status);
  return data;
}

/** O cardápio do garçom: preços e opções, sem fotos e sem vídeos. */
export async function fetchStaffMenu(): Promise<Schemas["PublicMenu"]> {
  const { data, error, response } = await api.GET("/api/v1/staff-menu");
  if (!data) throw problem(error, response.status);
  return data;
}

export async function placeStaffOrder(tableId: string, body: PlaceStaffOrder): Promise<Schemas["StaffOrder"]> {
  const { data, error, response } = await api.POST("/api/v1/tables/{id}/orders", {
    params: { path: { id: tableId } },
    body,
  });
  if (data) return data;
  const duplicates = (error as { duplicates?: AlreadyOrderedItem[] } | undefined)?.duplicates;
  if (response.status === 409 && duplicates?.length) {
    throw new DuplicateOrderError(problem(error, response.status).message, duplicates);
  }
  throw problem(error, response.status);
}
