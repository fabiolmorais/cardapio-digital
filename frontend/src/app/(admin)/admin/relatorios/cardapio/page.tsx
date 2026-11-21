import type { Metadata } from "next";
import { Suspense } from "react";
import { MenuScreen } from "@/features/reports/menu-report";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.reports.menu.title} · ${messages.admin.nav.reports} · ${messages.app.name}`,
};

export default function ReportsMenuRoute() {
  return (
    <Suspense fallback={null}>
      <MenuScreen />
    </Suspense>
  );
}
