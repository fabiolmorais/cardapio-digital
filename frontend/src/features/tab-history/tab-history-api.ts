import { api, type Schemas } from "@/lib/api/client";

export type ClosedTab = Schemas["ClosedTab"];
export type ClosedTabPage = Schemas["PageResponseClosedTab"];
export type HistoryFilter = { from: string; to: string; tableId: string | null; page: number };

export const PAGE_SIZE = 50;

/** A mensagem do back (Problem Details: o período inválido, por exemplo) ou o código HTTP. */
function problem(error: unknown, status: number): Error {
  const detail = (error as { detail?: unknown } | undefined)?.detail;
  return new Error(typeof detail === "string" ? detail : String(status));
}

/** As comandas fechadas no período (os dias do restaurante), da mais nova à mais antiga. */
export async function fetchClosedTabs({ from, to, tableId, page }: HistoryFilter): Promise<ClosedTabPage> {
  const { data, error, response } = await api.GET("/api/v1/tabs", {
    params: { query: { from, to, page, size: PAGE_SIZE, ...(tableId && { tableId }) } },
  });
  if (!data) throw problem(error, response.status);
  return data;
}
