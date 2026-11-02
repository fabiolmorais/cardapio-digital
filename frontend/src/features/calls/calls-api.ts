import { DeviceNotLinkedError } from "@/features/devices/device-api";
import { api, type Schemas } from "@/lib/api/client";

export type StaffCall = Schemas["StaffCall"];

/** A mensagem do back (Problem Details) ou o código HTTP. */
function problem(error: unknown, status: number): Error {
  const detail = (error as { detail?: unknown } | undefined)?.detail;
  return new Error(typeof detail === "string" ? detail : String(status));
}

/** A fila: os chamados abertos, do mais antigo para o mais novo (uma página basta para o salão). */
export async function fetchCallQueue(): Promise<StaffCall[]> {
  const { data, error, response } = await api.GET("/api/v1/calls", { params: { query: { page: 0, size: 100 } } });
  if (!data) throw problem(error, response.status);
  return data.content;
}

/**
 * A mesma fila, para a TV vinculada: sem login, pelo cookie do aparelho (que só vai para
 * /api/v1/auth). Sem vínculo (a TV foi desvinculada), DeviceNotLinkedError.
 */
export async function fetchTvCallQueue(): Promise<StaffCall[]> {
  const { data, error, response } = await api.GET("/api/v1/auth/device/tv/calls", {
    params: { query: { page: 0, size: 100 } },
  });
  if (data) return data.content;
  if (response.status === 401) throw new DeviceNotLinkedError();
  throw problem(error, response.status);
}

export async function acceptCall(id: string): Promise<StaffCall> {
  const { data, error, response } = await api.POST("/api/v1/calls/{id}/accept", { params: { path: { id } } });
  if (!data) throw problem(error, response.status);
  return data;
}

export async function finishCall(id: string): Promise<StaffCall> {
  const { data, error, response } = await api.POST("/api/v1/calls/{id}/finish", { params: { path: { id } } });
  if (!data) throw problem(error, response.status);
  return data;
}
