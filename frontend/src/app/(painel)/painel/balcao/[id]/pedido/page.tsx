import type { Metadata } from "next";
import { Suspense } from "react";
import { AdminShell } from "@/features/admin/admin-shell";
import { StaffOrderScreen } from "@/features/staff-order/staff-order-screen";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.panel.staffOrder.title} · ${messages.app.name}`,
};

/** A equipe lança o pedido na comanda de um cliente do balcão (ADR 0015). */
export default function CounterOrderPage({ params }: PageProps<"/painel/balcao/[id]/pedido">) {
  return (
    <AdminShell>
      <Suspense fallback={<p className="px-4 py-6 text-muted-foreground">{messages.panel.staffOrder.loading}</p>}>
        <Screen params={params} />
      </Suspense>
    </AdminShell>
  );
}

async function Screen({ params }: Pick<PageProps<"/painel/balcao/[id]/pedido">, "params">) {
  const { id } = await params;
  return <StaffOrderScreen counterTabId={id} />;
}
