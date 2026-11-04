import { api, type Schemas } from "@/lib/api/client";
import { messages } from "@/messages/pt-BR";

export type Rodizio = Schemas["Rodizio"];
export type RodizioRequest = Schemas["RodizioRequest"];
export type DayOfWeek = Schemas["RodizioSpecialPrice"]["days"][number];

const t = messages.rodizio.errors;

/** Problem Details do back vira frase pronta para a tela (o 400 do domínio, o 409 e o 422 trazem a sua). */
function failure(response: Response | undefined, error: unknown): Error {
  const problem = error as { detail?: string; fields?: string[] } | undefined;
  if (response?.status === 400 && problem?.fields) return new Error(t.fields);
  if (response && [400, 403, 404, 409, 422].includes(response.status) && problem?.detail) {
    return new Error(problem.detail);
  }
  return new Error(t.generic);
}

/** Os rodízios ativos, por nome (no máximo 10 por restaurante: uma página basta). */
export async function fetchRodizios(): Promise<Rodizio[]> {
  const { data, error, response } = await api.GET("/api/v1/rodizios", { params: { query: { page: 0, size: 20 } } });
  if (data) return data.content;
  throw failure(response, error);
}

export async function createRodizio(body: RodizioRequest): Promise<Rodizio> {
  const { data, error, response } = await api.POST("/api/v1/rodizios", { body });
  if (data) return data;
  throw failure(response, error);
}

export async function updateRodizio(id: string, body: RodizioRequest): Promise<Rodizio> {
  const { data, error, response } = await api.PUT("/api/v1/rodizios/{id}", { params: { path: { id } }, body });
  if (data) return data;
  throw failure(response, error);
}

export async function deactivateRodizio(id: string): Promise<void> {
  const { error, response } = await api.DELETE("/api/v1/rodizios/{id}", { params: { path: { id } } });
  if (!response.ok) throw failure(response, error);
}

export type MenuProduct = { id: string; name: string; categoryId: string };

/** Todos os produtos ativos do cardápio, de página em página (100 por vez), para escolher os incluídos. */
export async function fetchAllProducts(): Promise<MenuProduct[]> {
  const all: MenuProduct[] = [];
  for (let page = 0; ; page++) {
    const { data, error, response } = await api.GET("/api/v1/products", { params: { query: { page, size: 100 } } });
    if (!data) throw failure(response, error);
    all.push(...data.content.map((p) => ({ id: p.id, name: p.name, categoryId: p.categoryId })));
    if (all.length >= data.totalElements || data.content.length === 0) return all;
  }
}
