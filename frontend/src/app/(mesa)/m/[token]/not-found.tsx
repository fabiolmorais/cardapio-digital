import { QrCode } from "lucide-react";
import { messages } from "@/messages/pt-BR";

const t = messages.tablePage;

/** QR trocado, inventado ou de mesa desativada: a mesma mensagem, sem dizer qual dos casos. */
export default function TableNotFound() {
  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col justify-center gap-4 px-6 py-10">
      <QrCode aria-hidden className="size-12 text-muted-foreground" />
      <h1 className="text-2xl font-bold tracking-tight">{t.invalidTitle}</h1>
      <p className="text-muted-foreground">{t.invalidText}</p>
    </main>
  );
}
