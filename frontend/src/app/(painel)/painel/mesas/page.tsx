import type { Metadata } from "next";
import { AdminShell } from "@/features/admin/admin-shell";
import { FloorMap } from "@/features/live/floor-map";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.panel.floorLive.title} · ${messages.app.name}`,
};

/** Mesas (desenho 7.2): o mapa do salão ao vivo e a comanda de cada mesa. */
export default function FloorPage() {
  return (
    <AdminShell>
      <FloorMap />
    </AdminShell>
  );
}
