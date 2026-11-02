import type { Metadata } from "next";
import { PresenceEditor } from "@/features/presence/presence-editor";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.presence.title} · ${messages.app.name}`,
};

export default function PresencePage() {
  return <PresenceEditor />;
}
