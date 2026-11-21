import type { Metadata } from "next";
import { Suspense } from "react";
import { KitchenScreen } from "@/features/reports/kitchen-report";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.reports.kitchen.title} · ${messages.admin.nav.reports} · ${messages.app.name}`,
};

export default function ReportsKitchenRoute() {
  return (
    <Suspense fallback={null}>
      <KitchenScreen />
    </Suspense>
  );
}
