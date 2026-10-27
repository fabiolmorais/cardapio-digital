import type { Schemas } from "@/lib/api/client";

export type Role = Schemas["TeamMember"]["role"];

/**
 * Espelho, só para a tela, da regra "quem mexe em quem" do back (TeamPolicy). Serve para
 * esconder botões que dariam 403; quem garante a regra é o back.
 */
export function canManageTeam(actor: Role): boolean {
  return actor === "OWNER" || actor === "MANAGER";
}

/** Papéis que quem está logado pode dar a alguém (na ordem em que aparecem no formulário). */
export function assignableRoles(actor: Role): Role[] {
  if (actor === "OWNER") return ["MANAGER", "STAFF", "KITCHEN"];
  if (actor === "MANAGER") return ["STAFF", "KITCHEN"];
  return [];
}

export function canManageMember(actor: Role, target: Role): boolean {
  return assignableRoles(actor).includes(target);
}
