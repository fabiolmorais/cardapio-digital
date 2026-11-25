"use client";

import { ChartColumn, Check, ChevronDown, Download, Lock } from "lucide-react";
import Link from "next/link";
import { useEffect, useId, useRef, useState, type KeyboardEvent, type ReactNode } from "react";
import { Button, buttonVariants } from "@/components/ui/button";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { DateRangeDialog } from "./date-range-dialog";
import { formatDay, formatRange, PERIOD_KEYS, previousOf, type Period } from "./period";
import {
  canSeeReports,
  exportReport,
  useReportContext,
  useReportSectors,
  type ExportFormat,
  type ExportScreen,
  type ReportContext,
} from "./reports-api";
import { useReportFilters } from "./use-report-filters";

const t = messages.reports;

/** A ordem do menu "Exportar": o Excel primeiro, que é o que o dono mais abre. */
const EXPORT_FORMATS: ExportFormat[] = ["XLSX", "PDF", "CSV"];

export type ReportFilters = ReturnType<typeof useReportFilters>;

/**
 * A moldura de cada tela de relatório na gestão (ADR 0013): confere quem pode ver, busca o contexto
 * (plano, fuso, "hoje"), mostra o Essencial (14.10), o cabeçalho com o período e o setor, e o aviso de
 * sem dados (14.11). O corpo de cada tela vem em `children`.
 */
export function ReportsScreen({
  title,
  filter,
  businessOnly,
  units = false,
  exportAs,
  children,
}: {
  title: string;
  /**
   * A comparação entre unidades (REL-80): sem o setor (cada unidade tem os seus), sem o aviso de "sem
   * dados" da unidade de agora (as outras podem ter), e só para quem o back libera.
   */
  units?: boolean;
  /** A planilha desta tela (o botão "Exportar", ligado só nos planos com exportação). */
  exportAs?: ExportScreen;
  /** Um filtro a mais, só desta tela (a categoria, no Cardápio). */
  filter?: (filters: ReportFilters) => ReactNode;
  /** A tela é toda do Business: no Pro, só o que ela traz (o texto) e o "Conhecer o Business". */
  businessOnly?: string;
  children: (context: ReportContext, filters: ReportFilters) => ReactNode;
}) {
  return (
    <ReportsGate>
      {(context) =>
        businessOnly && context.level !== "FULL" ? (
          <BusinessOnly title={title} text={businessOnly} />
        ) : units && !context.unitComparison ? (
          <UnitsUnavailable title={title} />
        ) : (
          <Screen title={title} context={context} filter={filter} exportAs={exportAs} units={units}>
            {children}
          </Screen>
        )
      }
    </ReportsGate>
  );
}

/** A comparação entre unidades sem acesso (outro plano, uma unidade só, ou não ser dono de todas). */
function UnitsUnavailable({ title }: { title: string }) {
  return (
    <div className="flex flex-col gap-3 px-4 py-6 sm:px-7">
      <h1 className="text-[28px] leading-[34px] font-bold tracking-tight">{title}</h1>
      <p role="status" className="max-w-2xl text-muted-foreground">
        {t.units.unavailable}
      </p>
      <Link href="/admin/unidades" className={cn(buttonVariants({ variant: "outline" }), "h-11 w-fit")}>
        {t.units.manage}
      </Link>
    </div>
  );
}

/** Uma tela só do Business, vista no Pro: o que ela traz e o caminho para o plano. */
function BusinessOnly({ title, text }: { title: string; text: string }) {
  const b = t.businessOnly;
  return (
    <div className="flex flex-col gap-5 px-4 py-6 sm:px-7">
      <h1 className="text-[28px] leading-[34px] font-bold tracking-tight">{title}</h1>
      <section
        aria-labelledby="relatorio-business"
        className="flex flex-col gap-3 rounded-[20px] border border-dashed border-border bg-secondary/40 p-6 sm:p-8"
      >
        <span className="inline-flex w-fit items-center gap-1.5 rounded-full bg-info/15 px-2.5 py-0.5 text-xs font-semibold text-info">
          <Lock aria-hidden className="size-3.5" />
          {b.badge}
        </span>
        <h2 id="relatorio-business" className="max-w-2xl text-xl leading-7 font-bold">
          {b.title(title)}
        </h2>
        <p className="max-w-2xl text-muted-foreground">{text}</p>
        <Link href="/admin/assinatura" className={cn(buttonVariants(), "mt-1 h-11 w-fit")}>
          {b.more}
        </Link>
      </section>
    </div>
  );
}

/**
 * Quem pode ver os relatórios: só o dono e o gerente (os outros veem o aviso), com o plano que os
 * tem (o Essencial vê o que o Pro e o Business trazem). As telas sem período (o resumo diário) usam
 * só isto.
 */
export function ReportsGate({ children }: { children: (context: ReportContext) => ReactNode }) {
  const { status, session } = useRequiredSession();
  const access = status === "authenticated" ? session.establishment : null;
  const allowed = access !== null && canSeeReports(access.role);
  const context = useReportContext(allowed);

  if (access && !allowed) {
    return (
      <p role="alert" className="px-4 py-6 text-muted-foreground sm:px-7">
        {messages.team.noPermission}
      </p>
    );
  }
  if (context.isError) return <LoadError onRetry={() => context.refetch()} />;
  if (!context.data) return <Loading />;
  if (context.data.level === "NONE") return <EssentialReports />;
  return children(context.data);
}

function Screen({
  title,
  context,
  filter,
  exportAs,
  units,
  children,
}: {
  title: string;
  context: ReportContext;
  units: boolean;
  exportAs?: ExportScreen;
  filter?: (filters: ReportFilters) => ReactNode;
  children: (context: ReportContext, filters: ReportFilters) => ReactNode;
}) {
  const filters = useReportFilters(context);
  const { period } = filters;
  const noData = !units && (!context.firstDataDay || period.to < context.firstDataDay);

  return (
    <div className="flex flex-col">
      <header className="flex flex-wrap items-end gap-3 px-4 pt-6 sm:px-7">
        <div className="mr-auto flex flex-col gap-1">
          <div className="flex flex-wrap items-center gap-2.5">
            <h1 className="text-[28px] leading-[34px] font-bold tracking-tight">{title}</h1>
            {context.level === "BASIC" && (
              <span className="rounded-full bg-info/15 px-2.5 py-0.5 text-xs font-semibold text-info">
                {t.proBadge}
              </span>
            )}
          </div>
          <p className="text-sm text-muted-foreground first-letter:uppercase">{subtitle(period)}</p>
        </div>
        <div className="flex flex-wrap items-center gap-2.5">
          <div
            role="group"
            aria-label={t.period}
            className="flex flex-wrap gap-1 rounded-[14px] border border-border bg-card p-1"
          >
            {PERIOD_KEYS.map((option) => (
              <button
                key={option}
                type="button"
                aria-pressed={option === period.key}
                onClick={() => filters.choosePreset(option)}
                className={cn(
                  "h-10 rounded-[10px] px-3.5 text-sm font-semibold text-muted-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                  option === period.key && "bg-foreground text-background",
                )}
              >
                {t.periods[option]}
              </button>
            ))}
            <DateRangeDialog
              active={period.key === "custom"}
              current={period}
              limits={filters.limits}
              onApply={filters.chooseRange}
            />
          </div>
          {!units && <SectorFilter value={filters.sectorId} onChange={filters.chooseSector} />}
          {filter?.(filters)}
          {exportAs && (
            <ExportButton screen={exportAs} filters={filters} allowed={context.canExport} disabled={noData} />
          )}
        </div>
      </header>
      <div className="flex flex-col gap-5 px-4 py-5 sm:px-7">
        {noData ? <EmptyPeriod firstDataDay={context.firstDataDay} /> : children(context, filters)}
      </div>
    </div>
  );
}

/**
 * "Exportar": um menu com Excel, PDF e CSV, que baixa a tela com o período, o setor e os filtros dela.
 * Fora do Business, o botão fica desligado, com o porquê para o leitor de tela e no "title".
 */
function ExportButton({
  screen,
  filters,
  allowed,
  disabled,
}: {
  screen: ExportScreen;
  filters: ReportFilters;
  allowed: boolean;
  disabled: boolean;
}) {
  const id = useId();
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState<ExportFormat | null>(null);
  const [failed, setFailed] = useState(false);
  const root = useRef<HTMLDivElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  const items = useRef<(HTMLButtonElement | null)[]>([]);

  // Fecha ao tocar fora do menu.
  useEffect(() => {
    if (!open) return;
    function outside(event: PointerEvent) {
      if (!root.current?.contains(event.target as Node)) setOpen(false);
    }
    document.addEventListener("pointerdown", outside);
    return () => document.removeEventListener("pointerdown", outside);
  }, [open]);

  // Ao abrir, o foco vai para a primeira opção.
  useEffect(() => {
    if (open) items.current[0]?.focus();
  }, [open]);

  function close() {
    setOpen(false);
    trigger.current?.focus();
  }

  function onMenuKey(event: KeyboardEvent<HTMLDivElement>) {
    const index = items.current.findIndex((item) => item === document.activeElement);
    if (event.key === "Escape") {
      event.preventDefault();
      close();
    } else if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      const step = event.key === "ArrowDown" ? 1 : -1;
      items.current[(index + step + EXPORT_FORMATS.length) % EXPORT_FORMATS.length]?.focus();
    }
  }

  async function download(format: ExportFormat) {
    setOpen(false);
    setBusy(format);
    setFailed(false);
    try {
      await exportReport(
        screen,
        {
          ...filters.query,
          ...(filters.categoryId ? { categoryId: filters.categoryId } : {}),
          ...(screen === "sales" ? { groupBy: filters.grouping } : {}),
        },
        format,
      );
    } catch {
      setFailed(true);
    } finally {
      setBusy(null);
      trigger.current?.focus();
    }
  }

  return (
    <div ref={root} className="relative flex items-center gap-2.5">
      <Button
        ref={trigger}
        type="button"
        variant="outline"
        className="h-11"
        disabled={!allowed || disabled || busy !== null}
        title={allowed ? undefined : t.exportLocked}
        aria-describedby={allowed ? undefined : "relatorio-exportar-bloqueado"}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={open ? `${id}menu` : undefined}
        onClick={() => setOpen((value) => !value)}
      >
        <Download aria-hidden className="size-4" />
        {busy ? t.exporting : t.export}
        <ChevronDown aria-hidden className="size-4" />
      </Button>
      {!allowed && (
        <span id="relatorio-exportar-bloqueado" className="sr-only">
          {t.exportLocked}
        </span>
      )}
      {open && (
        <div
          id={`${id}menu`}
          role="menu"
          aria-label={t.export}
          onKeyDown={onMenuKey}
          className="absolute top-full right-0 z-20 mt-2 flex w-64 flex-col rounded-2xl border border-border bg-popover p-1.5 shadow-lg"
        >
          {EXPORT_FORMATS.map((format, i) => (
            <button
              key={format}
              ref={(el) => {
                items.current[i] = el;
              }}
              type="button"
              role="menuitem"
              onClick={() => download(format)}
              className="flex min-h-11 flex-col items-start justify-center rounded-xl px-3 py-2 text-left hover:bg-secondary focus-visible:bg-secondary focus-visible:outline-none"
            >
              <span className="text-sm font-semibold">{t.exportFormats[format].label}</span>
              <span className="text-xs text-muted-foreground">{t.exportFormats[format].hint}</span>
            </button>
          ))}
        </div>
      )}
      {failed && (
        <span role="alert" className="text-sm text-destructive">
          {t.exportError}
        </span>
      )}
    </div>
  );
}

/** Com dois setores ou mais: "Todos os setores", "Salão", "Deck"… */
function SectorFilter({ value, onChange }: { value?: string; onChange: (id: string | null) => void }) {
  const sectors = useReportSectors();
  const list = sectors.data ?? [];
  if (list.length < 2) return null;
  return (
    <select
      aria-label={t.sector}
      value={value ?? ""}
      onChange={(e) => onChange(e.target.value || null)}
      className="h-12 rounded-xl border border-input bg-card px-3.5 text-sm text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
    >
      <option value="">{t.allSectors}</option>
      {list.map((sector) => (
        <option key={sector.id} value={sector.id}>
          {sector.name}
        </option>
      ))}
    </select>
  );
}

/** "Últimos 7 dias · 13 a 19 de novembro · comparado com 6 a 12 de novembro". */
function subtitle(period: Period) {
  return [
    ...(period.key === "custom" ? [] : [t.periodTitle[period.key]]),
    formatRange(period, t.range),
    t.comparedWith(formatRange(previousOf(period), t.range)),
  ].join(" · ");
}

/** Um bloco que não carregou: os outros continuam na tela. */
export function BlockError({ onRetry }: { onRetry: () => void }) {
  return (
    <div
      role="alert"
      className="flex flex-wrap items-center gap-3 rounded-[14px] bg-destructive/10 px-4 py-3.5 text-sm"
    >
      <span className="flex-1">{t.blockError}</span>
      <Button variant="outline" className="h-10" onClick={onRetry}>
        {t.retry}
      </Button>
    </div>
  );
}

function LoadError({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex flex-col items-start gap-3 px-4 py-6 sm:px-7">
      <p role="alert" className="text-destructive">
        {t.loadError}
      </p>
      <Button variant="outline" className="h-11" onClick={onRetry}>
        {t.retry}
      </Button>
    </div>
  );
}

function EmptyPeriod({ firstDataDay }: { firstDataDay?: string }) {
  return (
    <section
      aria-labelledby="relatorios-vazio"
      className="flex flex-col items-center gap-3.5 rounded-[20px] border border-border bg-card px-6 py-14 text-center"
    >
      <span className="flex size-[72px] items-center justify-center rounded-[22px] bg-secondary text-muted-foreground">
        <ChartColumn aria-hidden className="size-10" />
      </span>
      <h2 id="relatorios-vazio" className="text-[22px] leading-7 font-bold">
        {t.empty.title}
      </h2>
      <p className="max-w-xl text-muted-foreground">
        {t.empty.text}
        {firstDataDay && ` ${t.empty.since(formatDay(firstDataDay))}`}
      </p>
      <div className="mt-1.5 flex flex-wrap justify-center gap-2.5">
        <Link href="/painel" className={cn(buttonVariants({ variant: "outline" }), "h-11")}>
          {t.empty.live}
        </Link>
        <Link href="/admin/mesas" className={cn(buttonVariants({ variant: "outline" }), "h-11")}>
          {t.empty.tables}
        </Link>
      </div>
    </section>
  );
}

/** O esqueleto enquanto os números chegam (desenho 14.12): o brilho para se o aparelho pede menos movimento. */
export function Loading({ inline = false }: { inline?: boolean }) {
  return (
    <div aria-busy="true" className={cn("flex flex-col gap-5", !inline && "px-4 py-6 sm:px-7")}>
      <p role="status" className="sr-only">
        {t.loading}
      </p>
      {!inline && (
        <div aria-hidden className="flex flex-col gap-2">
          <span className="h-8 w-48 rounded-lg bg-secondary motion-safe:animate-pulse" />
          <span className="h-4 w-80 max-w-full rounded-md bg-secondary motion-safe:animate-pulse" />
        </div>
      )}
      <div aria-hidden className="grid grid-cols-2 gap-3.5 lg:grid-cols-3">
        {Array.from({ length: 3 }, (_, i) => (
          <span key={i} className="h-28 rounded-[20px] bg-secondary motion-safe:animate-pulse" />
        ))}
      </div>
      <span aria-hidden className="h-64 rounded-[20px] bg-secondary motion-safe:animate-pulse" />
    </div>
  );
}

/** O Essencial não tem relatórios: a tela mostra o que o Pro e o Business trazem (desenho 14.10). */
function EssentialReports() {
  const e = t.essential;
  return (
    <div className="flex flex-col gap-5 px-4 py-6 sm:px-7">
      <header className="flex flex-col gap-1">
        <h1 className="text-[28px] leading-[34px] font-bold tracking-tight">{messages.admin.nav.reports}</h1>
        <p className="text-sm text-muted-foreground">{e.subtitle}</p>
      </header>
      <section
        aria-labelledby="relatorios-essencial"
        className="flex flex-col gap-3 rounded-[20px] border border-border bg-card p-6 sm:p-8"
      >
        <h2 id="relatorios-essencial" className="max-w-2xl text-2xl leading-8 font-bold tracking-tight sm:text-[28px]">
          {e.title}
        </h2>
        <p className="max-w-2xl text-muted-foreground">{e.text}</p>
        <div className="mt-2 flex flex-wrap gap-2.5">
          <Link href="/admin/assinatura" className={cn(buttonVariants(), "h-11")}>
            {e.upgrade}
          </Link>
          <Link href="/admin/assinatura" className={cn(buttonVariants({ variant: "outline" }), "h-11")}>
            {e.compare}
          </Link>
        </div>
      </section>
      <div className="grid gap-5 lg:grid-cols-2">
        <PlanList title={e.pro} items={e.proItems} />
        <PlanList title={e.business} items={e.businessItems} />
      </div>
      <p className="text-xs text-muted-foreground">{e.trial}</p>
    </div>
  );
}

function PlanList({ title, items }: { title: string; items: readonly string[] }) {
  return (
    <section aria-label={title} className="flex flex-col gap-3.5 rounded-[20px] border border-border bg-card p-5">
      <h2 className="font-semibold">{title}</h2>
      <ul className="flex flex-col gap-2.5">
        {items.map((item) => (
          <li key={item} className="flex gap-2.5 text-sm">
            <Check aria-hidden className="mt-0.5 size-[18px] shrink-0 text-success" />
            {item}
          </li>
        ))}
      </ul>
    </section>
  );
}
