import { api, type Schemas } from "@/lib/api/client";

export type PrintJob = Schemas["PrintJob"];
export type TicketKind = PrintJob["kind"];

/** A mensagem do back (Problem Details) ou o código HTTP. */
function problem(error: unknown, status: number): Error {
  const detail = (error as { detail?: unknown } | undefined)?.detail;
  return new Error(typeof detail === "string" ? detail : String(status));
}

/** As comandas que ainda não saíram na impressora, da mais antiga à mais nova (até 20 por vez). */
export async function fetchPendingJobs(kind: TicketKind): Promise<PrintJob[]> {
  const { data, error, response } = await api.GET("/api/v1/print-jobs", {
    params: { query: { kind, page: 0, size: 20 } },
  });
  if (!data) throw problem(error, response.status);
  return data.content;
}

/** Pega a comanda: devolve só os itens que esta estação deve imprimir (vazio: outra já imprimiu). */
export async function claimJob(orderId: string, kind: TicketKind): Promise<PrintJob> {
  const { data, error, response } = await api.POST("/api/v1/print-jobs/claim", {
    body: { orderId, kind },
  });
  if (!data) throw problem(error, response.status);
  return data;
}

/** As comandas impressas nas últimas 12 horas, da mais recente para a mais antiga. */
export async function fetchPrintedJobs(): Promise<PrintJob[]> {
  const { data, error, response } = await api.GET("/api/v1/print-jobs/printed", {
    params: { query: { page: 0, size: 30 } },
  });
  if (!data) throw problem(error, response.status);
  return data.content;
}
