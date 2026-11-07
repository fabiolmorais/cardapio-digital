import { ChevronRight, Printer } from "lucide-react";
import Link from "next/link";
import { messages } from "@/messages/pt-BR";

/** O atalho para a impressão de comandas (o computador do balcão, com a impressora). */
export function PrintingLink() {
  return (
    <Link
      href="/painel/impressao"
      className="flex min-h-16 items-center gap-4 rounded-2xl border border-border bg-card p-5 transition-colors hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
    >
      <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-primary/15 text-brand-text">
        <Printer aria-hidden className="size-6" />
      </span>
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="text-lg font-semibold">{messages.panel.printing.open}</span>
        <span className="text-sm text-muted-foreground">{messages.panel.printing.openHint}</span>
      </span>
      <ChevronRight aria-hidden className="size-5 text-muted-foreground" />
    </Link>
  );
}
