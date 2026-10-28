import { api, type Schemas } from "@/lib/api/client";
import { messages } from "@/messages/pt-BR";

export type Sector = Schemas["Sector"];
export type DiningTable = Schemas["DiningTable"];
export type UpdateTableRequest = Schemas["UpdateTableRequest"];

const t = messages.floor.errors;

/** Problem Details do back vira mensagem pronta para a tela (409, 422, 404 trazem uma frase). */
function failure(response: Response | undefined, error: unknown): Error {
  const problem = error as { detail?: string; fields?: string[] } | undefined;
  if (response?.status === 400 && problem?.fields) return new Error(t.fields);
  if (response && [400, 403, 404, 409, 422].includes(response.status) && problem?.detail) {
    return new Error(problem.detail);
  }
  return new Error(t.generic);
}

export async function fetchSectors(): Promise<Sector[]> {
  const { data, error, response } = await api.GET("/api/v1/sectors", { params: { query: { page: 0, size: 100 } } });
  if (data) return data.content;
  throw failure(response, error);
}

export async function createSector(name: string): Promise<Sector> {
  const { data, error, response } = await api.POST("/api/v1/sectors", { body: { name } });
  if (data) return data;
  throw failure(response, error);
}

export async function renameSector(id: string, name: string): Promise<Sector> {
  const { data, error, response } = await api.PATCH("/api/v1/sectors/{id}", { params: { path: { id } }, body: { name } });
  if (data) return data;
  throw failure(response, error);
}

export async function deactivateSector(id: string): Promise<void> {
  const { error, response } = await api.DELETE("/api/v1/sectors/{id}", { params: { path: { id } } });
  if (!response.ok) throw failure(response, error);
}

export async function fetchTables(): Promise<DiningTable[]> {
  const { data, error, response } = await api.GET("/api/v1/tables", { params: { query: { page: 0, size: 100 } } });
  if (data) return data.content;
  throw failure(response, error);
}

export async function createTable(label: string, sectorId: string): Promise<DiningTable> {
  const { data, error, response } = await api.POST("/api/v1/tables", { body: { label, sectorId } });
  if (data) return data;
  throw failure(response, error);
}

export async function updateTable(id: string, body: UpdateTableRequest): Promise<DiningTable> {
  const { data, error, response } = await api.PATCH("/api/v1/tables/{id}", { params: { path: { id } }, body });
  if (data) return data;
  throw failure(response, error);
}

export async function rotateTableToken(id: string): Promise<DiningTable> {
  const { data, error, response } = await api.POST("/api/v1/tables/{id}/token-rotation", { params: { path: { id } } });
  if (data) return data;
  throw failure(response, error);
}
