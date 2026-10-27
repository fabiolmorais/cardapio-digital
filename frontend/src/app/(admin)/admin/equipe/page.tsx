import type { Metadata } from "next";
import { TeamManager } from "@/features/team/team-manager";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.team.title} · ${messages.app.name}`,
};

export default function TeamPage() {
  return <TeamManager />;
}
