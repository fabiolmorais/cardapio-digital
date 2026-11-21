import type { Metadata } from "next";
import { Suspense } from "react";
import { OverviewScreen } from "@/features/reports/overview-report";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.reports.overview} · ${messages.admin.nav.reports} · ${messages.app.name}`,
};

/** Os filtros ficam no endereço (?periodo=, ?de=&ate=, ?setor=): a tela lê os parâmetros num Suspense. */
export default function ReportsOverviewRoute() {
  return (
    <Suspense fallback={null}>
      <OverviewScreen />
    </Suspense>
  );
}
