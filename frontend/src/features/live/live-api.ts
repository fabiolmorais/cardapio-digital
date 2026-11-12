import { DeviceNotLinkedError } from "@/features/devices/device-api";
import { api, type Schemas } from "@/lib/api/client";

export type LiveFloor = Schemas["LiveFloor"];
export type LiveTable = Schemas["LiveTable"];
export type TvBoard = Schemas["TvBoard"];

/** A mensagem do back (Problem Details) ou o código HTTP. */
function problem(error: unknown, status: number): Error {
  const detail = (error as { detail?: unknown } | undefined)?.detail;
  return new Error(typeof detail === "string" ? detail : String(status));
}

/** O salão agora: as mesas (livre, ocupada, chamando, conta pedida) e o resumo do dia. */
export async function fetchLiveFloor(): Promise<LiveFloor> {
  const { data, error, response } = await api.GET("/api/v1/live/floor");
  if (!data) throw problem(error, response.status);
  return data;
}

/** O quadro da TV vinculada (sem login, pelo cookie): a cozinha, os setores e a espera média. */
export async function fetchTvBoard(): Promise<TvBoard> {
  const { data, error, response } = await api.GET("/api/v1/auth/device/tv/board");
  if (data) return data;
  if (response.status === 401) throw new DeviceNotLinkedError();
  throw problem(error, response.status);
}

/** Os setores na ordem do salão, sem repetir (as mesas já vêm por setor). */
export function sectorsOf(tables: LiveTable[]): string[] {
  return [...new Set(tables.map((t) => t.sectorName))];
}
