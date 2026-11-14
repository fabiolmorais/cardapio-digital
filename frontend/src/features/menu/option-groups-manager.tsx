"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Plus } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { ToggleSwitch } from "@/components/toggle-switch";
import { Button } from "@/components/ui/button";
import { canManageTeam } from "@/features/team/team-rules";
import { useSession } from "@/lib/auth/use-session";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { OptionGroupDialog } from "./option-group-dialog";
import {
  OPTION_GROUPS_KEY,
  createOptionGroup,
  deactivateOptionGroup,
  fetchOptionGroups,
  setOptionItemAvailability,
  updateOptionGroup,
  type OptionGroup,
} from "./options-api";

const t = messages.options;

/** A regra do grupo numa linha: "Obrigatório · escolha 1 · Divide o item em partes · Cobra o maior preço". */
export function rulesSummary(g: OptionGroup): string {
  return [
    g.minChoices > 0 ? t.required(g.minChoices, g.maxChoices) : t.optional(g.maxChoices),
    g.splitsItem ? t.splits : null,
    g.maxPerOption > 1 ? t.repeat(g.maxPerOption) : null,
    t.pricing[g.pricing],
    g.freeQuantity > 0 ? t.free(g.freeQuantity) : null,
  ]
    .filter(Boolean)
    .join(" · ");
}

/** A biblioteca de grupos de opções (ADR 0010): criar, editar, esgotar uma opção e desativar. */
export function OptionGroupsManager() {
  const { status, session } = useSession();
  const allowed = status === "authenticated" && canManageTeam(session.establishment.role);
  const queryClient = useQueryClient();
  const groups = useQuery({ queryKey: OPTION_GROUPS_KEY, queryFn: fetchOptionGroups, enabled: allowed });
  const [editing, setEditing] = useState<OptionGroup | "new" | null>(null);
  const [removing, setRemoving] = useState<OptionGroup | null>(null);
  const refresh = () => queryClient.invalidateQueries({ queryKey: OPTION_GROUPS_KEY });
  const availability = useMutation({
    mutationFn: ({ id, available }: { id: string; available: boolean }) => setOptionItemAvailability(id, available),
    onSettled: refresh,
  });

  if (status !== "authenticated" || (allowed && groups.isPending)) {
    return <p className="px-4 py-6 text-muted-foreground sm:px-7">{t.loading}</p>;
  }
  if (!allowed) {
    return (
      <p role="alert" className="px-4 py-6 text-muted-foreground sm:px-7">
        {messages.team.noPermission}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-6 px-4 py-6 sm:px-7">
      <header className="flex flex-wrap items-end gap-4">
        <div className="mr-auto flex max-w-2xl flex-col gap-1">
          <Link
            href="/admin/cardapio"
            className="inline-flex min-h-11 items-center gap-1.5 self-start rounded-lg text-sm text-muted-foreground hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
          >
            <ArrowLeft aria-hidden className="size-4" />
            {t.back}
          </Link>
          <h1 className="text-2xl font-bold tracking-tight">{t.title}</h1>
          <p className="text-sm text-muted-foreground">{t.subtitle}</p>
        </div>
        <Button size="xl" onClick={() => setEditing("new")}>
          <Plus aria-hidden />
          {t.newGroup}
        </Button>
      </header>

      {groups.isError && (
        <p role="alert" className="text-sm text-destructive">
          {t.loadError}
        </p>
      )}
      {availability.error && (
        <p role="alert" className="text-sm text-destructive">
          {availability.error.message}
        </p>
      )}

      {groups.data?.length === 0 ? (
        <p className="max-w-lg rounded-2xl border border-dashed border-border p-6 text-muted-foreground">{t.empty}</p>
      ) : (
        <ul className="grid gap-4 lg:grid-cols-2">
          {(groups.data ?? []).map((group) => (
            <li
              key={group.id}
              aria-label={group.name}
              className="flex flex-col gap-3 rounded-2xl border border-border bg-card p-5"
            >
              <div className="flex items-start gap-3">
                <div className="mr-auto flex min-w-0 flex-col gap-0.5">
                  <h2 className="font-semibold break-words">{group.name}</h2>
                  {group.name !== group.title && <span className="text-sm text-muted-foreground">“{group.title}”</span>}
                  <span className="text-[13px] text-muted-foreground">{rulesSummary(group)}</span>
                  <span className="text-[13px] text-muted-foreground">{t.usedIn(group.productCount ?? 0)}</span>
                </div>
                <Button
                  variant="ghost"
                  className="h-10 px-3"
                  aria-label={t.editLabel(group.name)}
                  onClick={() => setEditing(group)}
                >
                  {t.edit}
                </Button>
                <Button
                  variant="ghost"
                  className="h-10 px-3 text-destructive hover:text-destructive"
                  aria-label={t.deactivateLabel(group.name)}
                  onClick={() => setRemoving(group)}
                >
                  {t.deactivate}
                </Button>
              </div>
              <ul className="flex flex-col divide-y divide-border rounded-xl border border-border">
                {group.items.map((item) => (
                  <li key={item.id} className="flex items-center gap-3 px-3 py-2">
                    <span className={item.available ? "mr-auto" : "mr-auto text-muted-foreground line-through"}>
                      {item.name}
                    </span>
                    <span className="font-mono text-sm text-muted-foreground">
                      {item.priceCents === 0 ? "—" : formatCents(item.priceCents)}
                    </span>
                    <ToggleSwitch
                      checked={item.available}
                      label={t.itemAvailable(item.name)}
                      onToggle={() =>
                        availability.mutateAsync({ id: item.id, available: !item.available }).then(() => {})
                      }
                    />
                  </li>
                ))}
              </ul>
            </li>
          ))}
        </ul>
      )}

      <OptionGroupDialog
        open={editing !== null}
        onOpenChange={(open) => !open && setEditing(null)}
        group={editing === "new" || editing === null ? undefined : editing}
        onSave={async (body) => {
          await (editing && editing !== "new" ? updateOptionGroup(editing.id, body) : createOptionGroup(body));
          await refresh();
        }}
      />
      {removing && (
        <ConfirmDialog
          open
          onOpenChange={(open) => !open && setRemoving(null)}
          title={t.deactivateTitle(removing.name)}
          description={t.deactivateText}
          confirmLabel={t.deactivate}
          onConfirm={async () => {
            await deactivateOptionGroup(removing.id);
            await refresh();
          }}
        />
      )}
    </div>
  );
}
