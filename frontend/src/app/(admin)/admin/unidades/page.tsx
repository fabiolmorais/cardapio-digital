import type { Metadata } from "next";
import { UnitsPage as Units } from "@/features/units/units-page";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.units.title} · ${messages.app.name}`,
};

export default function UnitsPage() {
  return <Units />;
}
