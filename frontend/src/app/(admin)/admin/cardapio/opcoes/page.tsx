import type { Metadata } from "next";
import { OptionGroupsManager } from "@/features/menu/option-groups-manager";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.options.title} · ${messages.app.name}`,
};

/** A biblioteca de grupos de opções do restaurante (ADR 0010). */
export default function OptionGroupsPage() {
  return <OptionGroupsManager />;
}
