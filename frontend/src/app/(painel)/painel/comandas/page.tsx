import type { Metadata } from "next";
import { AdminShell } from "@/features/admin/admin-shell";
import { OpenTabs } from "@/features/tabs/open-tabs";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.admin.nav.openTabs} · ${messages.app.name}`,
};

/** As comandas abertas: cada mesa com o total até agora; cada uma abre a comanda para fechar a conta. */
export default function OpenTabsPage() {
  return (
    <AdminShell>
      <div className="flex w-full max-w-2xl flex-col gap-5 px-4 py-6 sm:px-7">
        <OpenTabs />
      </div>
    </AdminShell>
  );
}
