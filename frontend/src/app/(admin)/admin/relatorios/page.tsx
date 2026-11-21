import type { Metadata } from "next";
import { Suspense } from "react";
import { ReportsPage } from "@/features/reports/reports-page";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.admin.nav.reports} · ${messages.app.name}`,
};

/** O período fica no endereço (?periodo=): a página lê os parâmetros dentro de um Suspense. */
export default function ReportsRoute() {
  return (
    <Suspense fallback={null}>
      <ReportsPage />
    </Suspense>
  );
}
