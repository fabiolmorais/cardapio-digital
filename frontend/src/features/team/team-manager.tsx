"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { useSession } from "@/lib/auth/use-session";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { DeactivateDialog } from "./deactivate-dialog";
import { MemberFormDialog } from "./member-form-dialog";
import { fetchTeam, reactivateTeamMember, type TeamMember } from "./team-api";
import { ErrorText, isPlanLimit } from "@/features/subscription/error-text";
import { PlanLimitBanner, PlanMeter } from "@/features/subscription/plan-meter";
import { assignableRoles, canManageMember, canManageTeam, type Role } from "./team-rules";

const t = messages.team;
const roleNames = messages.panel.roles;

/** As cores dos papéis (desenho 12.5): dono na cor da marca, gerente azul, garçom neutro, cozinha âmbar. */
const roleStyle: Record<Role, string> = {
  OWNER: "bg-primary/15 text-brand-text",
  MANAGER: "bg-sky-400/15 text-sky-300",
  STAFF: "bg-muted text-foreground",
  KITCHEN: "bg-amber-400/15 text-amber-300",
};

/** "Agora", "Hoje, 18:02", "Ontem, 23:10" ou "05/11, 14:30", no fuso do restaurante. */
function lastAccess(iso: string | null | undefined, timeZone: string, now = Date.now()) {
  if (!iso) return t.lastAccess.never;
  const at = new Date(iso);
  if (now - at.getTime() < 2 * 60_000) return t.lastAccess.now;
  const day = (d: Date) => new Intl.DateTimeFormat("pt-BR", { timeZone, dateStyle: "short" }).format(d);
  const time = new Intl.DateTimeFormat("pt-BR", { timeZone, hour: "2-digit", minute: "2-digit" }).format(at);
  if (day(at) === day(new Date(now))) return t.lastAccess.today(time);
  if (day(at) === day(new Date(now - 86_400_000))) return t.lastAccess.yesterday(time);
  const date = new Intl.DateTimeFormat("pt-BR", { timeZone, day: "2-digit", month: "2-digit" }).format(at);
  return t.lastAccess.other(date, time);
}

function initials(name: string) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}

/** Tela da equipe: lista paginada, cadastro, edição de papel/PIN e desligamento. */
export function TeamManager() {
  const { status, session } = useSession();
  const actor = session?.establishment.role ?? "STAFF";
  const allowed = canManageTeam(actor);

  const [page, setPage] = useState(0);
  const [adding, setAdding] = useState(false);
  const [editing, setEditing] = useState<TeamMember | null>(null);
  const [removing, setRemoving] = useState<TeamMember | null>(null);

  const team = useQuery({
    queryKey: ["team", page],
    queryFn: () => fetchTeam(page),
    enabled: status === "authenticated" && allowed,
    placeholderData: keepPreviousData,
  });

  const roles = assignableRoles(actor);

  // Religar não pede confirmação: é o caminho de volta, e desfazer é só desligar de novo.
  const queryClient = useQueryClient();
  const reactivation = useMutation({
    mutationFn: (member: TeamMember) => reactivateTeamMember(member.id),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: ["team"] }),
        queryClient.invalidateQueries({ queryKey: ["subscription"] }),
      ]),
  });

  return (
    <div className="flex min-w-0 flex-1 flex-col gap-6">
      <header className="flex flex-wrap items-end gap-4">
        <div className="mr-auto flex flex-col gap-1">
          <h1 className="text-2xl font-bold tracking-tight">{t.title}</h1>
          <p className="text-sm text-muted-foreground">{t.subtitle}</p>
        </div>
        {status === "authenticated" && allowed && <PlanMeter kind="users" role={actor} />}
        {status === "authenticated" && allowed && (
          <Button size="xl" onClick={() => setAdding(true)}>
            <Plus aria-hidden />
            {t.add}
          </Button>
        )}
      </header>

      {status === "authenticated" && allowed && <PlanLimitBanner kind="users" role={actor} />}

      {status !== "authenticated" ? (
        <p className="text-muted-foreground">{t.loading}</p>
      ) : !allowed ? (
        <p role="alert" className="text-muted-foreground">
          {t.noPermission}
        </p>
      ) : team.isPending ? (
        <p className="text-muted-foreground">{t.loading}</p>
      ) : team.isError ? (
        <p role="alert" className="text-destructive">
          {t.loadError}
        </p>
      ) : (
        <>
          {/* relative: prende o texto sr-only (absolute) dentro da caixa com rolagem; sem isso ele alarga a página no celular. */}
          <div className="relative overflow-x-auto rounded-2xl border border-border bg-card">
            <table className="w-full min-w-[760px] border-collapse text-sm">
              <thead>
                <tr className="border-b border-border text-left text-xs tracking-wide text-muted-foreground uppercase">
                  <th scope="col" className="px-4 py-3 font-semibold">
                    {t.columns.person}
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    {t.columns.role}
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    {t.columns.access}
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    {t.columns.status}
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    {t.columns.lastAccess}
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    <span className="sr-only">{t.columns.actions}</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {team.data.content.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-4 py-6 text-muted-foreground">
                      {t.empty}
                    </td>
                  </tr>
                )}
                {team.data.content.map((member) => {
                  const manageable = canManageMember(actor, member.role);
                  return (
                    <tr
                      key={member.id}
                      className={cn("border-b border-border last:border-0", !member.active && "opacity-60")}
                    >
                      <td className="px-4 py-3">
                        <span className="flex items-center gap-3">
                          <span
                            aria-hidden
                            className="flex size-9 shrink-0 items-center justify-center rounded-full bg-secondary text-xs font-bold"
                          >
                            {initials(member.name)}
                          </span>
                          <span className="font-medium whitespace-nowrap">{member.name}</span>
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={cn("rounded-full px-2.5 py-0.5 text-xs font-semibold", roleStyle[member.role])}
                        >
                          {roleNames[member.role]}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-muted-foreground">
                        {member.role === "OWNER" ? t.access.password : t.access.pin}
                      </td>
                      <td className="px-4 py-3">
                        {member.active ? (
                          <span className="text-success">{t.status.active}</span>
                        ) : (
                          <span className="text-muted-foreground">{t.status.inactive}</span>
                        )}
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap text-muted-foreground">
                        {lastAccess(member.lastAccessAt, session.establishment.timeZone)}
                      </td>
                      <td className="px-4 py-2">
                        {manageable && (
                          <span className="flex justify-end gap-1">
                            <Button
                              variant="ghost"
                              className="h-10 px-3"
                              aria-label={t.editLabel(member.name)}
                              onClick={() => setEditing(member)}
                            >
                              {t.edit}
                            </Button>
                            {member.active ? (
                              <Button
                                variant="ghost"
                                className="h-10 px-3 text-destructive hover:text-destructive"
                                aria-label={t.deactivateLabel(member.name)}
                                onClick={() => setRemoving(member)}
                              >
                                {t.deactivate}
                              </Button>
                            ) : (
                              <Button
                                variant="ghost"
                                className="h-10 px-3 text-brand-text hover:text-brand-text"
                                aria-label={t.reactivateLabel(member.name)}
                                disabled={reactivation.isPending}
                                onClick={() => reactivation.mutate(member)}
                              >
                                {reactivation.isPending && reactivation.variables?.id === member.id
                                  ? t.reactivating
                                  : t.reactivate}
                              </Button>
                            )}
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {reactivation.isError && (
            <p role="alert" className="text-sm text-destructive">
              {isPlanLimit(reactivation.error.message) ? (
                <ErrorText message={reactivation.error.message} />
              ) : (
                t.reactivateError(reactivation.variables?.name ?? "")
              )}
            </p>
          )}

          {team.data.totalPages > 1 && (
            <nav className="flex items-center justify-end gap-3" aria-label={t.title}>
              <span className="text-sm text-muted-foreground">{t.pagination.page(page + 1, team.data.totalPages)}</span>
              <Button variant="outline" className="h-10 px-4" disabled={page === 0} onClick={() => setPage(page - 1)}>
                {t.pagination.previous}
              </Button>
              <Button
                variant="outline"
                className="h-10 px-4"
                disabled={page + 1 >= team.data.totalPages}
                onClick={() => setPage(page + 1)}
              >
                {t.pagination.next}
              </Button>
            </nav>
          )}
        </>
      )}

      <MemberFormDialog open={adding} onOpenChange={setAdding} roles={roles} />
      <MemberFormDialog
        key={editing?.id ?? "none"}
        open={editing !== null}
        onOpenChange={(open) => !open && setEditing(null)}
        roles={roles}
        member={editing ?? undefined}
      />
      <DeactivateDialog member={removing} onOpenChange={(open) => !open && setRemoving(null)} />
    </div>
  );
}
