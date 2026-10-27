import type { Metadata } from "next";
import { TeamPage as TeamAndDevices } from "@/features/team/team-page";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.team.title} · ${messages.app.name}`,
};

export default function TeamPage() {
  return <TeamAndDevices />;
}
