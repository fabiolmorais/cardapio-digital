"use client";

import { Check, ChevronsUpDown } from "lucide-react";
import Link from "next/link";
import { useEffect, useId, useRef, useState } from "react";
import { useMe } from "@/features/account/use-me";
import { switchEstablishment } from "@/lib/auth/session";
import { initials } from "@/lib/initials";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";

const t = messages.admin.switcher;

type Current = { id: string; name: string };

/**
 * O topo do menu (desenho 12.10): o nome da unidade. Com duas unidades ou mais, vira o botão que
 * abre "Suas unidades", com o papel em cada uma, o ✓ na de agora e, para o dono, "Gerenciar
 * unidades". Escolher outra troca a sessão (o back abre uma nova lá) e recomeça no "Ao vivo".
 */
export function UnitSwitcher({ current, subtitle }: { current: Current | null; subtitle: string }) {
  const me = useMe(current !== null);
  const units = me.data?.establishments ?? [];
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);
  const box = useRef<HTMLDivElement>(null);
  const listId = useId();

  useEffect(() => {
    if (!open) return;
    const onDown = (event: PointerEvent) => {
      if (!box.current?.contains(event.target as Node)) setOpen(false);
    };
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    document.addEventListener("pointerdown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("pointerdown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const badge = (
    <span
      aria-hidden
      className="flex size-9 shrink-0 items-center justify-center rounded-[10px] bg-primary text-[13px] font-bold text-primary-foreground"
    >
      {current ? initials(current.name) : ""}
    </span>
  );

  // Uma unidade só (ou ainda carregando): o nome, sem botão.
  if (!current || units.length < 2) {
    return (
      <>
        {badge}
        <span className="flex min-w-0 flex-1 flex-col">
          <span className="truncate font-semibold">{current?.name ?? messages.admin.loading}</span>
          <span className="text-sm text-muted-foreground">{subtitle}</span>
        </span>
      </>
    );
  }

  async function choose(id: string) {
    if (id === current?.id) {
      setOpen(false);
      return;
    }
    setBusy(id);
    setFailed(false);
    // Deu certo: a página recomeça na unidade nova. Senão, avisa e deixa tentar de novo.
    if (!(await switchEstablishment(id))) {
      setBusy(null);
      setFailed(true);
    }
  }

  const isOwner = units.some((u) => u.id === current.id && u.role === "OWNER");

  return (
    <div ref={box} className="relative min-w-0 flex-1">
      <button
        type="button"
        aria-label={t.label(current.name)}
        aria-expanded={open}
        aria-controls={listId}
        onClick={() => setOpen((v) => !v)}
        className="flex w-full min-w-0 items-center gap-3 rounded-xl border border-border bg-secondary/60 p-2 text-left hover:bg-secondary focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
      >
        {badge}
        <span className="flex min-w-0 flex-1 flex-col">
          <span className="truncate font-semibold">{current.name}</span>
          <span className="truncate text-sm text-muted-foreground">{t.hint}</span>
        </span>
        <ChevronsUpDown aria-hidden className="size-4 shrink-0 text-muted-foreground" />
      </button>
      {open && (
        <div
          id={listId}
          className="absolute inset-x-0 top-full z-30 mt-2 flex flex-col gap-0.5 rounded-2xl border border-border bg-popover p-2 text-popover-foreground shadow-2xl"
        >
          <span className="px-2.5 pt-1.5 pb-1 text-[11px] font-semibold tracking-[.4px] text-muted-foreground uppercase">
            {t.menu}
          </span>
          <ul aria-label={t.menu} className="flex flex-col gap-0.5">
            {units.map((unit) => {
              const here = unit.id === current.id;
              return (
                <li key={unit.id}>
                  <button
                    type="button"
                    aria-current={here ? "true" : undefined}
                    disabled={busy !== null}
                    onClick={() => void choose(unit.id)}
                    className={cn(
                      "flex min-h-12 w-full items-center gap-3 rounded-xl px-2.5 py-2 text-left hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none disabled:opacity-60",
                      here && "bg-secondary",
                    )}
                  >
                    <span
                      aria-hidden
                      className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted text-xs font-bold"
                    >
                      {initials(unit.name)}
                    </span>
                    <span className="flex min-w-0 flex-1 flex-col">
                      <span className="truncate text-sm font-semibold">{unit.name}</span>
                      <span className="text-xs text-muted-foreground">
                        {busy === unit.id ? t.switching : messages.panel.roles[unit.role]}
                      </span>
                    </span>
                    {here && <Check aria-hidden className="size-4 shrink-0 text-brand-text" />}
                  </button>
                </li>
              );
            })}
          </ul>
          {failed && (
            <p role="alert" className="px-2.5 py-1 text-sm text-destructive">
              {t.error}
            </p>
          )}
          {isOwner && (
            <>
              <div className="my-1 border-t border-border" />
              <Link
                href="/admin/unidades"
                onClick={() => setOpen(false)}
                className="flex h-11 items-center rounded-xl px-2.5 text-sm font-medium hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
              >
                {t.manage}
              </Link>
            </>
          )}
        </div>
      )}
    </div>
  );
}
