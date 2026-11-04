import type { Metadata } from "next";
import { RodizioManager } from "@/features/rodizio/rodizio-manager";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.rodizio.title} · ${messages.app.name}`,
};

export default function RodiziosPage() {
  return <RodizioManager />;
}
