import type { Metadata } from "next";
import { Suspense } from "react";
import { SalesScreen } from "@/features/reports/sales-report";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.reports.sales} · ${messages.admin.nav.reports} · ${messages.app.name}`,
};

export default function ReportsSalesRoute() {
  return (
    <Suspense fallback={null}>
      <SalesScreen />
    </Suspense>
  );
}
