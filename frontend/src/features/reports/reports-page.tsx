"use client";

import { ChartColumn, Check } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Button, buttonVariants } from "@/components/ui/button";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import {
  formatDay,
  formatRange,
  parsePeriodKey,
  PERIOD_KEYS,
  previousOf,
  resolvePeriod,
  type Period,
  type PeriodKey,
} from "./period";
import { canSeeReports, useReportContext, type ReportContext } from "./reports-api";

const t = messages.reports;

/**
 * Os relatórios na gestão (ADR 0013): a Visão geral (desenho 14.1), com o período no endereço
 * (?periodo=30d) para o dono poder voltar ou mandar o link. O Essencial vê o que o Pro traz (14.10);
 * sem comandas fechadas no período, o aviso de sem dados (14.11).
 */
export function ReportsPage() {
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
  if (context.isError) {
    return (
      <div className="flex flex-col items-start gap-3 px-4 py-6 sm:px-7">
        <p role="alert" className="text-destructive">
          {t.loadError}
        </p>
        <Button variant="outline" className="h-11" onClick={() => context.refetch()}>
          {t.retry}
        </Button>
      </div>
    );
  }
  if (!context.data) return <Loading />;
  if (context.data.level === "NONE") return <EssentialReports />;
  return <Overview context={context.data} />;
}

function Overview({ context }: { context: ReportContext }) {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const key = parsePeriodKey(params.get("periodo"));
  const period = resolvePeriod(key, context.today, context.earliestDay);
  const noData = !context.firstDataDay || period.to < context.firstDataDay;

  function choose(next: PeriodKey) {
    router.replace(`${pathname}?periodo=${next}`, { scroll: false });
  }

  return (
    <div className="flex flex-col">
      <header className="flex flex-wrap items-end gap-3 px-4 pt-6 sm:px-7">
        <div className="mr-auto flex flex-col gap-1">
          <div className="flex flex-wrap items-center gap-2.5">
            <h1 className="text-[28px] leading-[34px] font-bold tracking-tight">{t.overview}</h1>
            {context.level === "BASIC" && (
              <span className="rounded-full bg-info/15 px-2.5 py-0.5 text-xs font-semibold text-info">
                {t.proBadge}
              </span>
            )}
          </div>
          <p className="text-sm text-muted-foreground first-letter:uppercase">{subtitle(period)}</p>
        </div>
        <div
          role="group"
          aria-label={t.period}
          className="flex flex-wrap gap-1 rounded-[14px] border border-border bg-card p-1"
        >
          {PERIOD_KEYS.map((option) => (
            <button
              key={option}
              type="button"
              aria-pressed={option === key}
              onClick={() => choose(option)}
              className={cn(
                "h-10 rounded-[10px] px-3.5 text-sm font-semibold text-muted-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                option === key && "bg-foreground text-background",
              )}
            >
              {t.periods[option]}
            </button>
          ))}
        </div>
      </header>
      <div className="flex flex-col gap-5 px-4 py-5 sm:px-7">
        {noData ? (
          <EmptyPeriod firstDataDay={context.firstDataDay} />
        ) : (
          <p className="text-sm text-muted-foreground">{t.soon}</p>
        )}
      </div>
    </div>
  );
}

/** "Últimos 7 dias · 13 a 19 de novembro · comparado com 6 a 12 de novembro". */
function subtitle(period: Period) {
  return [
    t.periodTitle[period.key],
    formatRange(period, t.range),
    t.comparedWith(formatRange(previousOf(period), t.range)),
  ].join(" · ");
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

function Loading() {
  return (
    <div aria-busy="true" className="flex flex-col gap-5 px-4 py-6 sm:px-7">
      <p role="status" className="sr-only">
        {t.loading}
      </p>
      <div aria-hidden className="flex flex-col gap-2">
        <span className="h-8 w-48 rounded-lg bg-secondary motion-safe:animate-pulse" />
        <span className="h-4 w-80 max-w-full rounded-md bg-secondary motion-safe:animate-pulse" />
      </div>
      <div aria-hidden className="grid grid-cols-2 gap-3.5 lg:grid-cols-4">
        {Array.from({ length: 4 }, (_, i) => (
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
