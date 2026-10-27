import type { Metadata } from "next";
import { FloorManager } from "@/features/floor/floor-manager";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.floor.title} · ${messages.app.name}`,
};

export default function FloorPage() {
  return <FloorManager />;
}
