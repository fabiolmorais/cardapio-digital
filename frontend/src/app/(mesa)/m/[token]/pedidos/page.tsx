import type { Metadata } from "next";
import { connection } from "next/server";
import { Suspense } from "react";
import { OrdersView } from "@/features/table/orders-view";
import { getTable } from "@/features/table/table-data";
import { TableShell } from "@/features/table/table-shell";
import { messages } from "@/messages/pt-BR";

const t = messages.tablePage;

export const metadata: Metadata = {
  title: `${t.orders.title} · ${messages.app.name}`,
  robots: { index: false, follow: false },
};

/** "Meus pedidos": os pedidos da comanda da mesa, com o passo de cada item. */
export default function TableOrdersPage({ params }: PageProps<"/m/[token]/pedidos">) {
  return (
    <Suspense fallback={<p className="p-6 text-muted-foreground">{t.loading}</p>}>
      <Orders params={params} />
    </Suspense>
  );
}

async function Orders({ params }: Pick<PageProps<"/m/[token]/pedidos">, "params">) {
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
    <TableShell token={token} table={table} current="orders">
      <OrdersView token={token} tableLabel={table.tableLabel} />
    </TableShell>
  );
}
