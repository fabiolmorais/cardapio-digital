import type { Metadata } from "next";
import { DeviceEntry } from "@/features/devices/device-entry";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.pin.team} · ${messages.app.name}`,
};

export default function DevicePage() {
  return <DeviceEntry />;
}
