"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowDown, ArrowUp, Plus, X } from "lucide-react";
import { useId, useState, type FormEvent } from "react";
import { ToggleRow } from "@/components/toggle-row";
import { Button } from "@/components/ui/button";
import { Dialog, DialogClose, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { centsToInput, parseCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import {
  fetchGroupPrices,
  saveGroupPrices,
  type OptionGroup,
  type OptionGroupPrice,
  type OptionGroupPriceColumn,
  type OptionGroupPrices,
  type OptionGroupRequest,
  type OptionPricing,
} from "./options-api";

const t = messages.options.form;
const TEMPLATES = messages.options.templates;
const PRICINGS: OptionPricing[] = ["SUM", "HIGHEST", "AVERAGE"];
const MAX_CHOICES = 20;
const MAX_PARTS = 4;
const BLANK = "blank";

type ItemDraft = {
  key: string;
  id?: string;
  name: string;
  description: string;
  price: string;
  available: boolean;
  /** O preço em cada coluna da grade (um tamanho de um produto): chave de {@link columnKey}; em branco, o padrão. */
  prices: Record<string, string>;
};

/** A chave de uma coluna da grade: o produto e o tamanho (ou o produto todo, sem tamanhos). */
const columnKey = (c: { productId: string; variantId?: string | null }) => `${c.productId}|${c.variantId ?? "produto"}`;

/** O nome da coluna: só o tamanho, quando todas são do mesmo produto; senão, o produto e o tamanho. */
function columnLabel(column: OptionGroupPriceColumn, columns: OptionGroupPriceColumn[]) {
  const oneProduct = columns.every((c) => c.productId === column.productId);
  if (!column.variantName) return column.productName;
  return oneProduct ? column.variantName : `${column.productName} · ${column.variantName}`;
}

let keys = 0;
const newKey = () => `item-${++keys}`;

/** O formulário do grupo: as regras e as opções, numa lista que se ordena com as setas. */
type Draft = {
  title: string;
  name: string;
  min: number;
  max: number;
  maxPerOption: number;
  splits: boolean;
  pricing: OptionPricing;
  free: number;
  items: ItemDraft[];
};

function fromGroup(group?: OptionGroup, grid?: OptionGroupPrices): Draft {
  if (!group) {
    return {
      title: "",
      name: "",
      min: 0,
      max: 1,
      maxPerOption: 1,
      splits: false,
      pricing: "SUM",
      free: 0,
      items: [{ key: newKey(), name: "", description: "", price: "", available: true, prices: {} }],
    };
  }
  return {
    title: group.title,
    name: group.name === group.title ? "" : group.name,
    min: group.minChoices,
    max: group.maxChoices,
    maxPerOption: group.maxPerOption,
    splits: group.splitsItem,
    pricing: group.pricing,
    free: group.freeQuantity,
    items: group.items.map((i) => ({
      key: newKey(),
      id: i.id,
      name: i.name,
      description: i.description,
      price: i.priceCents === 0 ? "" : centsToInput(i.priceCents),
      available: i.available,
      prices: Object.fromEntries(
        (grid?.prices ?? []).filter((p) => p.optionId === i.id).map((p) => [columnKey(p), centsToInput(p.priceCents)]),
      ),
    })),
  };
}

/** Criar (com modelos prontos) ou editar um grupo de opções. */
export function OptionGroupDialog({
  open,
  onOpenChange,
  group,
  onSave,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  group?: OptionGroup;
  /** Salva o grupo e devolve como ficou (as opções novas ganham id, para a grade de preços). */
  onSave: (body: OptionGroupRequest) => Promise<OptionGroup>;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] gap-5 overflow-y-auto p-6 sm:max-w-xl">
        <DialogHeader>
          <DialogTitle className="text-lg">{group ? t.editTitle(group.name) : t.newTitle}</DialogTitle>
        </DialogHeader>
        {/* A chave recomeça o formulário a cada abertura. */}
        {open && <GroupLoader group={group} onSave={onSave} onDone={() => onOpenChange(false)} />}
      </DialogContent>
    </Dialog>
  );
}

/** Busca a grade de preços do grupo (o grupo novo ainda não tem) antes de abrir o formulário. */
function GroupLoader({
  group,
  onSave,
  onDone,
}: {
  group?: OptionGroup;
  onSave: (body: OptionGroupRequest) => Promise<OptionGroup>;
  onDone: () => void;
}) {
  const grid = useQuery({
    queryKey: ["group-prices", group?.id],
    queryFn: () => fetchGroupPrices(group!.id),
    enabled: group !== undefined,
    staleTime: 0,
  });
  if (group && grid.isPending) return <p className="text-sm text-muted-foreground">{t.loadingPrices}</p>;
  if (group && grid.isError)
    return (
      <p role="alert" className="text-sm text-destructive">
        {grid.error.message}
      </p>
    );
  return <GroupForm key={group?.id ?? "new"} group={group} grid={grid.data} onSave={onSave} onDone={onDone} />;
}

function GroupForm({
  group,
  grid,
  onSave,
  onDone,
}: {
  group?: OptionGroup;
  grid?: OptionGroupPrices;
  onSave: (body: OptionGroupRequest) => Promise<OptionGroup>;
  onDone: () => void;
}) {
  const id = useId();
  const queryClient = useQueryClient();
  const columns = grid?.columns ?? [];
  const [draft, setDraft] = useState<Draft>(() => fromGroup(group, grid));
  const [template, setTemplate] = useState(BLANK);
  const [invalid, setInvalid] = useState(false);
  const mutation = useMutation({
    mutationFn: async ({ body, prices }: { body: OptionGroupRequest; prices: Record<string, number>[] }) => {
      const saved = await onSave(body);
      // As opções voltam na ordem do formulário: as novas ganham o id aqui, e só então entram na grade.
      if (columns.length > 0) {
        const rows: OptionGroupPrice[] = [];
        prices.forEach((byColumn, index) => {
          const optionId = saved.items[index]?.id;
          if (!optionId) return;
          for (const column of columns) {
            const cents = byColumn[columnKey(column)];
            if (cents !== undefined)
              rows.push({ productId: column.productId, variantId: column.variantId, optionId, priceCents: cents });
          }
        });
        await saveGroupPrices(saved.id, rows);
        await queryClient.invalidateQueries({ queryKey: ["group-prices", saved.id] });
      }
      return saved;
    },
    onSuccess: onDone,
  });
  const set = (patch: Partial<Draft>) => setDraft((d) => normalize({ ...d, ...patch }));

  function applyTemplate(key: string) {
    setTemplate(key);
    const tpl = TEMPLATES.find((x) => x.key === key);
    if (!tpl) return;
    setDraft(
      normalize({
        title: tpl.title,
        name: "",
        min: tpl.min,
        max: tpl.max,
        maxPerOption: tpl.maxPerOption,
        splits: tpl.splits,
        pricing: tpl.pricing,
        free: tpl.free,
        items: tpl.items.map((name) => ({
          key: newKey(),
          name,
          description: "",
          price: "",
          available: true,
          prices: {},
        })),
      }),
    );
  }

  function setItem(index: number, patch: Partial<ItemDraft>) {
    setDraft((d) => ({ ...d, items: d.items.map((it, i) => (i === index ? { ...it, ...patch } : it)) }));
  }

  function move(index: number, by: number) {
    setDraft((d) => {
      const items = [...d.items];
      const [item] = items.splice(index, 1);
      items.splice(index + by, 0, item);
      return { ...d, items };
    });
  }

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const prices = draft.items.map((it) => parseCents(it.price) ?? 0);
    // A grade: só o que foi preenchido, em centavos (em branco vale o preço padrão da opção).
    const grid = draft.items.map((it) => {
      const row: Record<string, number> = {};
      for (const c of columns) {
        const cents = parseCents(it.prices[columnKey(c)] ?? "");
        if (cents !== null) row[columnKey(c)] = cents;
      }
      return row;
    });
    if (prices.some((p) => Number.isNaN(p)) || grid.some((row) => Object.values(row).some((c) => Number.isNaN(c)))) {
      setInvalid(true);
      return;
    }
    setInvalid(false);
    mutation.mutate({
      prices: grid,
      body: {
        name: draft.name.trim() || undefined,
        title: draft.title.trim(),
        minChoices: draft.min,
        maxChoices: draft.max,
        maxPerOption: draft.maxPerOption,
        splitsItem: draft.splits,
        pricing: draft.pricing,
        freeQuantity: draft.pricing === "SUM" ? draft.free : 0,
        items: draft.items.map((it, i) => ({
          ...(it.id ? { id: it.id } : {}),
          name: it.name.trim(),
          ...(it.description.trim() ? { description: it.description.trim() } : {}),
          priceCents: prices[i],
          available: it.available,
        })),
      },
    });
  }

  const choices = Array.from({ length: (draft.splits ? MAX_PARTS : MAX_CHOICES) + 1 }, (_, n) => ({
    value: String(n),
    label: String(n),
  }));

  return (
    <form onSubmit={onSubmit} className="flex flex-col gap-5">
      {!group && (
        <div className="flex flex-col gap-2">
          <Label htmlFor={`${id}template`} className="text-muted-foreground">
            {t.template}
          </Label>
          <Select
            id={`${id}template`}
            value={template}
            onValueChange={applyTemplate}
            options={[{ value: BLANK, label: t.blank }, ...TEMPLATES.map((x) => ({ value: x.key, label: x.label }))]}
          />
        </div>
      )}

      <div className="flex flex-col gap-2">
        <Label htmlFor={`${id}title`} className="text-muted-foreground">
          {t.title}
        </Label>
        <Input
          id={`${id}title`}
          required
          maxLength={60}
          autoComplete="off"
          value={draft.title}
          onChange={(e) => set({ title: e.target.value })}
          placeholder={t.titlePlaceholder}
          className="h-12 rounded-xl px-4 text-base"
        />
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor={`${id}name`} className="text-muted-foreground">
          {t.name}
        </Label>
        <Input
          id={`${id}name`}
          maxLength={60}
          autoComplete="off"
          value={draft.name}
          onChange={(e) => set({ name: e.target.value })}
          aria-describedby={`${id}name-hint`}
          className="h-12 rounded-xl px-4 text-base"
        />
        <span id={`${id}name-hint`} className="text-sm text-muted-foreground">
          {t.nameHint}
        </span>
      </div>

      <div className="flex flex-col gap-4 rounded-xl border border-border p-4">
        <ToggleRow
          title={t.required}
          text={t.requiredText}
          checked={draft.min > 0}
          onChange={(on) => set({ min: on ? 1 : 0 })}
        />
        <div className="grid grid-cols-2 gap-4">
          {draft.min > 0 && (
            <div className="flex flex-col gap-2">
              <Label htmlFor={`${id}min`} className="text-muted-foreground">
                {t.min}
              </Label>
              <Select
                id={`${id}min`}
                value={String(draft.min)}
                onValueChange={(v) => set({ min: Number(v) })}
                options={choices.slice(1)}
              />
            </div>
          )}
          <div className="flex flex-col gap-2">
            <Label htmlFor={`${id}max`} className="text-muted-foreground">
              {t.max}
            </Label>
            <Select
              id={`${id}max`}
              value={String(draft.max)}
              onValueChange={(v) => set({ max: Number(v) })}
              options={choices.slice(1)}
            />
          </div>
        </div>
        <ToggleRow
          title={t.splits}
          text={t.splitsText}
          checked={draft.splits}
          onChange={(on) =>
            set({ splits: on, ...(on ? { pricing: "HIGHEST" as const, maxPerOption: 1, free: 0 } : {}) })
          }
        />
        {!draft.splits && (
          <ToggleRow
            title={t.repeat}
            text={t.repeatText}
            checked={draft.maxPerOption > 1}
            onChange={(on) => set({ maxPerOption: on ? Math.min(2, draft.max) : 1 })}
          />
        )}
        {!draft.splits && draft.maxPerOption > 1 && (
          <div className="flex flex-col gap-2">
            <Label htmlFor={`${id}repeat`} className="text-muted-foreground">
              {t.maxPerOption}
            </Label>
            <Select
              id={`${id}repeat`}
              value={String(draft.maxPerOption)}
              onValueChange={(v) => set({ maxPerOption: Number(v) })}
              options={choices.slice(2, draft.max + 1)}
            />
          </div>
        )}
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-2">
            <Label htmlFor={`${id}pricing`} className="text-muted-foreground">
              {t.pricing}
            </Label>
            <Select
              id={`${id}pricing`}
              value={draft.pricing}
              onValueChange={(v) => set({ pricing: v as OptionPricing })}
              options={PRICINGS.map((p) => ({ value: p, label: messages.options.pricing[p] }))}
            />
            <span className="text-sm text-muted-foreground">{t.pricingHints[draft.pricing]}</span>
          </div>
          {draft.pricing === "SUM" && (
            <div className="flex flex-col gap-2">
              <Label htmlFor={`${id}free`} className="text-muted-foreground">
                {t.free}
              </Label>
              <Select
                id={`${id}free`}
                value={String(draft.free)}
                onValueChange={(v) => set({ free: Number(v) })}
                options={choices.slice(0, draft.max + 1)}
              />
              <span className="text-sm text-muted-foreground">{t.freeHint}</span>
            </div>
          )}
        </div>
      </div>

      <fieldset className="flex flex-col gap-3">
        <legend className="mb-1 font-semibold">{t.items}</legend>
        <p className="-mt-1 text-sm text-muted-foreground">{t.itemsHint}</p>
        <p className="-mt-1 text-sm text-muted-foreground">{columns.length > 0 ? t.gridHint : t.noGrid}</p>
        <ol className="flex flex-col gap-2">
          {draft.items.map((item, index) => {
            const label = item.name || `${index + 1}`;
            return (
              <li key={item.key} className="flex flex-wrap items-center gap-2 rounded-xl border border-border p-2">
                <Input
                  aria-label={t.itemName(index + 1)}
                  required
                  maxLength={60}
                  autoComplete="off"
                  value={item.name}
                  onChange={(e) => setItem(index, { name: e.target.value })}
                  placeholder={t.itemPlaceholder}
                  className="h-11 min-w-0 flex-1 rounded-xl px-3 text-base"
                />
                <div className="relative w-28 shrink-0">
                  <span
                    aria-hidden
                    className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-xs font-semibold text-muted-foreground"
                  >
                    R$
                  </span>
                  <Input
                    aria-label={t.itemPrice(index + 1)}
                    inputMode="decimal"
                    autoComplete="off"
                    value={item.price}
                    placeholder="0,00"
                    onChange={(e) => setItem(index, { price: e.target.value })}
                    onBlur={() => {
                      const cents = parseCents(item.price);
                      if (cents !== null && !Number.isNaN(cents)) setItem(index, { price: centsToInput(cents) });
                    }}
                    className="h-11 rounded-xl pr-2 pl-9 font-mono text-base"
                  />
                </div>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-lg"
                  className="size-11 shrink-0"
                  aria-label={t.moveUp(label)}
                  disabled={index === 0}
                  onClick={() => move(index, -1)}
                >
                  <ArrowUp aria-hidden />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-lg"
                  className="size-11 shrink-0"
                  aria-label={t.moveDown(label)}
                  disabled={index === draft.items.length - 1}
                  onClick={() => move(index, 1)}
                >
                  <ArrowDown aria-hidden />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-lg"
                  className="size-11 shrink-0"
                  aria-label={t.remove(label)}
                  disabled={draft.items.length === 1}
                  onClick={() => setDraft((d) => ({ ...d, items: d.items.filter((_, i) => i !== index) }))}
                >
                  <X aria-hidden />
                </Button>
                <Input
                  aria-label={t.itemDescription(label)}
                  maxLength={300}
                  autoComplete="off"
                  value={item.description}
                  onChange={(e) => setItem(index, { description: e.target.value })}
                  placeholder={t.itemDescriptionPlaceholder}
                  className="h-11 w-full rounded-xl px-3 text-sm"
                />
                {columns.length > 0 && (
                  <div className="flex w-full flex-wrap gap-2">
                    {columns.map((column) => {
                      const k = columnKey(column);
                      const name = columnLabel(column, columns);
                      return (
                        <label key={k} className="flex w-28 flex-col gap-1 text-xs text-muted-foreground">
                          <span className="truncate">{name}</span>
                          <PriceInput
                            label={t.itemPriceIn(label, name)}
                            value={item.prices[k] ?? ""}
                            placeholder={item.price || "0,00"}
                            onChange={(value) => setItem(index, { prices: { ...item.prices, [k]: value } })}
                          />
                        </label>
                      );
                    })}
                  </div>
                )}
              </li>
            );
          })}
        </ol>
        <Button
          type="button"
          variant="outline"
          className="h-11 self-start"
          disabled={draft.items.length >= 50}
          onClick={() =>
            setDraft((d) => ({
              ...d,
              items: [...d.items, { key: newKey(), name: "", description: "", price: "", available: true, prices: {} }],
            }))
          }
        >
          <Plus aria-hidden />
          {t.addItem}
        </Button>
      </fieldset>

      <p role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
        {invalid ? t.invalidPrice : (mutation.error?.message ?? "")}
      </p>

      <DialogFooter className="-mx-6 -mb-6 gap-2 px-6 pb-6">
        <DialogClose render={<Button variant="outline" size="lg" className="h-11 px-4" />}>{t.cancel}</DialogClose>
        <Button type="submit" size="lg" className="h-11 px-4" disabled={mutation.isPending}>
          {mutation.isPending ? t.saving : group ? t.save : t.create}
        </Button>
      </DialogFooter>
    </form>
  );
}

/** Um campo de preço curto da grade, que se arruma ao sair ("27,9" vira "27,90"). */
function PriceInput({
  label,
  value,
  placeholder,
  onChange,
}: {
  label: string;
  value: string;
  placeholder: string;
  onChange: (value: string) => void;
}) {
  return (
    <div className="relative">
      <span
        aria-hidden
        className="pointer-events-none absolute top-1/2 left-2.5 -translate-y-1/2 text-[11px] font-semibold text-muted-foreground"
      >
        R$
      </span>
      <Input
        aria-label={label}
        inputMode="decimal"
        autoComplete="off"
        value={value}
        placeholder={placeholder}
        aria-invalid={Number.isNaN(parseCents(value)) || undefined}
        onChange={(e) => onChange(e.target.value)}
        onBlur={() => {
          const cents = parseCents(value);
          if (cents !== null && !Number.isNaN(cents)) onChange(centsToInput(cents));
        }}
        className="h-10 rounded-xl pr-2 pl-8 font-mono text-sm"
      />
    </div>
  );
}

/** Mantém as regras coerentes enquanto a pessoa mexe (as mesmas que o back confere). */
function normalize(d: Draft): Draft {
  const limit = d.splits ? MAX_PARTS : MAX_CHOICES;
  const max = Math.min(Math.max(d.max, 1), limit);
  const min = Math.min(d.min, max);
  const maxPerOption = d.splits ? 1 : Math.min(Math.max(d.maxPerOption, 1), max);
  const free = d.pricing === "SUM" ? Math.min(d.free, max) : 0;
  return { ...d, max, min, maxPerOption, free };
}
