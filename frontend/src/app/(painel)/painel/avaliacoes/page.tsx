import type { Metadata } from "next";
import { AdminShell } from "@/features/admin/admin-shell";
import { RatingsToday } from "@/features/ratings/ratings-today";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.admin.nav.ratings} · ${messages.app.name}`,
};

/** As avaliações do atendimento de hoje (RF40), para dono e gerente. */
export default function RatingsPage() {
  return (
    <AdminShell>
      <RatingsToday />
    </AdminShell>
  );
}
