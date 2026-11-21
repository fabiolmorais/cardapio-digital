import type { Metadata } from "next";
import { SummaryScreen } from "@/features/reports/summary-settings";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.reports.summary.title} · ${messages.admin.nav.reports} · ${messages.app.name}`,
};

export default function ReportsSummaryRoute() {
  return <SummaryScreen />;
}
