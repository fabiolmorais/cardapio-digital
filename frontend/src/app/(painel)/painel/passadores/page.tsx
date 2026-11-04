import type { Metadata } from "next";
import { PassagemBoard } from "@/features/rodizio/passagem-board";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.panel.passagem.title} · ${messages.app.name}`,
};

/** A tela dos passadores: as mesas do rodízio de passagem e a cor de cada cartão. */
export default function PassagemPage() {
  return <PassagemBoard />;
}
