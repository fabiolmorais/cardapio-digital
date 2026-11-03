import type { Metadata } from "next";
import { Suspense } from "react";
import { ClosedTabDetail } from "@/features/tab-history/closed-tab-detail";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.tabHistory.title} · ${messages.app.name}`,
};

export default function ClosedTabPage({ params }: PageProps<"/admin/comandas/[id]">) {
  return (
    <Suspense fallback={<p className="px-4 py-6 text-muted-foreground sm:px-7">{messages.tabHistory.detail.loading}</p>}>
      <ClosedTab params={params} />
    </Suspense>
  );
}

async function ClosedTab({ params }: Pick<PageProps<"/admin/comandas/[id]">, "params">) {
  const { id } = await params;
  return <ClosedTabDetail id={id} />;
}
