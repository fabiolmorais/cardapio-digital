"use client";

import { ChevronLeft, Download } from "lucide-react";
import Link from "next/link";
import { useSyncExternalStore } from "react";
import { Button, buttonVariants } from "@/components/ui/button";
import { canPromptInstall, promptInstall, subscribeToInstallPrompt } from "@/lib/pwa/install-prompt";
import { isStandalone } from "@/lib/pwa/service-worker";
import { messages } from "@/messages/pt-BR";

const t = messages.pwa.install;

const noop = () => () => {};

/** O passo a passo da instalação, com o botão "Instalar o app" quando o navegador oferece. */
export function InstallGuide() {
  const canInstall = useSyncExternalStore(subscribeToInstallPrompt, canPromptInstall, () => false);
  const installed = useSyncExternalStore(noop, isStandalone, () => false);

  return (
    <>
      <header className="flex items-center gap-3">
        <Link href="/painel" aria-label={t.back} className={buttonVariants({ variant: "outline", size: "icon-lg" })}>
          <ChevronLeft aria-hidden />
        </Link>
        <h1 className="text-2xl font-bold">{t.title}</h1>
      </header>
      <p className="text-muted-foreground">{installed ? t.installed : t.intro}</p>
      {canInstall && !installed && (
        <Button size="lg" className="h-12 self-start" onClick={() => void promptInstall()}>
          <Download aria-hidden />
          {t.button}
        </Button>
      )}
      <Steps title={t.android} steps={t.androidSteps} />
      <Steps title={t.iphone} steps={t.iphoneSteps} />
    </>
  );
}

function Steps({ title, steps }: { title: string; steps: readonly string[] }) {
  return (
    <section aria-label={title} className="flex flex-col gap-3 rounded-2xl border border-border bg-card p-5">
      <h2 className="text-lg font-semibold">{title}</h2>
      <ol className="flex list-decimal flex-col gap-2 pl-5">
        {steps.map((step) => (
          <li key={step}>{step}</li>
        ))}
      </ol>
    </section>
  );
}
