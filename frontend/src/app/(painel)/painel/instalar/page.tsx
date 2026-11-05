import type { Metadata } from "next";
import { InstallGuide } from "@/features/pwa/install-guide";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.pwa.install.title} · ${messages.app.name}`,
};

/** Como pôr o painel na tela de início do celular (Android e iPhone). */
export default function InstallPage() {
  return (
    <main className="mx-auto flex w-full max-w-2xl flex-col gap-6 px-4 py-6 sm:px-8">
      <InstallGuide />
    </main>
  );
}
