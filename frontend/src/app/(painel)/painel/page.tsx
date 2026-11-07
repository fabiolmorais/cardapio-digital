import type { Metadata } from "next";
import { PanelHome } from "@/features/account/panel-home";
import { AdminShell } from "@/features/admin/admin-shell";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.panel.title} · ${messages.app.name}`,
};

export default function PanelPage() {
  return (
    <AdminShell>
      <div className="flex w-full flex-col gap-6 px-4 py-6 sm:px-7">
        <PanelHome />
      </div>
    </AdminShell>
  );
}
