import type { Metadata } from "next";
import { Suspense } from "react";
import { AdminShell } from "@/features/admin/admin-shell";
import { CounterScreen } from "@/features/counter/counter-screen";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.panel.counter.title} · ${messages.app.name}`,
};

/** O balcão (RF45, ADR 0015): as comandas de quem pede em pé, cada uma com o nome do cliente. */
export default function CounterPage() {
  return (
    <AdminShell>
      <Suspense fallback={null}>
        <CounterScreen />
      </Suspense>
    </AdminShell>
  );
}
