import type { Metadata } from "next";
import { PrintStation } from "@/features/printing/print-station";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.panel.printing.title} · ${messages.app.name}`,
};

/** A estação de impressão: aberta no computador do balcão, imprime a comanda de cada pedido. */
export default function PrintingPage() {
  return <PrintStation />;
}
