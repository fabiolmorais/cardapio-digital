import type { Metadata } from "next";
import { Suspense } from "react";
import { TeamScreen } from "@/features/reports/team-report";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.reports.team.title} · ${messages.admin.nav.reports} · ${messages.app.name}`,
};

export default function ReportsTeamRoute() {
  return (
    <Suspense fallback={null}>
      <TeamScreen />
    </Suspense>
  );
}
