import { api, type Schemas } from "@/lib/api/client";

export type Unit = Schemas["Unit"];
export type UnitAllowance = Schemas["UnitAllowance"];

export const UNITS_KEY = ["units"] as const;

/** A mensagem do back (Problem Details) ou o código HTTP. */
function problem(error: unknown, status: number): Error {
  const detail = (error as { detail?: unknown } | undefined)?.detail;
  return new Error(typeof detail === "string" ? detail : String(status));
}

export async function fetchUnits(page: number, size: number) {
  const { data, error, response } = await api.GET("/api/v1/units", { params: { query: { page, size } } });
  if (!data) throw problem(error, response.status);
  return data;
}

export async function fetchUnitAllowance(): Promise<UnitAllowance> {
  const { data, error, response } = await api.GET("/api/v1/units/allowance");
  if (!data) throw problem(error, response.status);
  return data;
}

export async function createUnit(name: string, timeZone: string): Promise<Unit> {
  const { data, error, response } = await api.POST("/api/v1/units", { body: { name, timeZone } });
  if (!data) throw problem(error, response.status);
  return data;
}
