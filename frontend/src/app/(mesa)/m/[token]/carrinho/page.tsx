import type { Metadata } from "next";
import { connection } from "next/server";
import { Suspense } from "react";
import { CartView } from "@/features/table/cart-view";
import { getTable } from "@/features/table/table-data";
import { TableShell } from "@/features/table/table-shell";
import { messages } from "@/messages/pt-BR";

const t = messages.tablePage;

export const metadata: Metadata = {
  title: `${t.cart.title} · ${messages.app.name}`,
  robots: { index: false, follow: false },
};

/** "Seu pedido": o carrinho deste celular e o envio para a cozinha. */
export default function TableCartPage({ params }: PageProps<"/m/[token]/carrinho">) {
  return (
    <Suspense fallback={<p className="p-6 text-muted-foreground">{t.loading}</p>}>
      <Cart params={params} />
    </Suspense>
  );
}

async function Cart({ params }: Pick<PageProps<"/m/[token]/carrinho">, "params">) {
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
    <TableShell token={token} table={table} current="cart">
      <CartView token={token} tableLabel={table.tableLabel} />
    </TableShell>
  );
}
