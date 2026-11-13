"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ImagePlus, Plus, Trash2, X } from "lucide-react";
import { useId, useState, type ChangeEvent, type DragEvent, type FormEvent } from "react";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { ToggleRow } from "@/components/toggle-row";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { fetchCategories, type MenuCategory } from "@/features/menu/menu-api";
import { canManageTeam } from "@/features/team/team-rules";
import { useSession } from "@/lib/auth/use-session";
import { centsToInput, parseCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import {
  createRodizio,
  deactivateRodizio,
  reactivateRodizio,
  fetchAllProducts,
  fetchRodizios,
  updateRodizio,
  type MenuProduct,
  type Rodizio,
  BANNER_MAX_BYTES,
  BANNER_TYPES,
  removeRodizioBanner,
  uploadRodizioBanner,
} from "./rodizio-api";
import {
  DAYS,
  daysSummary,
  hasErrors,
  MAX_RODIZIOS,
  MAX_SPECIALS,
  MAX_TIERS,
  newSpecial,
  newTier,
  NUMERIC_RULES,
  priceError,
  ruleError,
  specialError,
  toDraft,
  toRequest,
  type Draft,
  type NumericRule,
  type SpecialDraft,
  type TierDraft,
} from "./rodizio-draft";

const t = messages.rodizio;
const DESCRIPTION_MAX = 280;
const NEW = "new";

/** Rodízios (desenho 12.4): a lista à esquerda e o rodízio escolhido à direita. */
export function RodizioManager() {
  const { status, session } = useSession();
  const allowed = status === "authenticated" && canManageTeam(session.establishment.role);

  const rodizios = useQuery({ queryKey: ["rodizios"], queryFn: fetchRodizios, enabled: allowed });
  const categories = useQuery({ queryKey: ["menu-categories"], queryFn: fetchCategories, enabled: allowed });
  const products = useQuery({ queryKey: ["products", "all"], queryFn: fetchAllProducts, enabled: allowed });
  const [chosen, setChosen] = useState<string | null>(null);
  // O aviso de "salvo" fica aqui: o editor recomeça quando o rodízio volta do back.
  const [savedId, setSavedId] = useState<string | null>(null);
  const choose = (next: string | null) => {
    setChosen(next);
    setSavedId(null);
  };

  if (status !== "authenticated" || (allowed && (rodizios.isPending || categories.isPending || products.isPending))) {
    return <p className="px-4 py-6 text-muted-foreground sm:px-7">{t.loading}</p>;
  }
  if (!allowed) {
    return (
      <p role="alert" className="px-4 py-6 text-muted-foreground sm:px-7">
        {messages.team.noPermission}
      </p>
    );
  }
  if (!rodizios.data || !categories.data || !products.data) {
    return (
      <p role="alert" className="px-4 py-6 text-destructive sm:px-7">
        {t.loadError}
      </p>
    );
  }

  const list = rodizios.data;
  // Sem escolha (ou o escolhido saiu da lista), abre o primeiro; sem nenhum, o formulário de um novo.
  const selected = chosen === NEW ? undefined : (list.find((r) => r.id === chosen) ?? list[0]);
  const selectedKey = selected?.id ?? NEW;

  return (
    <div className="flex flex-col gap-6 px-4 py-6 sm:px-7">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">{t.title}</h1>
        <p className="text-sm text-muted-foreground">{t.subtitle}</p>
      </header>

      <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
        <nav
          aria-label={t.listLabel}
          className="flex flex-col gap-1 rounded-2xl border border-border bg-card p-2 lg:w-64"
        >
          {list.length === 0 && <p className="px-3 py-2 text-sm text-muted-foreground">{t.empty}</p>}
          {list.map((r) => (
            <button
              key={r.id}
              type="button"
              aria-current={r.id === selectedKey ? "page" : undefined}
              onClick={() => choose(r.id)}
              className={cn(
                "flex min-h-11 items-center gap-2 rounded-xl px-3 text-left font-medium hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                r.id === selectedKey && "bg-muted",
                !r.active && "text-muted-foreground",
              )}
            >
              <span className="min-w-0 flex-1 truncate">{r.name}</span>
              <span className={cn("shrink-0 text-xs", r.active ? "text-success" : "text-muted-foreground")}>
                {r.active ? t.status.active : t.status.paused}
              </span>
            </button>
          ))}
          <div className="my-1 border-t border-border" />
          <button
            type="button"
            aria-current={selectedKey === NEW ? "page" : undefined}
            disabled={list.filter((r) => r.active).length >= MAX_RODIZIOS}
            onClick={() => choose(NEW)}
            className={cn(
              "flex min-h-11 items-center gap-2 rounded-xl px-3 text-left font-semibold text-primary hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none disabled:text-muted-foreground",
              selectedKey === NEW && "bg-muted",
            )}
          >
            <Plus aria-hidden className="size-4" />
            {list.filter((r) => r.active).length >= MAX_RODIZIOS ? t.limit(MAX_RODIZIOS) : t.newRodizio}
          </button>
        </nav>

        {/* A chave recomeça o formulário ao trocar de rodízio e quando ele volta salvo do back. */}
        <RodizioEditor
          key={`${selectedKey}-${rodizios.dataUpdatedAt}`}
          rodizio={selected}
          categories={categories.data}
          products={products.data}
          justSaved={savedId !== null && savedId === selected?.id}
          onSaved={(id) => {
            setChosen(id);
            setSavedId(id);
          }}
          onEdited={() => setSavedId(null)}
          onDeactivated={() => {}}
        />
      </div>
    </div>
  );
}

function RodizioEditor({
  rodizio,
  categories,
  products,
  justSaved,
  onSaved,
  onEdited,
  onDeactivated,
}: {
  rodizio?: Rodizio;
  categories: MenuCategory[];
  products: MenuProduct[];
  justSaved: boolean;
  onSaved: (id: string) => void;
  onEdited: () => void;
  onDeactivated: () => void;
}) {
  const queryClient = useQueryClient();
  const id = useId();
  const [draft, setDraft] = useState<Draft>(() => toDraft(rodizio));
  const [touched, setTouched] = useState(false);
  const [confirming, setConfirming] = useState(false);

  const reactivate = useMutation({
    mutationFn: () => reactivateRodizio(rodizio!.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["rodizios"] }),
  });

  const save = useMutation({
    mutationFn: () => (rodizio ? updateRodizio(rodizio.id, toRequest(draft)) : createRodizio(toRequest(draft))),
    onSuccess: async (saved) => {
      await queryClient.invalidateQueries({ queryKey: ["rodizios"] });
      onSaved(saved.id);
    },
  });

  function update(change: Partial<Draft>) {
    setDraft((d) => ({ ...d, ...change }));
    if (justSaved) onEdited();
  }
  function updateTier(key: string, change: Partial<TierDraft>) {
    update({ tiers: draft.tiers.map((tier) => (tier.key === key ? { ...tier, ...change } : tier)) });
  }

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setTouched(true);
    if (hasErrors(draft)) return;
    save.mutate();
  }

  const formId = `${id}form`;

  return (
    <section aria-label={rodizio?.name ?? t.newTitle} className="flex min-w-0 flex-1 flex-col gap-4 lg:max-w-4xl">
      <div className="flex flex-wrap items-center gap-3">
        <h2 className="mr-auto text-xl font-semibold break-words">{rodizio?.name ?? t.newTitle}</h2>
        {rodizio?.active && (
          <Button
            variant="ghost"
            size="xl"
            className="text-destructive hover:text-destructive"
            onClick={() => setConfirming(true)}
          >
            {t.deactivate}
          </Button>
        )}
        {rodizio && !rodizio.active && (
          <Button variant="secondary" size="xl" disabled={reactivate.isPending} onClick={() => reactivate.mutate()}>
            {reactivate.isPending ? t.reactivating : t.reactivate}
          </Button>
        )}
        <Button type="submit" form={formId} size="xl" disabled={save.isPending}>
          {save.isPending ? t.saving : rodizio ? t.save : t.create}
        </Button>
      </div>
      {rodizio && !rodizio.active && (
        <p role="status" className="rounded-xl border border-border bg-muted px-4 py-3 text-sm text-muted-foreground">
          {t.pausedNotice}
        </p>
      )}
      {reactivate.error && (
        <p role="alert" className="text-sm text-destructive">
          {reactivate.error.message}
        </p>
      )}

      <form id={formId} onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
        <Panel>
          <div className="flex flex-col gap-2">
            <Label htmlFor={`${id}name`} className="text-muted-foreground">
              {t.name}
            </Label>
            <Input
              id={`${id}name`}
              value={draft.name}
              onChange={(e) => update({ name: e.target.value })}
              maxLength={60}
              autoComplete="off"
              placeholder={t.namePlaceholder}
              aria-invalid={(touched && draft.name.trim() === "") || undefined}
              className="h-12 rounded-xl px-4 text-base"
            />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor={`${id}description`} className="text-muted-foreground">
              {t.description}
            </Label>
            <textarea
              id={`${id}description`}
              value={draft.description}
              onChange={(e) => update({ description: e.target.value })}
              maxLength={DESCRIPTION_MAX}
              rows={2}
              placeholder={t.descriptionPlaceholder}
              aria-describedby={`${id}description-hint`}
              className="min-h-20 rounded-xl border border-input bg-transparent px-4 py-3 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 dark:bg-input/30"
            />
            <span id={`${id}description-hint`} className="text-sm text-muted-foreground">
              {t.descriptionHint(draft.description.length, DESCRIPTION_MAX)}
            </span>
          </div>
          <fieldset className="flex flex-col gap-2">
            <legend className="pb-2 text-sm font-medium text-muted-foreground">{t.type}</legend>
            <div className="flex gap-1.5 rounded-2xl border border-border bg-muted/40 p-1">
              {(["A_LA_MINUTE", "PASSAGEM"] as const).map((type) => (
                <button
                  key={type}
                  type="button"
                  aria-pressed={draft.type === type}
                  onClick={() => update({ type })}
                  className={cn(
                    "flex min-h-14 flex-1 flex-col items-center justify-center gap-0.5 rounded-xl px-2 text-sm font-semibold text-muted-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                    draft.type === type && "bg-background text-foreground shadow-sm",
                  )}
                >
                  {t.types[type].title}
                  <span className="text-xs font-normal">{t.types[type].text}</span>
                </button>
              ))}
            </div>
          </fieldset>
        </Panel>
        <BannerPanel rodizio={rodizio} />

        <Panel>
          <PanelTitle title={t.prices.title} text={t.prices.text} />
          {draft.tiers.map((tier, index) => (
            <TierEditor
              key={tier.key}
              tier={tier}
              index={index}
              touched={touched}
              canRemove={draft.tiers.length > 1}
              onChange={(change) => updateTier(tier.key, change)}
              onRemove={() => update({ tiers: draft.tiers.filter((x) => x.key !== tier.key) })}
            />
          ))}
          <Button
            type="button"
            variant="outline"
            className="self-start"
            disabled={draft.tiers.length >= MAX_TIERS}
            onClick={() => update({ tiers: [...draft.tiers, newTier()] })}
          >
            {draft.tiers.length >= MAX_TIERS ? t.prices.maxTiers(MAX_TIERS) : t.prices.addTier}
          </Button>
        </Panel>

        <ProductPicker
          categories={categories}
          products={products}
          selected={draft.productIds}
          onChange={(productIds) => update({ productIds })}
        />

        <Panel>
          <PanelTitle title={t.rules.title} text={t.rules.text} />
          <div className="flex flex-col gap-1">
            {NUMERIC_RULES.map((rule) => (
              <NumericRuleRow
                key={rule}
                rule={rule}
                value={draft.rules[rule]}
                touched={touched}
                onChange={(value) => update({ rules: { ...draft.rules, [rule]: value } })}
              />
            ))}
            <ToggleRow
              title={t.rules.wasteNotice.title}
              text={t.rules.wasteNotice.text}
              checked={draft.wasteNotice !== null}
              onChange={(on) => update({ wasteNotice: on ? "" : null })}
            />
            {draft.wasteNotice !== null && (
              <Input
                aria-label={t.rules.wasteNotice.title}
                value={draft.wasteNotice}
                onChange={(e) => update({ wasteNotice: e.target.value })}
                maxLength={200}
                placeholder={t.rules.wasteNotice.placeholder}
                className="mb-2 h-11 rounded-xl px-4 text-base"
              />
            )}
          </div>
        </Panel>

        <p role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
          {touched && hasErrors(draft) ? t.errors.fields : (save.error?.message ?? "")}
        </p>
        {/* O salvar também no fim: a página é longa, e não precisa rolar até o topo. */}
        <div className="flex justify-end">
          <Button type="submit" size="xl" disabled={save.isPending}>
            {save.isPending ? t.saving : rodizio ? t.save : t.create}
          </Button>
        </div>
        <p role="status" className="sr-only">
          {justSaved ? t.saved : ""}
        </p>
      </form>

      {rodizio && (
        <ConfirmDialog
          open={confirming}
          onOpenChange={setConfirming}
          title={t.confirm.title(rodizio.name)}
          description={t.confirm.text}
          confirmLabel={t.confirm.action}
          onConfirm={async () => {
            await deactivateRodizio(rodizio.id);
            await queryClient.invalidateQueries({ queryKey: ["rodizios"] });
            onDeactivated();
          }}
        />
      )}
    </section>
  );
}

function Panel({ children }: { children: React.ReactNode }) {
  return <div className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-5">{children}</div>;
}

function PanelTitle({ title, text }: { title: string; text: string }) {
  return (
    <div className="flex flex-col gap-0.5">
      <h3 className="font-semibold">{title}</h3>
      <p className="text-sm text-muted-foreground">{text}</p>
    </div>
  );
}

/** Ao sair do campo, o preço fica no formato de sempre ("27,9" vira "27,90"). */
function tidy(value: string) {
  const cents = parseCents(value);
  return cents !== null && !Number.isNaN(cents) ? centsToInput(cents) : value;
}

function MoneyInput({
  id,
  label,
  value,
  onChange,
  error,
  className,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  error: string | null;
  className?: string;
}) {
  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      <Label htmlFor={id} className="text-sm text-muted-foreground">
        {label}
      </Label>
      <div className="relative">
        <span
          aria-hidden
          className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-sm font-semibold text-muted-foreground"
        >
          R$
        </span>
        <Input
          id={id}
          inputMode="decimal"
          autoComplete="off"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onBlur={() => onChange(tidy(value))}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? `${id}-error` : undefined}
          className="h-11 rounded-xl pr-3 pl-10 font-mono text-base"
        />
      </div>
      {error && (
        <span id={`${id}-error`} className="text-sm text-destructive">
          {error}
        </span>
      )}
    </div>
  );
}

function TierEditor({
  tier,
  index,
  touched,
  canRemove,
  onChange,
  onRemove,
}: {
  tier: TierDraft;
  index: number;
  touched: boolean;
  canRemove: boolean;
  onChange: (change: Partial<TierDraft>) => void;
  onRemove: () => void;
}) {
  const id = useId();
  const label = tier.name.trim() || String(index + 1);

  function updateSpecial(key: string, change: Partial<SpecialDraft>) {
    onChange({ specials: tier.specials.map((s) => (s.key === key ? { ...s, ...change } : s)) });
  }

  return (
    <div role="group" aria-label={label} className="flex flex-col gap-3 rounded-xl border border-border p-4">
      <div className="flex flex-wrap items-end gap-3">
        <div className="flex min-w-48 flex-1 flex-col gap-1.5">
          <Label htmlFor={`${id}name`} className="text-sm text-muted-foreground">
            {t.prices.tierName(index + 1)}
          </Label>
          <Input
            id={`${id}name`}
            value={tier.name}
            onChange={(e) => onChange({ name: e.target.value })}
            maxLength={40}
            autoComplete="off"
            placeholder={t.prices.tierPlaceholder}
            aria-invalid={(touched && tier.name.trim() === "") || undefined}
            className="h-11 rounded-xl px-3 text-base"
          />
        </div>
        <MoneyInput
          id={`${id}price`}
          label={t.prices.defaultPrice}
          value={tier.price}
          onChange={(price) => onChange({ price })}
          error={touched ? priceError(tier.price) : null}
          className="w-40"
        />
        {canRemove && (
          <Button
            type="button"
            variant="ghost"
            size="icon-lg"
            aria-label={t.prices.removeTier(label)}
            onClick={onRemove}
            className="mb-0.5"
          >
            <Trash2 aria-hidden />
          </Button>
        )}
      </div>

      {tier.specials.map((special) => (
        <SpecialEditor
          key={special.key}
          special={special}
          tierLabel={label}
          touched={touched}
          onChange={(change) => updateSpecial(special.key, change)}
          onRemove={() => onChange({ specials: tier.specials.filter((s) => s.key !== special.key) })}
        />
      ))}
      {tier.specials.length < MAX_SPECIALS && (
        <Button
          type="button"
          variant="ghost"
          className="self-start text-primary"
          onClick={() => onChange({ specials: [...tier.specials, newSpecial()] })}
        >
          {t.prices.addSpecial}
        </Button>
      )}
    </div>
  );
}

function SpecialEditor({
  special,
  tierLabel,
  touched,
  onChange,
  onRemove,
}: {
  special: SpecialDraft;
  tierLabel: string;
  touched: boolean;
  onChange: (change: Partial<SpecialDraft>) => void;
  onRemove: () => void;
}) {
  const id = useId();
  const error = touched ? specialError(special) : null;
  const priceInvalid = touched && priceError(special.price) !== null;
  const summary = `${t.prices.special} · ${daysSummary(special.days)}`;

  return (
    <div
      role="group"
      aria-label={`${tierLabel}: ${summary}`}
      className="flex flex-col gap-3 rounded-xl bg-muted/50 p-3 sm:ml-4"
    >
      <div className="flex items-center gap-2">
        <span className="mr-auto text-sm font-semibold">{summary}</span>
        <Button type="button" variant="ghost" size="icon" aria-label={t.prices.removeSpecial} onClick={onRemove}>
          <X aria-hidden />
        </Button>
      </div>
      <fieldset className="flex flex-col gap-1.5">
        <legend className="pb-1.5 text-sm text-muted-foreground">{t.prices.days}</legend>
        <div className="flex flex-wrap gap-1.5">
          {DAYS.map((day) => {
            const on = special.days.includes(day);
            return (
              <button
                key={day}
                type="button"
                aria-pressed={on}
                aria-label={t.days[day].long}
                onClick={() =>
                  onChange({
                    days: on
                      ? special.days.filter((d) => d !== day)
                      : DAYS.filter((d) => d === day || special.days.includes(d)),
                  })
                }
                className={cn(
                  "h-10 min-w-12 rounded-full border border-border px-3 text-sm font-semibold focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                  on && "border-transparent bg-foreground text-background",
                )}
              >
                {t.days[day].short}
              </button>
            );
          })}
        </div>
      </fieldset>
      <div className="flex flex-wrap items-start gap-3">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor={`${id}from`} className="text-sm text-muted-foreground">
            {t.prices.from}
          </Label>
          <Input
            id={`${id}from`}
            type="time"
            value={special.startsAt}
            onChange={(e) => onChange({ startsAt: e.target.value })}
            className="h-11 w-32 rounded-xl px-3 text-base"
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor={`${id}to`} className="text-sm text-muted-foreground">
            {t.prices.to}
          </Label>
          <Input
            id={`${id}to`}
            type="time"
            value={special.endsAt}
            onChange={(e) => onChange({ endsAt: e.target.value })}
            className="h-11 w-32 rounded-xl px-3 text-base"
          />
        </div>
        <MoneyInput
          id={`${id}price`}
          label={t.prices.price}
          value={special.price}
          onChange={(price) => onChange({ price })}
          error={priceInvalid ? t.errors.invalidPrice : null}
          className="w-40"
        />
      </div>
      {error && !priceInvalid ? (
        <p className="text-sm text-destructive">{error}</p>
      ) : (
        <p className="text-xs text-muted-foreground">{t.prices.untilMidnight}</p>
      )}
    </div>
  );
}

function NumericRuleRow({
  rule,
  value,
  touched,
  onChange,
}: {
  rule: NumericRule;
  value: string | null;
  touched: boolean;
  onChange: (value: string | null) => void;
}) {
  const r = t.rules[rule];
  const error = value !== null && touched ? ruleError(rule, value) : null;
  return (
    <>
      <ToggleRow
        title={r.title}
        text={r.text}
        checked={value !== null}
        onChange={(on) => onChange(on ? String(r.initial) : null)}
      />
      {value !== null && (
        <div className="mb-2 flex flex-col gap-1">
          <div className="flex items-center gap-3">
            <Input
              type="number"
              inputMode="numeric"
              min={r.min}
              max={r.max}
              aria-label={r.title}
              aria-invalid={error ? true : undefined}
              value={value}
              onChange={(e) => onChange(e.target.value)}
              className="h-11 w-28 rounded-xl px-3 font-mono text-base"
            />
            <span className="text-sm text-muted-foreground">{r.unit}</span>
          </div>
          {error && <span className="text-sm text-destructive">{error}</span>}
        </div>
      )}
    </>
  );
}

/** Os produtos incluídos: o resumo e, aberto, a escolha por categoria com busca. */
function ProductPicker({
  categories,
  products,
  selected,
  onChange,
}: {
  categories: MenuCategory[];
  products: MenuProduct[];
  selected: string[];
  onChange: (ids: string[]) => void;
}) {
  const id = useId();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const chosen = new Set(selected);
  // Um produto desativado no cardápio não aparece mais e sai da escolha ao salvar.
  const known = new Set(products.map((p) => p.id));
  const count = selected.filter((p) => known.has(p)).length;
  const text = query.trim().toLocaleLowerCase("pt-BR");
  const groups = categories
    .map((c) => ({
      category: c,
      items: products.filter(
        (p) => p.categoryId === c.id && (!text || p.name.toLocaleLowerCase("pt-BR").includes(text)),
      ),
    }))
    .filter((g) => g.items.length > 0);

  function set(ids: string[], on: boolean) {
    const next = new Set([...selected].filter((p) => known.has(p)));
    for (const p of ids) {
      if (on) next.add(p);
      else next.delete(p);
    }
    onChange([...next]);
  }

  return (
    <Panel>
      <div className="flex flex-wrap items-center gap-3">
        <div className="mr-auto flex flex-col gap-0.5">
          <h3 className="font-semibold">{t.products.title}</h3>
          <p className="text-sm text-muted-foreground" aria-live="polite">
            {t.products.count(count)}
          </p>
        </div>
        <Button type="button" variant="outline" aria-expanded={open} onClick={() => setOpen((o) => !o)}>
          {open ? t.products.done : t.products.choose}
        </Button>
      </div>
      {open &&
        (products.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t.products.empty}</p>
        ) : (
          <div className="flex flex-col gap-4">
            <Input
              type="search"
              aria-label={t.products.search}
              placeholder={t.products.search}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="h-11 rounded-xl px-4 text-base"
            />
            {groups.length === 0 && <p className="text-sm text-muted-foreground">{t.products.noResults}</p>}
            {groups.map(({ category, items }) => {
              const all = items.every((p) => chosen.has(p.id));
              return (
                <fieldset key={category.id} className="flex flex-col gap-1">
                  <legend className="flex w-full items-center gap-3 pb-1">
                    <span className="mr-auto text-sm font-semibold text-muted-foreground">{category.name}</span>
                  </legend>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="self-start text-primary"
                    aria-label={all ? t.products.noneOf(category.name) : t.products.allOf(category.name)}
                    onClick={() =>
                      set(
                        items.map((p) => p.id),
                        !all,
                      )
                    }
                  >
                    {all ? t.products.none : t.products.all}
                  </Button>
                  <div className="grid gap-x-4 sm:grid-cols-2">
                    {items.map((p) => (
                      <div key={p.id} className="flex min-h-11 items-center gap-3 px-1">
                        <Checkbox
                          id={`${id}${p.id}`}
                          checked={chosen.has(p.id)}
                          onCheckedChange={(on) => set([p.id], on === true)}
                        />
                        <Label htmlFor={`${id}${p.id}`} className="flex-1 font-normal">
                          {p.name}
                        </Label>
                      </div>
                    ))}
                  </div>
                </fieldset>
              );
            })}
          </div>
        ))}
    </Panel>
  );
}

/**
 * O banner do rodízio (tela "Escolha o rodízio" da mesa): a prévia no formato 2:1, o tamanho ideal,
 * e o envio direto ao armazenamento. Sem banner, a mesa usa a foto de um prato (automática).
 */
function BannerPanel({ rodizio }: { rodizio?: Rodizio }) {
  const inputId = useId();
  // O banner fica aqui, e não na lista: recarregar a lista recria o formulário e perderia o que
  // ainda não foi salvo nele.
  const [url, setUrl] = useState(rodizio?.bannerUrl ?? null);
  const [percent, setPercent] = useState<number | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const b = messages.rodizio.banner;

  const send = useMutation({
    mutationFn: async (file: File) => {
      if (!BANNER_TYPES.includes(file.type)) throw new Error(b.wrongType);
      if (file.size > BANNER_MAX_BYTES) throw new Error(b.tooBig);
      setPercent(0);
      return uploadRodizioBanner(rodizio!.id, file, (fraction) =>
        setPercent(fraction < 1 ? Math.round(fraction * 100) : null),
      );
    },
    onSuccess: (saved) => {
      setUrl(saved.bannerUrl ?? null);
      setNotice(b.sent);
    },
    onSettled: () => setPercent(null),
  });
  const remove = useMutation({
    mutationFn: () => removeRodizioBanner(rodizio!.id),
    onSuccess: (saved) => {
      setUrl(saved.bannerUrl ?? null);
      setNotice(b.removed);
    },
  });

  function pick(file: File | undefined) {
    if (!file) return;
    setNotice(null);
    remove.reset();
    send.mutate(file);
  }

  function onPick(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = ""; // a mesma imagem pode ser escolhida de novo
    pick(file);
  }

  function onDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault();
    setDragOver(false);
    pick(event.dataTransfer.files[0]);
  }

  const busy = send.isPending || remove.isPending;
  const error = send.error ?? remove.error;

  return (
    <Panel>
      <PanelTitle title={b.title} text={b.text} />
      {/* Como a foto do produto: a imagem enviada (com o X para voltar à automática) e a área tracejada,
        que aceita tocar ou arrastar a imagem. */}
      {url && (
        <div className="relative aspect-[2/1] w-full max-w-[420px] overflow-hidden rounded-xl border border-border bg-muted">
          {/* eslint-disable-next-line @next/next/no-img-element -- banner já no tamanho certo, vindo do R2 */}
          <img src={url} alt={b.preview(rodizio!.name)} className="size-full object-cover" />
          <Button
            type="button"
            variant="secondary"
            size="icon"
            aria-label={b.remove}
            title={b.remove}
            disabled={busy}
            onClick={() => remove.mutate()}
            className="absolute top-2 right-2 size-9 rounded-full bg-black/60 text-white hover:bg-black/80"
          >
            <X aria-hidden />
          </Button>
        </div>
      )}
      {rodizio ? (
        <>
          <input
            id={inputId}
            type="file"
            accept={BANNER_TYPES.join(",")}
            disabled={busy}
            onChange={onPick}
            className="sr-only"
          />
          <label
            htmlFor={inputId}
            onDragOver={(event) => {
              event.preventDefault();
              setDragOver(true);
            }}
            onDragLeave={() => setDragOver(false)}
            onDrop={onDrop}
            className={cn(
              "flex min-h-28 cursor-pointer flex-col items-center justify-center gap-1.5 rounded-xl border-2 border-dashed border-border px-4 py-5 text-center transition-colors hover:bg-muted has-[:focus-visible]:ring-3 has-[:focus-visible]:ring-ring/50",
              dragOver && "border-primary bg-muted",
              busy && "pointer-events-none opacity-60",
            )}
          >
            <ImagePlus aria-hidden className="size-6 text-muted-foreground" />
            <span className="font-medium">{dragOver ? b.drop : url ? b.replace : b.send}</span>
            <span className="text-sm text-muted-foreground">{b.ideal}</span>
          </label>
        </>
      ) : (
        <p className="text-sm font-medium">{b.saveFirst}</p>
      )}
      {!url && <p className="text-sm text-muted-foreground">{b.automatic}</p>}
      {send.isPending && (
        <div role="status" className="flex flex-col gap-1 text-sm">
          <span>{percent === null ? b.processing : b.sending(percent)}</span>
          <span aria-hidden className="h-1.5 overflow-hidden rounded-full bg-muted">
            <span className="block h-full bg-primary transition-[width]" style={{ width: `${percent ?? 100}%` }} />
          </span>
        </div>
      )}
      {notice && !busy && (
        <p role="status" className="text-sm text-success">
          {notice}
        </p>
      )}
      {error && (
        <p role="alert" className="text-sm text-destructive">
          {error.message}
        </p>
      )}
    </Panel>
  );
}
