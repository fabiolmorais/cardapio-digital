import { api, type Schemas } from "@/lib/api/client";
import { messages } from "@/messages/pt-BR";

export type PresenceRule = Schemas["PresenceRule"];
export type PresenceRuleRequest = Schemas["PresenceRuleRequest"];
export type PresenceLocation = Schemas["PresenceLocation"];

/** Problem Details do back (o IP ou o raio inválido, com o texto) vira mensagem pronta. */
function failure(response: Response | undefined, error: unknown): Error {
  const detail = (error as { detail?: unknown } | undefined)?.detail;
  if (response && [400, 403].includes(response.status) && typeof detail === "string") return new Error(detail);
  return new Error(messages.presence.error);
}

export async function fetchPresenceRule(): Promise<PresenceRule> {
  const { data, error, response } = await api.GET("/api/v1/presence-rule");
  if (data) return data;
  throw failure(response, error);
}

export async function updatePresenceRule(body: PresenceRuleRequest): Promise<PresenceRule> {
  const { data, error, response } = await api.PUT("/api/v1/presence-rule", { body });
  if (data) return data;
  throw failure(response, error);
}
