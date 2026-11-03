import { Check } from "lucide-react";
import { tablePage } from "@/messages/table.pt-BR";

/** "Conta fechada": o garçom fechou a comanda, e a visita deste celular acabou. */
export function TabClosed() {
  return (
    <section role="status" className="flex flex-col items-center gap-5 pt-10 text-center">
      <span className="flex size-28 items-center justify-center rounded-full bg-(--mesa-glass)">
        <span className="flex size-18 items-center justify-center rounded-full bg-(--brand) text-white">
          <Check aria-hidden className="size-9" />
        </span>
      </span>
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-bold tracking-tight">{tablePage.closed.title}</h1>
        <p className="text-(--mesa-muted)">{tablePage.closed.text}</p>
      </div>
    </section>
  );
}
