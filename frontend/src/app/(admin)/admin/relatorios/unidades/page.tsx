import type { Metadata } from "next";
import { Suspense } from "react";
import { UnitsScreen } from "@/features/reports/units-report";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.reports.units.title} · ${messages.admin.nav.reports} · ${messages.app.name}`,
};

export default function ReportsUnitsRoute() {
  return (
    <Suspense fallback={null}>
      <UnitsScreen />
    </Suspense>
  );
}
