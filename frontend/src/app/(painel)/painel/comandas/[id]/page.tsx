import type { Metadata } from "next";
import { Suspense } from "react";
import { TabDetail } from "@/features/tabs/tab-detail";
import { AdminShell } from "@/features/admin/admin-shell";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.panel.tab.openTitle} · ${messages.app.name}`,
};

/** A comanda de uma mesa, para o garçom conferir e fechar a conta. */
export default function TabPage({ params }: PageProps<"/painel/comandas/[id]">) {
  return (
    <AdminShell>
      <div className="flex w-full max-w-xl flex-col gap-5 px-4 py-6 sm:px-7">
        <Suspense fallback={<p className="text-muted-foreground">{messages.panel.tab.loading}</p>}>
          <Tab params={params} />
        </Suspense>
      </div>
    </AdminShell>
  );
}

async function Tab({ params }: Pick<PageProps<"/painel/comandas/[id]">, "params">) {
  const { id } = await params;
  return <TabDetail id={id} />;
}
