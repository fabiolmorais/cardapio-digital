"use client";

import { ChevronRight, Smartphone } from "lucide-react";
import Link from "next/link";
import { useSyncExternalStore } from "react";
import { isStandalone } from "@/lib/pwa/service-worker";
import { messages } from "@/messages/pt-BR";

const t = messages.pwa.install;

const noop = () => () => {};

/** O atalho para o passo a passo da instalação. Dentro do app instalado, some. */
export function InstallLink() {
  const installed = useSyncExternalStore(noop, isStandalone, () => true);
  if (installed) return null;
  return (
    <Link
      href="/painel/instalar"
      className="flex min-h-16 items-center gap-4 rounded-2xl border border-border bg-card p-5 transition-colors hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
    >
      <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-secondary text-foreground">
        <Smartphone aria-hidden className="size-6" />
      </span>
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="text-lg font-semibold">{t.link}</span>
        <span className="text-sm text-muted-foreground">{t.linkHint}</span>
      </span>
      <ChevronRight aria-hidden className="size-5 text-muted-foreground" />
    </Link>
  );
}
