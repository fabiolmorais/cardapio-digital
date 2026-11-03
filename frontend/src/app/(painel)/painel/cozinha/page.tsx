import type { Metadata } from "next";
import { KitchenBoard } from "@/features/orders/kitchen-board";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.panel.kitchen.title} · ${messages.app.name}`,
};

/** A tela da cozinha: a fila em tela cheia, para um tablet ou uma TV com toque. */
export default function KitchenPage() {
  return <KitchenBoard />;
}
