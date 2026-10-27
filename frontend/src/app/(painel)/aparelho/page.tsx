import type { Metadata } from "next";
import { PairingScreen } from "@/features/devices/pairing-screen";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.pairing.title} · ${messages.app.name}`,
};

export default function DevicePage() {
  return <PairingScreen />;
}
