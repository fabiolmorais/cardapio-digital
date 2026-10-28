"use client";

import { useMutation } from "@tanstack/react-query";
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
import type { DiningTable, Sector } from "./floor-api";

const t = messages.floor;

const contentClass = "max-h-[calc(100dvh-2rem)] gap-5 overflow-y-auto p-6 sm:max-w-md";
const footerClass = "-mx-6 -mb-6 gap-2 px-6 pb-6";

type Open = { open: boolean; onOpenChange: (open: boolean) => void };

/** Criar ou renomear setor (com sector: renomear). */
export function SectorFormDialog({
  open,
  onOpenChange,
  sector,
  onSave,
}: Open & { sector?: Sector; onSave: (name: string) => Promise<unknown> }) {
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
      <DialogContent className={contentClass}>
        <DialogHeader>
          <DialogTitle className="text-lg">{sector ? t.sectorForm.renameTitle : t.sectorForm.createTitle}</DialogTitle>
        </DialogHeader>
        <form onSubmit={onSubmit} className="flex flex-col gap-5">
          <div className="flex flex-col gap-2">
            <Label htmlFor="sector-name" className="text-muted-foreground">
              {t.sectorForm.name}
            </Label>
            <Input
              id="sector-name"
              name="name"
              required
              maxLength={40}
              autoComplete="off"
              defaultValue={sector?.name}
              placeholder={t.sectorForm.namePlaceholder}
              className="h-12 rounded-xl px-4 text-base"
            />
          </div>
          <p role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
            {mutation.error?.message ?? ""}
          </p>
          <DialogFooter className={footerClass}>
            <DialogClose render={<Button variant="outline" size="lg" className="h-11 px-4" />}>
              {t.sectorForm.cancel}
            </DialogClose>
            <Button type="submit" size="lg" className="h-11 px-4" disabled={mutation.isPending}>
              {mutation.isPending ? t.sectorForm.saving : sector ? t.sectorForm.save : t.sectorForm.create}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

/** Criar ou editar mesa (com table: editar rótulo e setor). */
export function TableFormDialog({
  open,
  onOpenChange,
  sectors,
  table,
  defaultSectorId,
  onSave,
}: Open & {
  sectors: Sector[];
  table?: DiningTable;
  defaultSectorId?: string;
  onSave: (values: { label: string; sectorId: string }) => Promise<unknown>;
}) {
  const mutation = useMutation({ mutationFn: onSave, onSuccess: () => onOpenChange(false) });

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    mutation.mutate({
      label: String(form.get("label") ?? "").trim(),
      sectorId: String(form.get("sectorId") ?? ""),
    });
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) mutation.reset();
        onOpenChange(next);
      }}
    >
      <DialogContent className={contentClass}>
        <DialogHeader>
          <DialogTitle className="text-lg">
            {table ? t.tableForm.editTitle(table.label) : t.tableForm.createTitle}
          </DialogTitle>
        </DialogHeader>
        <form onSubmit={onSubmit} className="flex flex-col gap-5">
          <div className="flex flex-col gap-2">
            <Label htmlFor="table-label" className="text-muted-foreground">
              {t.tableForm.label}
            </Label>
            <Input
              id="table-label"
              name="label"
              required
              maxLength={10}
              autoComplete="off"
              defaultValue={table?.label}
              placeholder={t.tableForm.labelPlaceholder}
              aria-describedby="table-label-hint"
              className="h-12 rounded-xl px-4 font-mono text-base"
            />
            <span id="table-label-hint" className="text-sm text-muted-foreground">
              {t.tableForm.labelHint}
            </span>
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="table-sector" className="text-muted-foreground">
              {t.tableForm.sector}
            </Label>
            <select
              id="table-sector"
              name="sectorId"
              required
              defaultValue={table?.sectorId ?? defaultSectorId ?? sectors[0]?.id}
              className="h-12 rounded-xl border border-input bg-transparent px-3 text-base dark:bg-input/30"
            >
              {sectors.map((sector) => (
                <option key={sector.id} value={sector.id}>
                  {sector.name}
                </option>
              ))}
            </select>
          </div>
          <p role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
            {mutation.error?.message ?? ""}
          </p>
          <DialogFooter className={footerClass}>
            <DialogClose render={<Button variant="outline" size="lg" className="h-11 px-4" />}>
              {t.tableForm.cancel}
            </DialogClose>
            <Button type="submit" size="lg" className="h-11 px-4" disabled={mutation.isPending}>
              {mutation.isPending ? t.tableForm.saving : table ? t.tableForm.save : t.tableForm.create}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

/** Confirmação de uma ação que não dá para desfazer sem trabalho (trocar QR, desativar setor). */
export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel,
  onConfirm,
}: Open & { title: string; description: string; confirmLabel: string; onConfirm: () => Promise<unknown> }) {
  const mutation = useMutation({ mutationFn: onConfirm, onSuccess: () => onOpenChange(false) });

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) mutation.reset();
        onOpenChange(next);
      }}
    >
      <DialogContent className={contentClass} role="alertdialog">
        <DialogHeader>
          <DialogTitle className="text-lg">{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        <p role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
          {mutation.error?.message ?? ""}
        </p>
        <DialogFooter className={footerClass}>
          <DialogClose render={<Button variant="outline" size="lg" className="h-11 px-4" />}>
            {t.confirm.cancel}
          </DialogClose>
          <Button
            variant="destructive"
            size="lg"
            className="h-11 px-4"
            disabled={mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            {mutation.isPending ? t.confirm.working : confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
