"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { messages } from "@/messages/pt-BR";
import { addTeamMember, updateTeamMember, type TeamMember } from "./team-api";
import type { Role } from "./team-rules";

const t = messages.team;
const roleNames = messages.panel.roles;

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Papéis que quem está logado pode dar. */
  roles: Role[];
  /** Sem membro: cadastro. Com membro: edição. */
  member?: TeamMember;
};

/** Cadastro (nome, papel e PIN) ou edição (papel e PIN novo) de uma pessoa da equipe. */
export function MemberFormDialog({ open, onOpenChange, roles, member }: Props) {
  const queryClient = useQueryClient();
  const editing = member !== undefined;

  const mutation = useMutation({
    mutationFn: async (form: FormData) => {
      const role = String(form.get("role") ?? "") as Role;
      const pin = String(form.get("pin") ?? "").trim();
      if (member) {
        return updateTeamMember(member.id, {
          role: role === member.role ? undefined : role,
          pin: pin === "" ? undefined : pin,
        });
      }
      return addTeamMember({ name: String(form.get("name") ?? "").trim(), role, pin });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["team"] });
      onOpenChange(false);
    },
  });

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    mutation.mutate(new FormData(event.currentTarget));
  }

  function handleOpenChange(next: boolean) {
    if (!next) mutation.reset();
    onOpenChange(next);
  }

  const defaultRole = member?.role ?? roles.at(-1);
  const failed = mutation.isError;

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] gap-5 overflow-y-auto p-6 sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="text-lg">{editing ? t.form.editTitle(member.name) : t.form.addTitle}</DialogTitle>
          <DialogDescription>{editing ? t.form.editDescription : t.form.addDescription}</DialogDescription>
        </DialogHeader>

        <form onSubmit={onSubmit} className="flex flex-col gap-5" aria-describedby="member-form-error">
          {!editing && (
            <div className="flex flex-col gap-2">
              <Label htmlFor="member-name" className="text-muted-foreground">
                {t.form.name}
              </Label>
              <Input
                id="member-name"
                name="name"
                required
                maxLength={120}
                autoComplete="off"
                placeholder={t.form.namePlaceholder}
                aria-invalid={failed || undefined}
                className="h-12 rounded-xl px-4 text-base"
              />
            </div>
          )}

          <fieldset className="flex flex-col gap-2">
            <legend className="mb-2 text-sm font-medium text-muted-foreground">{t.form.role}</legend>
            {roles.map((role) => (
              <label
                key={role}
                className="flex min-h-14 cursor-pointer items-center gap-3 rounded-xl border border-border bg-card px-4 py-2 transition-colors has-checked:border-ring has-checked:bg-secondary has-focus-visible:ring-3 has-focus-visible:ring-ring/50"
              >
                <input
                  type="radio"
                  name="role"
                  value={role}
                  defaultChecked={role === defaultRole}
                  required
                  className="size-4 accent-primary"
                />
                <span className="flex flex-col">
                  <span className="font-medium">{roleNames[role]}</span>
                  <span className="text-sm text-muted-foreground">
                    {t.roleDescriptions[role as keyof typeof t.roleDescriptions]}
                  </span>
                </span>
              </label>
            ))}
          </fieldset>

          <div className="flex flex-col gap-2">
            <Label htmlFor="member-pin" className="text-muted-foreground">
              {editing ? t.form.newPin : t.form.pin}
            </Label>
            <Input
              id="member-pin"
              name="pin"
              inputMode="numeric"
              autoComplete="off"
              pattern="\d{4,6}"
              minLength={4}
              maxLength={6}
              required={!editing}
              placeholder={t.form.pinPlaceholder}
              aria-describedby="member-pin-hint"
              aria-invalid={failed || undefined}
              className="h-12 rounded-xl px-4 font-mono text-base tracking-[0.3em] placeholder:font-sans placeholder:tracking-normal"
            />
            <span id="member-pin-hint" className="text-sm text-muted-foreground">
              {editing ? t.form.newPinHint : t.form.pinHint}
            </span>
          </div>

          <p id="member-form-error" role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
            {mutation.error?.message ?? ""}
          </p>

          <DialogFooter className="-mx-6 -mb-6 gap-2 px-6 pb-6">
            <DialogClose render={<Button variant="outline" size="lg" className="h-11 px-4" />}>
              {t.form.cancel}
            </DialogClose>
            <Button type="submit" size="lg" className="h-11 px-4" disabled={mutation.isPending}>
              {editing
                ? mutation.isPending
                  ? t.form.saving
                  : t.form.save
                : mutation.isPending
                  ? t.form.adding
                  : t.form.addSubmit}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
