import { baseUrl } from "@/lib/api/base";
import type { components } from "@/lib/api/schema";
import { tablePage } from "@/messages/table.pt-BR";

export type RatingOffer = components["schemas"]["TableRatingOffer"];
export type RateRequest = components["schemas"]["RateServiceRequest"];
export type RatingTag = NonNullable<RateRequest["tags"]>[number];

/**
 * A avaliação usa o cookie que o celular já tem, sem reabrir a sessão (o envio de sempre abre outra
 * quando a dela acabou): depois de a comanda fechar, é a sessão encerrada que ainda pode avaliar.
 */
function call(token: string, path: string, init: RequestInit = {}) {
  return fetch(`${baseUrl}/api/v1/public/tables/${encodeURIComponent(token)}/rating${path}`, {
    ...init,
    credentials: "include",
  });
}

/** Se este celular pode avaliar agora; null se não deu para saber (sem sessão, sem rede). */
export async function fetchRatingOffer(token: string): Promise<RatingOffer | null> {
  try {
    const response = await call(token, "");
    return response.ok ? response.json() : null;
  } catch {
    return null;
  }
}

export async function rateService(token: string, request: RateRequest): Promise<RatingOffer> {
  const response = await call(token, "", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(typeof body?.detail === "string" ? body.detail : tablePage.rating.failed);
  }
  return response.json();
}

/** O toque em "Avaliar no Google": conta mesmo com a página indo para o Google. */
export function clickGoogle(token: string): void {
  void call(token, "/google", { method: "POST", keepalive: true }).catch(() => {});
}
