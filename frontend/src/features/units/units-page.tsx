"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { LockedCard } from "@/features/reports/locked";
import { ErrorText } from "@/features/subscription/error-text";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { switchEstablishment } from "@/lib/auth/session";
import { initials } from "@/lib/initials";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { createUnit, fetchUnitAllowance, fetchUnits, UNITS_KEY, type Unit, type UnitAllowance } from "./units-api";

const t = messages.units;
const PAGE_SIZE = 20;
const ZONES = Object.keys(t.timeZones);

/** "12/03/2026" no fuso do restaurante. */
function day(iso: string, timeZone: string) {
  return new Intl.DateTimeFormat("pt-BR", { timeZone, dateStyle: "short" }).format(new Date(iso));
}

/**
 * Unidades (RF31, desenho 12.9): as unidades da empresa, com as mesas e a equipe de cada uma, o
 * "Entrar nesta unidade" e, ao lado, a "Nova unidade". Fora do Enterprise, o cartão do plano no
 * lugar do cadastro. Só o dono.
 */
export function UnitsPage() {
  const { status, session } = useRequiredSession();
  const owner = status === "authenticated" && session.establishment.role === "OWNER";
  const timeZone = session?.establishment.timeZone ?? "America/Sao_Paulo";
  const [page, setPage] = useState(0);

  const units = useQuery({
    queryKey: [...UNITS_KEY, page],
    queryFn: () => fetchUnits(page, PAGE_SIZE),
    enabled: owner,
    placeholderData: keepPreviousData,
  });
  const allowance = useQuery({ queryKey: [...UNITS_KEY, "allowance"], queryFn: fetchUnitAllowance, enabled: owner });

  return (
    <div className="flex flex-col gap-6 px-4 py-6 sm:px-7">
      <header className="flex flex-wrap items-end gap-4">
        <div className="mr-auto flex max-w-2xl flex-col gap-1">
          <h1 className="text-2xl font-bold tracking-tight">{t.title}</h1>
          <p className="text-sm text-muted-foreground">{t.subtitle}</p>
        </div>
        {allowance.data && <UnitMeter allowance={allowance.data} />}
      </header>

      {status !== "authenticated" ? (
        <p className="text-muted-foreground">{t.loading}</p>
      ) : !owner ? (
        <p role="alert" className="text-muted-foreground">
          {t.noPermission}
        </p>
      ) : units.isError || allowance.isError ? (
        <p role="alert" className="text-destructive">
          {t.loadError}
        </p>
      ) : !units.data || !allowance.data ? (
        <p className="text-muted-foreground">{t.loading}</p>
      ) : (
        <div className="flex flex-col gap-6 xl:flex-row xl:items-start">
          <section
            aria-label={t.list}
            className="flex min-w-0 flex-1 flex-col rounded-2xl border border-border bg-card px-5 py-1"
          >
            <ul className="flex flex-col">
              {units.data.content.map((unit) => (
                <UnitRow key={unit.id} unit={unit} timeZone={timeZone} />
              ))}
            </ul>
            {units.data.totalPages > 1 && (
              <div className="flex justify-end gap-2 border-t border-border py-3">
                <Button variant="outline" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                  {t.previous}
                </Button>
                <Button
                  variant="outline"
                  disabled={page + 1 >= units.data.totalPages}
                  onClick={() => setPage((p) => p + 1)}
                >
                  {t.next}
                </Button>
              </div>
            )}
          </section>
          <aside className="flex w-full flex-col xl:w-96">
            {allowance.data.canAddMore ? (
              <NewUnitForm defaultTimeZone={ZONES.includes(timeZone) ? timeZone : ZONES[0]} />
            ) : (
              <LockedCard title={t.locked.title} text={t.locked.text} badge={t.locked.badge} more={t.locked.more} />
            )}
          </aside>
        </div>
      )}
    </div>
  );
}

function UnitMeter({ allowance }: { allowance: UnitAllowance }) {
  const max = allowance.max ?? null;
  const percent = max === null ? Math.min(100, allowance.used * 10) : Math.min(100, (allowance.used / max) * 100);
  return (
    <div className="flex w-56 flex-col gap-2">
      <div className="flex items-baseline justify-between gap-3 text-[13px]">
        <span className="text-muted-foreground">{t.meter}</span>
        <span className="font-semibold tabular-nums">{t.meterValue(allowance.used, max)}</span>
      </div>
      <div aria-hidden className="h-1.5 overflow-hidden rounded-full bg-muted">
        <div
          className={cn("h-full rounded-full", allowance.canAddMore ? "bg-primary" : "bg-warning")}
          style={{ width: `${percent}%` }}
        />
      </div>
    </div>
  );
}

function UnitRow({ unit, timeZone }: { unit: Unit; timeZone: string }) {
  const [busy, setBusy] = useState(false);
  const [failed, setFailed] = useState(false);
  const today = day(unit.createdAt, timeZone) === day(new Date().toISOString(), timeZone);

  async function enter() {
    setBusy(true);
    setFailed(false);
    if (!(await switchEstablishment(unit.id))) {
      setBusy(false);
      setFailed(true);
    }
  }

  return (
    <li className="flex flex-wrap items-center gap-x-4 gap-y-2 border-b border-border py-4 last:border-b-0">
      <span
        aria-hidden
        className="flex size-9 shrink-0 items-center justify-center rounded-[10px] bg-primary text-[13px] font-bold text-primary-foreground"
      >
        {initials(unit.name)}
      </span>
      <span className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className="truncate font-semibold">{unit.name}</span>
        <span className="truncate text-sm text-muted-foreground">
          {today ? t.createdToday(unit.slug) : t.since(unit.slug, day(unit.createdAt, timeZone))}
        </span>
      </span>
      <span className="flex min-w-20 flex-col items-end">
        <span className="font-semibold tabular-nums">{unit.activeTables}</span>
        <span className="text-sm text-muted-foreground">{t.tables}</span>
      </span>
      <span className="flex min-w-20 flex-col items-end">
        <span className="font-semibold tabular-nums">{unit.activeTeam}</span>
        <span className="text-sm text-muted-foreground">{t.team}</span>
      </span>
      <span className="flex w-full justify-end sm:w-44">
        {unit.current ? (
          <span className="rounded-full bg-primary/15 px-3 py-1 text-xs font-semibold text-brand-text">
            {t.current}
          </span>
        ) : (
          <Button variant="outline" disabled={busy} onClick={() => void enter()} aria-label={t.enterLabel(unit.name)}>
            {busy ? messages.admin.switcher.switching : t.enter}
          </Button>
        )}
      </span>
      {failed && (
        <p role="alert" className="w-full text-right text-sm text-destructive">
          {messages.admin.switcher.error}
        </p>
      )}
    </li>
  );
}

function NewUnitForm({ defaultTimeZone }: { defaultTimeZone: string }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [timeZone, setTimeZone] = useState(defaultTimeZone);
  const [created, setCreated] = useState<Unit | null>(null);

  const creation = useMutation({
    mutationFn: () => createUnit(name.trim(), timeZone),
    onSuccess: async (unit) => {
      setCreated(unit);
      setName("");
      // O seletor (o /me) passa a mostrar a unidade nova.
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: UNITS_KEY }),
        queryClient.invalidateQueries({ queryKey: ["me"] }),
      ]);
    },
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    setCreated(null);
    creation.mutate();
  }

  return (
    <form
      onSubmit={submit}
      aria-labelledby="new-unit-title"
      className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-5"
    >
      <h2 id="new-unit-title" className="font-semibold">
        {t.form.title}
      </h2>
      <div className="flex flex-col gap-2">
        <Label htmlFor="unit-name">{t.form.name}</Label>
        <Input
          id="unit-name"
          value={name}
          maxLength={120}
          required
          aria-describedby="unit-name-hint"
          onChange={(e) => setName(e.target.value)}
          className="h-11"
        />
        <span id="unit-name-hint" className="text-sm text-muted-foreground">
          {t.form.nameHint}
        </span>
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor="unit-time-zone">{t.form.timeZone}</Label>
        <Select
          id="unit-time-zone"
          value={timeZone}
          onValueChange={setTimeZone}
          options={ZONES.map((zone) => ({ value: zone, label: t.timeZones[zone] }))}
        />
      </div>
      <p className="rounded-xl bg-secondary px-3.5 py-3 text-sm text-muted-foreground">{t.form.note}</p>
      {creation.isError && (
        <p role="alert" className="text-sm text-destructive">
          <ErrorText message={creation.error.message} />
        </p>
      )}
      <Button type="submit" size="xl" disabled={creation.isPending || name.trim() === ""}>
        {creation.isPending ? t.form.saving : t.form.submit}
      </Button>
      {created && (
        <div role="status" className="flex flex-col gap-2 rounded-xl border border-border px-3.5 py-3 text-sm">
          <span>{t.form.created(created.name)}</span>
          <Button variant="outline" onClick={() => void switchEstablishment(created.id)}>
            {t.form.enterNow}
          </Button>
        </div>
      )}
    </form>
  );
}
