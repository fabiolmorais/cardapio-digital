"use client";

import { useMutation } from "@tanstack/react-query";
import { type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogClose, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { messages } from "@/messages/pt-BR";
import type { MenuCategory } from "./menu-api";

const t = messages.menu.categoryForm;

/** Criar ou renomear categoria (com category: renomear). */
export function CategoryFormDialog({
  open,
  onOpenChange,
  category,
  onSave,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  category?: MenuCategory;
  onSave: (name: string) => Promise<unknown>;
}) {
  const mutation = useMutation({ mutationFn: onSave, onSuccess: () => onOpenChange(false) });

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    mutation.mutate(String(new FormData(event.currentTarget).get("name") ?? "").trim());
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) mutation.reset();
        onOpenChange(next);
      }}
    >
      <DialogContent className="max-h-[calc(100dvh-2rem)] gap-5 overflow-y-auto p-6 sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="text-lg">{category ? t.renameTitle : t.createTitle}</DialogTitle>
        </DialogHeader>
        <form onSubmit={onSubmit} className="flex flex-col gap-5">
          <div className="flex flex-col gap-2">
            <Label htmlFor="category-name" className="text-muted-foreground">
              {t.name}
            </Label>
            <Input
              id="category-name"
              name="name"
              required
              maxLength={40}
              autoComplete="off"
              defaultValue={category?.name}
              placeholder={t.namePlaceholder}
              className="h-12 rounded-xl px-4 text-base"
            />
          </div>
          <p role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
            {mutation.error?.message ?? ""}
          </p>
          <DialogFooter className="-mx-6 -mb-6 gap-2 px-6 pb-6">
            <DialogClose render={<Button variant="outline" size="lg" className="h-11 px-4" />}>
              {messages.common.cancel}
            </DialogClose>
            <Button type="submit" size="lg" className="h-11 px-4" disabled={mutation.isPending}>
              {mutation.isPending ? t.saving : category ? t.save : t.create}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
