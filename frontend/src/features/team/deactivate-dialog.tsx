"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
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
import { messages } from "@/messages/pt-BR";
import { deactivateTeamMember, type TeamMember } from "./team-api";

const t = messages.team.confirm;

type Props = {
  member: TeamMember | null;
  onOpenChange: (open: boolean) => void;
};

/** Confirmação antes de desligar alguém da equipe. */
export function DeactivateDialog({ member, onOpenChange }: Props) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: (id: string) => deactivateTeamMember(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["team"] });
      onOpenChange(false);
    },
  });

  function handleOpenChange(next: boolean) {
    if (!next) mutation.reset();
    onOpenChange(next);
  }

  return (
    <Dialog open={member !== null} onOpenChange={handleOpenChange}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] gap-5 overflow-y-auto p-6 sm:max-w-md" role="alertdialog">
        <DialogHeader>
          <DialogTitle className="text-lg">{member ? t.title(member.name) : ""}</DialogTitle>
          <DialogDescription>{t.description}</DialogDescription>
        </DialogHeader>
        <p role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
          {mutation.error?.message ?? ""}
        </p>
        <DialogFooter className="-mx-6 -mb-6 gap-2 px-6 pb-6">
          <DialogClose render={<Button variant="outline" size="lg" className="h-11 px-4" />}>{t.cancel}</DialogClose>
          <Button
            variant="destructive"
            size="lg"
            className="h-11 px-4"
            disabled={mutation.isPending}
            onClick={() => member && mutation.mutate(member.id)}
          >
            {mutation.isPending ? t.confirming : t.confirm}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
