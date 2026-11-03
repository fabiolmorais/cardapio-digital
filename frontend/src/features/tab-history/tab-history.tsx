"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { ChevronRight } from "lucide-react";
import Link from "next/link";
import { useId, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { fetchTables } from "@/features/floor/floor-api";
import { canManageTeam } from "@/features/team/team-rules";
import { useSession } from "@/lib/auth/use-session";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { fetchClosedTabs, PAGE_SIZE } from "./tab-history-api";

const t = messages.tabHistory;
const pay = messages.panel.calls.payment;
type Preset = keyof typeof t.presets;
const PRESETS: Preset[] = ["today", "yesterday", "week", "month", "custom"];

/** "2026-11-03" de uma data, no calendário deste aparelho (que está no restaurante). */
export function isoDay(date: Date) {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function daysAgo(days: number) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return isoDay(date);
}

/** O período de cada atalho: hoje, ontem, os últimos 7 e os últimos 30 dias (hoje incluído). */
function periodOf(preset: Exclude<Preset, "custom">) {
  return {
    today: { from: daysAgo(0), to: daysAgo(0) },
    yesterday: { from: daysAgo(1), to: daysAgo(1) },
    week: { from: daysAgo(6), to: daysAgo(0) },
    month: { from: daysAgo(29), to: daysAgo(0) },
  }[preset];
}

export function dateTime(iso: string) {
  return new Date(iso).toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });
}

/** O histórico de comandas (gestão): o período, o filtro por mesa e a lista paginada. */
export function TabHistory() {
  const { status, session } = useSession();
  if (status !== "authenticated") return <p className="px-4 py-6 text-muted-foreground sm:px-7">{t.loading}</p>;
  if (!canManageTeam(session.establishment.role)) {
    return (
      <p role="alert" className="px-4 py-6 text-muted-foreground sm:px-7">
        {messages.team.noPermission}
      </p>
    );
  }
  return <History />;
}

function History() {
  const id = useId();
  const [preset, setPreset] = useState<Preset>("today");
  const [custom, setCustom] = useState(() => periodOf("week"));
  const [tableId, setTableId] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const period = preset === "custom" ? custom : periodOf(preset);
  const valid = Boolean(period.from && period.to && period.from <= period.to);

  const tables = useQuery({ queryKey: ["tables"], queryFn: fetchTables });
  const history = useQuery({
    queryKey: ["tab-history", period.from, period.to, tableId, page],
    queryFn: () => fetchClosedTabs({ ...period, tableId, page }),
    enabled: valid,
    placeholderData: keepPreviousData,
  });
  const data = history.data;

  const chip = (on: boolean) =>
    cn(
      "h-10 rounded-full border border-border px-4 text-sm font-semibold focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
      on && "border-transparent bg-foreground text-background",
    );

  return (
    <div className="flex flex-col gap-6 px-4 py-6 sm:px-7">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">{t.title}</h1>
        <p className="text-sm text-muted-foreground">{t.subtitle}</p>
      </header>

      <section className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-5">
        <fieldset className="flex flex-col gap-2">
          <legend className="pb-2 text-sm font-semibold text-muted-foreground">{t.period}</legend>
          <div className="flex flex-wrap gap-2">
            {PRESETS.map((p) => (
              <button
                key={p}
                type="button"
                aria-pressed={preset === p}
                onClick={() => {
                  setPreset(p);
                  setPage(0);
                }}
                className={chip(preset === p)}
              >
                {t.presets[p]}
              </button>
            ))}
          </div>
        </fieldset>
        <div className="flex flex-wrap gap-4">
          {preset === "custom" && (
            <>
              <div className="flex flex-col gap-2">
                <Label htmlFor={`${id}from`}>{t.from}</Label>
                <Input
                  id={`${id}from`}
                  type="date"
                  value={custom.from}
                  max={custom.to || undefined}
                  onChange={(event) => {
                    setCustom({ ...custom, from: event.target.value });
                    setPage(0);
                  }}
                  className="h-11 rounded-xl"
                />
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor={`${id}to`}>{t.to}</Label>
                <Input
                  id={`${id}to`}
                  type="date"
                  value={custom.to}
                  min={custom.from || undefined}
                  onChange={(event) => {
                    setCustom({ ...custom, to: event.target.value });
                    setPage(0);
                  }}
                  className="h-11 rounded-xl"
                />
              </div>
            </>
          )}
          <div className="flex flex-col gap-2">
            <Label htmlFor={`${id}table`}>{t.table}</Label>
            <select
              id={`${id}table`}
              value={tableId ?? ""}
              onChange={(event) => {
                setTableId(event.target.value || null);
                setPage(0);
              }}
              className="h-11 min-w-48 rounded-xl border border-input bg-transparent px-3 text-base focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
            >
              <option value="">{t.allTables}</option>
              {(tables.data ?? []).map((table) => (
                <option key={table.id} value={table.id}>
                  {t.tableOption(table.label)}
                </option>
              ))}
            </select>
          </div>
        </div>
      </section>

      {history.isError && (
        <p role="alert" className="text-sm text-destructive">
          {history.error.message || t.loadError}
        </p>
      )}

      {!data ? (
        valid && <p className="text-muted-foreground">{t.loading}</p>
      ) : (
        <section aria-live="polite" className="flex flex-col gap-3">
          <p className="text-sm font-semibold">{t.count(data.totalElements)}</p>
          {data.content.length === 0 ? (
            <p className="text-muted-foreground">{t.empty}</p>
          ) : (
            <ul className="flex flex-col divide-y divide-border rounded-2xl border border-border bg-card">
              {data.content.map((tab) => (
                <li key={tab.id}>
                  <Link
                    href={`/admin/comandas/${tab.id}`}
                    aria-label={t.open(tab.tableLabel, dateTime(tab.closedAt))}
                    className="grid grid-cols-[1fr_auto_auto] items-center gap-x-4 gap-y-1 px-4 py-3.5 hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none focus-visible:ring-inset sm:grid-cols-[1.4fr_1fr_1fr_1fr_auto]"
                  >
                    <span className="flex min-w-0 flex-col">
                      <span className="font-semibold">{t.tableLabel(tab.tableLabel, tab.sectorName)}</span>
                      <span className="text-sm text-muted-foreground sm:hidden">
                        {dateTime(tab.closedAt)} · {pay[tab.paymentMethod]}
                      </span>
                    </span>
                    <span className="hidden text-sm text-muted-foreground sm:block">{dateTime(tab.closedAt)}</span>
                    <span className="hidden text-sm sm:block">{pay[tab.paymentMethod]}</span>
                    <span className="font-mono font-semibold tabular-nums">{formatCents(tab.totalCents)}</span>
                    <ChevronRight aria-hidden className="size-5 text-muted-foreground" />
                  </Link>
                </li>
              ))}
            </ul>
          )}
          {data.totalPages > 1 && (
            <nav className="flex items-center justify-between gap-3">
              <Button variant="outline" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                {t.previous}
              </Button>
              <span className="text-sm text-muted-foreground">{t.pageOf(page + 1, data.totalPages)}</span>
              <Button
                variant="outline"
                disabled={(page + 1) * PAGE_SIZE >= data.totalElements}
                onClick={() => setPage((p) => p + 1)}
              >
                {t.next}
              </Button>
            </nav>
          )}
        </section>
      )}
    </div>
  );
}
