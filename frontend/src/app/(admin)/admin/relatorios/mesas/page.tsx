import type { Metadata } from "next";
import { Suspense } from "react";
import { TablesScreen } from "@/features/reports/table-report";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.reports.tables.title} · ${messages.admin.nav.reports} · ${messages.app.name}`,
};

export default function ReportsTablesRoute() {
  return (
    <Suspense fallback={null}>
      <TablesScreen />
    </Suspense>
  );
}
