import type { Metadata } from "next";
import { Suspense } from "react";
import { TabDetail } from "@/features/tabs/tab-detail";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.panel.tab.openTitle} · ${messages.app.name}`,
};

/** A comanda de uma mesa, para o garçom conferir e fechar a conta. */
export default function TabPage({ params }: PageProps<"/painel/comandas/[id]">) {
  return (
    <main className="mx-auto flex w-full max-w-xl flex-col gap-5 px-4 py-8 sm:px-8">
      <Suspense fallback={<p className="text-muted-foreground">{messages.panel.tab.loading}</p>}>
        <Tab params={params} />
      </Suspense>
    </main>
  );
}

async function Tab({ params }: Pick<PageProps<"/painel/comandas/[id]">, "params">) {
  const { id } = await params;
  return <TabDetail id={id} />;
}
