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

/** Onde o pedido vai: a comanda da mesa ou a comanda de um cliente do balcão (ADR 0015). */
export type StaffOrderTarget = { kind: "table"; id: string } | { kind: "counter"; id: string };

/** Se o lançamento está liberado, o que a mesa (ou a comanda do balcão) já pediu e o rodízio dela. */
export async function fetchStaffOrderSheet(target: StaffOrderTarget): Promise<StaffOrderSheet> {
  const params = { params: { path: { id: target.id } } };
  const { data, error, response } =
    target.kind === "table"
      ? await api.GET("/api/v1/tables/{id}/staff-order", params)
      : await api.GET("/api/v1/counter-tabs/{id}/staff-order", params);
  if (!data) throw problem(error, response.status);
  return data;
}

/** O cardápio do garçom: preços e opções, sem fotos e sem vídeos. */
export async function fetchStaffMenu(): Promise<Schemas["PublicMenu"]> {
  const { data, error, response } = await api.GET("/api/v1/staff-menu");
  if (!data) throw problem(error, response.status);
  return data;
}

export async function placeStaffOrder(target: StaffOrderTarget, body: PlaceStaffOrder): Promise<Schemas["StaffOrder"]> {
  const request = { params: { path: { id: target.id } }, body };
  const { data, error, response } =
    target.kind === "table"
      ? await api.POST("/api/v1/tables/{id}/orders", request)
      : await api.POST("/api/v1/counter-tabs/{id}/orders", request);
  if (data) return data;
  const duplicates = (error as { duplicates?: AlreadyOrderedItem[] } | undefined)?.duplicates;
  if (response.status === 409 && duplicates?.length) {
    throw new DuplicateOrderError(problem(error, response.status).message, duplicates);
  }
  throw problem(error, response.status);
}
