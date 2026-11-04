import type { Metadata } from "next";
import { connection } from "next/server";
import { Suspense } from "react";
import { RodizioChooser } from "@/features/table/rodizio-chooser";
import { getTable } from "@/features/table/table-data";
import { TableShell } from "@/features/table/table-shell";
import { messages } from "@/messages/pt-BR";

const t = messages.tablePage;

export const metadata: Metadata = {
  title: `${t.rodizio.chooseTitle} · ${messages.app.name}`,
  robots: { index: false, follow: false },
};

/** "Escolha o rodízio" (desenho Quero o rodízio): os rodízios com o preço de agora e o pedido ao garçom. */
export default function TableRodizioPage({ params }: PageProps<"/m/[token]/rodizio">) {
  return (
    <Suspense fallback={<p className="p-6 text-muted-foreground">{t.loading}</p>}>
      <Rodizio params={params} />
    </Suspense>
  );
}

async function Rodizio({ params }: Pick<PageProps<"/m/[token]/rodizio">, "params">) {
  await connection();
  const { token } = await params;
  const table = await getTable(token);
  if (!table) {
    return (
      <p role="alert" className="p-6 text-muted-foreground">
        {t.error}
      </p>
    );
  }
  return (
    <TableShell token={token} table={table} current="home">
      <RodizioChooser token={token} tableLabel={table.tableLabel} />
    </TableShell>
  );
}
