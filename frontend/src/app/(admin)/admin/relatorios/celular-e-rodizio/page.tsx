import type { Metadata } from "next";
import { Suspense } from "react";
import { PhoneRodizioScreen } from "@/features/reports/phone-rodizio-report";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.reports.phone.title} · ${messages.admin.nav.reports} · ${messages.app.name}`,
};

export default function ReportsPhoneRoute() {
  return (
    <Suspense fallback={null}>
      <PhoneRodizioScreen />
    </Suspense>
  );
}
