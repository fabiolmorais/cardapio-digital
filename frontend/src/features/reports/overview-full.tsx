import { CircleAlert, Clock3, TriangleAlert } from "lucide-react";
import Link from "next/link";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { ChartCard, KpiTile, NoData, variation, type Delta } from "./charts";
import { formatDuration, timeVariation } from "./durations";
import type { OverviewReport } from "./reports-api";

const t = messages.reports;
const f = t.full;

type Full = NonNullable<OverviewReport["full"]>;
type Operation = Full["current"];

const share = new Intl.NumberFormat("pt-BR", { style: "percent", maximumFractionDigits: 1 });

/** Um tempo em que tanto faz subir ou descer (a permanência): a variação sem cor. */
function neutralTime(current: number, previous: number): Delta {
  const diff = current - previous;
  if (Math.abs(diff) < 1) return { text: t.delta.same, tone: "flat" };
  const value = formatDuration(Math.abs(diff));
  return { text: diff > 0 ? f.timeUp(value) : f.timeDown(value), tone: "flat" };
}

/** Os 5 números do Business (REL-32, 63, 38, 56, 12), ao lado dos 3 básicos. */
export function OperationTiles({ full, revenueCents }: { full: Full; revenueCents: number }) {
  const now: Operation = full.current;
  const before: Operation = full.compared;
  const time = (seconds: number | null | undefined) => (seconds == null ? f.noValue : formatDuration(seconds));
  const sharePoints = () => {
    if (now.phoneSharePercent == null || before.phoneSharePercent == null) return null;
    const diff = Math.round(now.phoneSharePercent - before.phoneSharePercent);
    if (diff === 0) return { text: t.delta.same, tone: "flat" as const };
    return diff > 0
      ? { text: f.pointsUp(f.points(diff)), tone: "up" as const }
      : { text: f.pointsDown(f.points(-diff)), tone: "down" as const };
  };
  return (
    <>
      <KpiTile
        label={f.toTable}
        value={time(now.toTableSeconds)}
        delta={
          now.toTableSeconds != null && before.toTableSeconds != null
            ? timeVariation(now.toTableSeconds, before.toTableSeconds)
            : null
        }
        hint={now.toTableSeconds == null ? f.noDeliveries : t.delta.none}
      />
      <KpiTile
        label={f.phoneOrders}
        value={now.phoneSharePercent == null ? f.noValue : `${Math.round(now.phoneSharePercent)}%`}
        delta={sharePoints()}
        hint={now.phoneSharePercent == null ? f.noOrders : t.delta.none}
      />
      <KpiTile
        label={f.callResponse}
        value={time(now.callResponseSeconds)}
        delta={
          now.callResponseSeconds != null && before.callResponseSeconds != null
            ? timeVariation(now.callResponseSeconds, before.callResponseSeconds)
            : null
        }
        hint={now.callResponseSeconds == null ? f.noCalls : t.delta.none}
      />
      <KpiTile
        label={f.stay}
        value={time(now.staySeconds)}
        delta={
          now.staySeconds != null && before.staySeconds != null
            ? neutralTime(now.staySeconds, before.staySeconds)
            : null
        }
        hint={now.staySeconds == null ? f.noClosed : t.delta.none}
      />
      <KpiTile
        label={f.canceled}
        value={formatCents(now.canceledCents)}
        hint={revenueCents > 0 ? f.ofRevenue(share.format(now.canceledCents / revenueCents)) : undefined}
      />
    </>
  );
}

/** "Hoje, até agora" (REL-10): hoje contra os mesmos dias da semana das últimas semanas. */
export function TodayCard({ full, timeZone }: { full: Full; timeZone: string }) {
  const now = new Date();
  const weekday = new Intl.DateTimeFormat("pt-BR", { timeZone, weekday: "long" }).format(now).replace("-feira", "");
  const time = new Intl.DateTimeFormat("pt-BR", { timeZone, hour: "2-digit", minute: "2-digit" })
    .format(now)
    .replace(":", "h");
  const isoWeekday = ["segunda", "terça", "quarta", "quinta", "sexta", "sábado", "domingo"].indexOf(weekday) + 1 || 1;
  const today = full.today;
  return (
    <ChartCard title={f.today.title} hint={f.today.at(weekday, time)}>
      {!today ? (
        <NoData text={f.today.empty} height={120} />
      ) : (
        <div className="flex flex-col gap-3">
          <p>
            <span className="text-[28px] leading-9 font-bold tabular-nums">{formatCents(today.soFarCents)}</span>
            <span className="text-sm text-muted-foreground">{f.today.of(formatCents(today.expectedCents))}</span>
          </p>
          <span
            role="img"
            aria-label={f.today.chart(formatCents(today.soFarCents), formatCents(today.expectedCents))}
            className="flex h-3 overflow-hidden rounded-full bg-secondary"
          >
            <span
              className="h-full rounded-full bg-report-now"
              style={{
                width: `${today.expectedCents > 0 ? Math.min(100, (today.soFarCents / today.expectedCents) * 100) : 0}%`,
              }}
            />
          </span>
          <p className="text-[13px] leading-[18px] text-muted-foreground">
            {f.today.note(today.weeks, isoWeekday, formatCents(today.usualCents))}
            {todayVersusUsual(today.soFarCents, today.usualCents)}
          </p>
        </div>
      )}
    </ChartCard>
  );
}

function todayVersusUsual(soFar: number, usual: number) {
  if (usual === 0) return ".";
  if (soFar === 0) return f.today.nothingYet;
  const delta = variation(soFar, usual);
  const percent = delta.text.replace(/^[▲▼] /, "");
  if (delta.tone === "up") return f.today.above(percent);
  if (delta.tone === "down") return f.today.below(percent);
  return f.today.same;
}

/** Os garçons que mais atenderam chamados (REL-46, 48). */
export function WaitersCard({ full }: { full: Full }) {
  return (
    <ChartCard title={f.waiters.title} hint={f.waiters.hint}>
      {full.waiters.length === 0 ? (
        <NoData text={f.waiters.empty} height={120} />
      ) : (
        <table className="w-full border-collapse text-sm">
          <thead>
            <tr className="text-left text-xs font-semibold tracking-[.4px] text-muted-foreground uppercase">
              <th scope="col" className="border-b border-border py-3 pr-3 font-semibold">
                {f.waiters.columns.name}
              </th>
              <th scope="col" className="border-b border-border px-3 py-3 text-right font-semibold">
                {f.waiters.columns.calls}
              </th>
              <th scope="col" className="border-b border-border py-3 pl-3 text-right font-semibold">
                {f.waiters.columns.response}
              </th>
            </tr>
          </thead>
          <tbody>
            {full.waiters.map((waiter, index) => (
              <tr key={waiter.userId} className="border-b border-border last:border-b-0">
                <td className="py-3 pr-3">
                  <span className="mr-2 inline-flex size-6 items-center justify-center rounded-full bg-secondary text-xs font-semibold">
                    {index + 1}
                  </span>
                  {waiter.name}
                </td>
                <td className="px-3 py-3 text-right font-mono tabular-nums">{waiter.calls}</td>
                <td className="py-3 pl-3 text-right font-mono whitespace-nowrap tabular-nums">
                  {formatDuration(waiter.averageResponseSeconds)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </ChartCard>
  );
}

/** Os pontos de atenção (REL-33, 61, 23): o que vale olhar, sem precisar procurar. */
export function AttentionCard({ full }: { full: Full }) {
  const a = f.attention;
  return (
    <ChartCard title={a.title}>
      {full.attention.length === 0 ? (
        <NoData text={a.empty} height={120} />
      ) : (
        <ul className="flex flex-col gap-3">
          {full.attention.map((point) => {
            if (point.kind === "SLOW_ITEM") {
              return (
                <li key="slow" className="flex gap-2.5 text-sm leading-5">
                  <Clock3 aria-hidden className="mt-0.5 size-[18px] shrink-0 text-warning" />
                  <span>
                    <b>{point.name}</b>
                    {a.slowText(
                      formatDuration(point.dayAverageSeconds ?? 0),
                      formatDuration(point.usualAverageSeconds ?? 0),
                    )}
                  </span>
                </li>
              );
            }
            if (point.kind === "FORGOTTEN_TABS") {
              const tables = point.tables ?? [];
              return (
                <li key="forgotten" className="flex gap-2.5 text-sm leading-5">
                  <TriangleAlert aria-hidden className="mt-0.5 size-[18px] shrink-0 text-destructive" />
                  <span>
                    <b>{a.forgotten(tables.length)}</b>
                    {a.forgottenText(a.tables(tables.slice(0, 3), Math.max(0, tables.length - 3)))}
                  </span>
                </li>
              );
            }
            return (
              <li key="canceled" className="flex gap-2.5 text-sm leading-5">
                <CircleAlert aria-hidden className="mt-0.5 size-[18px] shrink-0 text-info" />
                <span>
                  <b>{point.name}</b>
                  {a.canceledText(point.times ?? 0, point.reason ?? null, point.reasonTimes ?? 0)}
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </ChartCard>
  );
}

/** "2026-11" → "novembro". */
function monthName(month: string) {
  return f.goal.months[Number(month.slice(5, 7)) - 1];
}

/**
 * A meta do mês (REL-79, Business): quanto já foi, a barra até a meta e quanto por dia falta. É sempre o
 * mês de hoje, seja qual for o período escolhido.
 */
export function GoalCard({ full, today }: { full: Full; today: string }) {
  const goal = full.goal;
  if (!goal) {
    const month = monthName(today);
    return (
      <ChartCard title={f.goal.title(month)}>
        <div className="flex flex-col items-start gap-2 text-sm">
          <p className="text-muted-foreground">{f.goal.none(month)}</p>
          <Link
            href="/admin/relatorios/resumo"
            className="font-semibold underline underline-offset-4 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
          >
            {f.goal.set}
          </Link>
        </div>
      </ChartCard>
    );
  }
  const ratio = goal.achievedCents / goal.goalCents;
  const percent = new Intl.NumberFormat("pt-BR", { style: "percent", maximumFractionDigits: 0 }).format(ratio);
  return (
    <ChartCard title={f.goal.title(monthName(goal.month))} hint={percent}>
      <div className="flex flex-col gap-3">
        <p>
          <span className="text-[28px] leading-9 font-bold tabular-nums">{formatCents(goal.achievedCents)}</span>
          <span className="text-sm text-muted-foreground">{f.goal.of(formatCents(goal.goalCents))}</span>
        </p>
        <span
          role="img"
          aria-label={f.goal.chart(formatCents(goal.achievedCents), formatCents(goal.goalCents), percent)}
          className="flex h-3 overflow-hidden rounded-full bg-secondary"
        >
          <span className="h-full rounded-full bg-report-now" style={{ width: `${Math.min(100, ratio * 100)}%` }} />
        </span>
        <p className="text-[13px] leading-[18px] text-muted-foreground">
          {goal.perDayNeededCents == null
            ? f.goal.reached
            : f.goal.perDay(goal.daysLeft, formatCents(goal.perDayNeededCents))}
          {goal.dailyAverageCents != null && f.goal.average(formatCents(goal.dailyAverageCents))}
        </p>
      </div>
    </ChartCard>
  );
}
