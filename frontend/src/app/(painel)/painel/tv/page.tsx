import type { Metadata } from "next";
import { TvMode } from "@/features/calls/tv-mode";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.tv.title} · ${messages.app.name}`,
};

/** Modo TV: os chamados ao vivo em tela cheia, para a TV da cozinha, da copa ou do balcão. */
export default function TvPage() {
  return <TvMode />;
}
