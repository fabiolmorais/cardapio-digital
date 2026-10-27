import { api, type Schemas } from "@/lib/api/client";
import { messages } from "@/messages/pt-BR";

export type TeamMember = Schemas["TeamMember"];
export type TeamPage = Schemas["PageResponseTeamMember"];
export type AddTeamMemberRequest = Schemas["AddTeamMemberRequest"];
export type UpdateTeamMemberRequest = Schemas["UpdateTeamMemberRequest"];

export const PAGE_SIZE = 20;

const t = messages.team.errors;

/**
 * Transforma a resposta de erro do back (Problem Details) numa mensagem para a tela:
 * 403 e 404 trazem uma frase pronta; 400 de validação traz só os nomes dos campos.
 */
function failure(response: Response | undefined, error: unknown): Error {
  const problem = error as { detail?: string; fields?: string[] } | undefined;
  if (response?.status === 400 && problem?.fields) return new Error(t.fields);
  if (response && [400, 403, 404, 409, 422].includes(response.status) && problem?.detail) {
    return new Error(problem.detail);
  }
  return new Error(t.generic);
}

export async function fetchTeam(page: number): Promise<TeamPage> {
  const { data, error, response } = await api.GET("/api/v1/team-members", {
    params: { query: { page, size: PAGE_SIZE } },
  });
  if (data) return data;
  throw failure(response, error);
}

export async function addTeamMember(body: AddTeamMemberRequest): Promise<TeamMember> {
  const { data, error, response } = await api.POST("/api/v1/team-members", { body });
  if (data) return data;
  throw failure(response, error);
}

export async function updateTeamMember(id: string, body: UpdateTeamMemberRequest): Promise<TeamMember> {
  const { data, error, response } = await api.PATCH("/api/v1/team-members/{id}", {
    params: { path: { id } },
    body,
  });
  if (data) return data;
  throw failure(response, error);
}

export async function reactivateTeamMember(id: string): Promise<TeamMember> {
  const { data, error, response } = await api.POST("/api/v1/team-members/{id}/reactivation", {
    params: { path: { id } },
  });
  if (data) return data;
  throw failure(response, error);
}

export async function deactivateTeamMember(id: string): Promise<void> {
  const { error, response } = await api.DELETE("/api/v1/team-members/{id}", { params: { path: { id } } });
  if (!response.ok) throw failure(response, error);
}
