import { Lock } from "lucide-react";
import Link from "next/link";
import { messages } from "@/messages/pt-BR";

const t = messages.reports.full.locked;

/**
 * Um bloco do Business no Pro (desenho 14.9): tracejado, com o cadeado e o "Conhecer o Business". Só
 * descreve o bloco: o dado não sai do servidor.
 */
export function LockedCard({ title, text, small = false }: { title: string; text: string; small?: boolean }) {
  return (
    <section
      aria-label={t.label(title)}
      className="flex min-w-0 flex-col gap-3 rounded-[20px] border border-dashed border-border bg-secondary/40 px-5 py-[18px]"
    >
      <div className="flex items-center gap-2">
        <h2 className={small ? "mr-auto text-sm text-muted-foreground" : "mr-auto font-semibold"}>{title}</h2>
        <span className="inline-flex items-center gap-1.5 rounded-full bg-info/15 px-2.5 py-0.5 text-xs font-semibold text-info">
          <Lock aria-hidden className="size-3.5" />
          {t.badge}
        </span>
      </div>
      <p className="text-sm leading-5 text-muted-foreground">{text}</p>
      <Link
        href="/admin/assinatura"
        className="w-fit text-sm font-semibold text-foreground underline underline-offset-4 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
      >
        {t.more}
      </Link>
    </section>
  );
}
