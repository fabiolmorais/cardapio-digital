import type { Metadata } from "next";
import { PanelHome } from "@/features/account/panel-home";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.panel.title} · ${messages.app.name}`,
};

export default function PanelPage() {
  return (
    <main className="mx-auto flex w-full max-w-3xl flex-col gap-6 px-4 py-10 sm:px-8">
      <PanelHome />
    </main>
  );
}
