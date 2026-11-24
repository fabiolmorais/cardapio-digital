"use client";

import Link from "next/link";
import { useId, useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { formatCents } from "@/lib/format";
import { LockedCard } from "./locked";
import { messages } from "@/messages/pt-BR";
import { formatRange } from "./period";
import {
  useReportAlerts,
  useSaveReportAlerts,
  useSaveSummarySettings,
  useSendSummaryTest,
  useSummaryPreview,
  useSummarySettings,
  type ReportAlerts,
  type SummarySettings,
} from "./reports-api";
import { Loading, ReportsGate } from "./reports-page";

const t = messages.reports.summary;
const a = t.alerts;

const HOURS = Array.from({ length: 24 }, (_, hour) => ({ value: String(hour), label: t.hourOption(hour) }));

/**
 * O resumo diário e os alertas (desenhos 14.8 e 14.15). O resumo é do Pro em diante; os alertas, do
 * Business (no Pro, aparecem bloqueados).
 */
export function SummaryScreen() {
  return <ReportsGate>{(context) => <SummaryLoader full={context.level === "FULL"} />}</ReportsGate>;
}

function SummaryLoader({ full }: { full: boolean }) {
  const settings = useSummarySettings();
  const alerts = useReportAlerts(full);
  if (settings.isError || alerts.isError) {
    return (
      <div className="flex flex-col items-start gap-3 px-4 py-6 sm:px-7">
        <p role="alert" className="text-destructive">
          {settings.isError ? t.loadError : a.loadError}
        </p>
        <Button
          variant="outline"
          className="h-11"
          onClick={() => (settings.isError ? settings.refetch() : alerts.refetch())}
        >
          {messages.reports.retry}
        </Button>
      </div>
    );
  }
  if (!settings.data || (full && !alerts.data)) return <Loading />;
  return <SummaryForm initial={settings.data} initialAlerts={full ? (alerts.data ?? null) : null} />;
}

function SummaryForm({ initial, initialAlerts }: { initial: SummarySettings; initialAlerts: ReportAlerts | null }) {
  const id = useId();
  const [email, setEmail] = useState(initial.emailEnabled);
  const [push, setPush] = useState(initial.pushEnabled);
  const [hour, setHour] = useState(initial.sendHour);
  const [receivers, setReceivers] = useState(
    () => new Set(initial.recipients.filter((r) => r.receives).map((r) => r.userId)),
  );
  const [alerts, setAlerts] = useState<ReportAlerts | null>(initialAlerts);
  const [notice, setNotice] = useState<string | null>(null);
  const save = useSaveSummarySettings();
  const saveAlerts = useSaveReportAlerts();
  const saving = save.isPending || saveAlerts.isPending;

  function changed() {
    setNotice(null);
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await save.mutateAsync({ emailEnabled: email, pushEnabled: push, sendHour: hour, receiverIds: [...receivers] });
      if (alerts) await saveAlerts.mutateAsync(alerts);
      setNotice(alerts ? t.savedWithAlerts : t.saved);
    } catch {
      // O erro aparece embaixo do título (save.isError ou saveAlerts.isError).
    }
  }

  return (
    <form onSubmit={onSubmit} className="flex flex-col gap-6 px-4 py-6 sm:px-7">
      <header className="flex flex-wrap items-end gap-4">
        <div className="mr-auto flex flex-col gap-1">
          <h1 className="text-[28px] leading-[34px] font-bold tracking-tight">{t.title}</h1>
          <p className="text-sm text-muted-foreground">{t.subtitle}</p>
        </div>
        <Button type="submit" size="xl" disabled={saving}>
          {saving ? t.saving : t.save}
        </Button>
      </header>

      {notice && (
        <p role="status" className="text-sm text-success">
          {notice}
        </p>
      )}
      {(save.isError || saveAlerts.isError) && (
        <p role="alert" className="text-sm text-destructive">
          {t.saveError}
        </p>
      )}

      <div className="grid items-start gap-5 xl:grid-cols-[minmax(0,1fr)_380px]">
        <section
          aria-labelledby={`${id}day`}
          className="flex min-w-0 flex-col gap-5 rounded-[20px] border border-border bg-card p-5 sm:p-6"
        >
          <h2 id={`${id}day`} className="text-lg font-semibold">
            {t.day}
          </h2>
          <Toggle
            checked={email}
            onChange={(value) => {
              changed();
              setEmail(value);
            }}
            label={t.email}
            description={t.emailText}
          />
          <Toggle
            checked={push}
            onChange={(value) => {
              changed();
              setPush(value);
            }}
            label={t.push}
            description={t.pushText}
          />
          <div className="flex max-w-md flex-col gap-2">
            <Label htmlFor={`${id}hour`}>{t.hour}</Label>
            <Select
              id={`${id}hour`}
              value={String(hour)}
              onValueChange={(value) => {
                changed();
                setHour(Number(value));
              }}
              options={HOURS}
              className="h-11"
            />
            <p className="text-sm text-muted-foreground">{t.hourHint}</p>
          </div>
          <fieldset className="flex flex-col gap-1">
            <legend className="mb-1.5 text-sm font-semibold text-muted-foreground">{t.recipients}</legend>
            {initial.recipients.map((recipient) => (
              <label key={recipient.userId} className="flex min-h-12 items-center gap-3">
                <input
                  type="checkbox"
                  checked={receivers.has(recipient.userId)}
                  onChange={(event) => {
                    changed();
                    const checked = event.target.checked;
                    setReceivers((current) => {
                      const next = new Set(current);
                      if (checked) next.add(recipient.userId);
                      else next.delete(recipient.userId);
                      return next;
                    });
                  }}
                  className="size-5 shrink-0 accent-primary"
                />
                <span className="flex flex-1 flex-col">
                  <span>{recipient.name}</span>
                  <span className="text-sm text-muted-foreground">
                    {t.role[recipient.role]} · {t.channels(recipient.hasEmail)}
                  </span>
                </span>
              </label>
            ))}
            <p className="text-sm text-muted-foreground">
              {t.recipientsHint}{" "}
              <Link href="/painel" className="font-semibold text-foreground underline underline-offset-4">
                {t.live}
              </Link>
              .
            </p>
          </fieldset>
        </section>
        <Preview />
        {alerts ? (
          <Alerts
            value={alerts}
            onChange={(next) => {
              changed();
              setAlerts(next);
            }}
          />
        ) : (
          <div className="xl:col-start-1">
            <LockedCard title={a.title} text={a.locked} />
          </div>
        )}
      </div>
    </form>
  );
}

/** O que o e-mail traria agora, e o "Enviar um teste para mim". */
function Preview() {
  const id = useId();
  const preview = useSummaryPreview();
  const test = useSendSummaryTest();
  const data = preview.data;
  return (
    <aside aria-labelledby={`${id}preview`} className="flex flex-col gap-3">
      <h2 id={`${id}preview`} className="text-sm font-semibold text-muted-foreground">
        {t.preview}
      </h2>
      {/* O e-mail é claro em qualquer tema: a prévia também. */}
      <div className="flex flex-col gap-3 rounded-[18px] bg-[#F8F5F1] p-5 text-sm text-[#1A1714]">
        {!data ? (
          <span aria-hidden className="h-40 rounded-xl bg-black/5 motion-safe:animate-pulse" />
        ) : (
          <>
            <span className="text-xs text-[#5C544C]">{t.previewFrom(data.establishmentName)}</span>
            <b className="text-lg leading-6">
              {t.previewDay(
                formatRange({ from: data.day, to: data.day }, (a, b) => `${a} a ${b}`).replace("-feira", ""),
              )}
            </b>
            {data.current.tabs === 0 ? (
              <p>{t.previewEmpty}</p>
            ) : (
              <>
                <dl className="grid grid-cols-2 gap-2.5">
                  <div className="flex flex-col">
                    <dt className="text-xs text-[#5C544C]">{t.revenue}</dt>
                    <dd className="font-bold tabular-nums">{formatCents(data.current.revenueCents)}</dd>
                  </div>
                  <div className="flex flex-col">
                    <dt className="text-xs text-[#5C544C]">{t.tabs}</dt>
                    <dd className="font-bold tabular-nums">{data.current.tabs}</dd>
                  </div>
                </dl>
                {data.champion && (
                  <p>
                    <b>{t.champion}:</b> {t.championText(data.champion.name, data.champion.quantity)}
                  </p>
                )}
              </>
            )}
          </>
        )}
      </div>
      <p className="text-xs text-muted-foreground">{t.previewNote}</p>
      <Button type="button" variant="outline" className="h-11" disabled={test.isPending} onClick={() => test.mutate()}>
        {test.isPending ? t.testing : t.test}
      </Button>
      {test.isSuccess && (
        <p role="status" className="text-sm text-success">
          {t.testResult(test.data.email, test.data.pushDevices)}
        </p>
      )}
      {test.isError && (
        <p role="alert" className="text-sm text-destructive">
          {t.testError}
        </p>
      )}
    </aside>
  );
}

type Limit = { enabled: keyof ReportAlerts; value: keyof ReportAlerts; min: number; max: number; unit: string };

const LIMITS: { title: string; text: string; limit: Limit }[] = [
  {
    title: a.kitchen,
    text: a.kitchenText,
    limit: { enabled: "kitchenEnabled", value: "kitchenMinutes", min: 5, max: 120, unit: a.minutes },
  },
  {
    title: a.call,
    text: a.callText,
    limit: { enabled: "callEnabled", value: "callMinutes", min: 1, max: 30, unit: a.minutes },
  },
  {
    title: a.tab,
    text: a.tabText,
    limit: { enabled: "tabEnabled", value: "tabHours", min: 1, max: 24, unit: a.hours },
  },
];

/** Os alertas em tempo real (REL-78): cada um liga e desliga, com o limite ao lado. */
function Alerts({ value, onChange }: { value: ReportAlerts; onChange: (next: ReportAlerts) => void }) {
  const id = useId();
  return (
    <section
      aria-labelledby={`${id}alerts`}
      className="flex min-w-0 flex-col gap-4 rounded-[20px] border border-border bg-card p-5 sm:p-6 xl:col-start-1"
    >
      <div className="flex flex-col gap-1">
        <h2 id={`${id}alerts`} className="text-lg font-semibold">
          {a.title}
        </h2>
        <p className="text-sm text-muted-foreground">{a.text}</p>
      </div>
      {LIMITS.map(({ title, text, limit }) => {
        const enabled = value[limit.enabled] as boolean;
        const amount = value[limit.value] as number;
        return (
          <div key={title} className="flex flex-wrap items-center gap-x-4 gap-y-2">
            <div className="flex min-w-0 flex-1 flex-col gap-0.5">
              <span className="font-medium">{title}</span>
              <span className="text-sm text-muted-foreground">
                {text} {amount} {limit.unit}
              </span>
            </div>
            <LimitField
              key={title}
              label={a.limitLabel(title, limit.unit)}
              value={amount}
              min={limit.min}
              max={limit.max}
              unit={limit.unit}
              disabled={!enabled}
              onChange={(next) => onChange({ ...value, [limit.value]: next })}
            />
            <input
              type="checkbox"
              role="switch"
              aria-label={title}
              checked={enabled}
              onChange={(event) => onChange({ ...value, [limit.enabled]: event.target.checked })}
              className="size-5 shrink-0 accent-primary"
            />
          </div>
        );
      })}
    </section>
  );
}

/**
 * O limite de um alerta. Enquanto a pessoa digita, o texto fica como está; ao sair do campo, o valor
 * volta para a faixa permitida (apagar e digitar "30" não vira 120 no meio do caminho).
 */
function LimitField({
  label,
  value,
  min,
  max,
  unit,
  disabled,
  onChange,
}: {
  label: string;
  value: number;
  min: number;
  max: number;
  unit: string;
  disabled: boolean;
  onChange: (next: number) => void;
}) {
  const [draft, setDraft] = useState(String(value));
  function commit() {
    const typed = Math.round(Number(draft));
    const next = Number.isNaN(typed) || draft.trim() === "" ? value : Math.min(max, Math.max(min, typed));
    setDraft(String(next));
    if (next !== value) onChange(next);
  }
  return (
    <label className="flex items-center gap-2 text-sm text-muted-foreground">
      <span className="sr-only">{label}</span>
      <input
        type="number"
        inputMode="numeric"
        min={min}
        max={max}
        value={draft}
        disabled={disabled}
        title={a.range(min, max, unit)}
        onChange={(event) => {
          setDraft(event.target.value);
          const typed = Number(event.target.value);
          if (event.target.value.trim() !== "" && Number.isInteger(typed) && typed >= min && typed <= max) {
            onChange(typed);
          }
        }}
        onBlur={commit}
        className="h-11 w-20 rounded-xl border border-input bg-transparent px-3 text-right text-base text-foreground tabular-nums disabled:opacity-50"
      />
      {unit}
    </label>
  );
}

/** Um interruptor de verdade (checkbox com role="switch"), com o título e a explicação no rótulo. */
function Toggle({
  checked,
  onChange,
  label,
  description,
}: {
  checked: boolean;
  onChange: (value: boolean) => void;
  label: string;
  description: string;
}) {
  return (
    <label className="flex min-h-12 items-start gap-4">
      <span className="flex flex-1 flex-col gap-0.5">
        <span className="font-medium">{label}</span>
        <span className="text-sm text-muted-foreground">{description}</span>
      </span>
      <input
        type="checkbox"
        role="switch"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="mt-1 size-5 shrink-0 accent-primary"
      />
    </label>
  );
}
