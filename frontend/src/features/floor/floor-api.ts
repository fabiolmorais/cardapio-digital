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

// Arquivos para impressão (QR Code e display).

export type DisplayFormat = "PNG" | "SVG" | "PDF";
export type DownloadedFile = { blob: Blob; fileName: string };

/** Nome do arquivo que o back mandou no Content-Disposition (o CORS do back expõe esse cabeçalho). */
function fileNameFrom(response: Response, fallback: string): string {
  const header = response.headers.get("Content-Disposition") ?? "";
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(header)?.[1];
  if (encoded) return decodeURIComponent(encoded);
  return /filename="?([^";]+)"?/i.exec(header)?.[1] ?? fallback;
}

async function problemOf(response: Response): Promise<Error> {
  const problem = await response
    .clone()
    .json()
    .catch(() => undefined);
  return failure(response, problem);
}

export async function fetchTableDisplay(tableId: string, format: DisplayFormat): Promise<DownloadedFile> {
  const { data, response } = await api.GET("/api/v1/tables/{id}/display", {
    params: { path: { id: tableId }, query: { format } },
    parseAs: "blob",
  });
  if (!response.ok || !data) throw await problemOf(response);
  return { blob: data, fileName: fileNameFrom(response, `mesa.${format.toLowerCase()}`) };
}

export async function fetchSectorDisplays(sectorId: string): Promise<DownloadedFile> {
  const { data, response } = await api.GET("/api/v1/sectors/{id}/displays", {
    params: { path: { id: sectorId } },
    parseAs: "blob",
  });
  if (!response.ok || !data) throw await problemOf(response);
  return { blob: data, fileName: fileNameFrom(response, "setor.pdf") };
}

/** Entrega o arquivo ao navegador como um download comum, com o nome que veio do back. */
export function saveFile({ blob, fileName }: DownloadedFile) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  link.click();
  // Dá tempo de o navegador começar o download antes de liberar a memória.
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
