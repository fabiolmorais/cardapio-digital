import type { Metadata } from "next";
import { connection } from "next/server";
import { Suspense } from "react";
import { BillRequest } from "@/features/table/bill-request";
import { getTable } from "@/features/table/table-data";
import { TableShell } from "@/features/table/table-shell";
import { messages } from "@/messages/pt-BR";

const t = messages.tablePage;

export const metadata: Metadata = {
  title: `${t.bill.title} · ${messages.app.name}`,
  robots: { index: false, follow: false },
};

/** Pedir a conta: a forma de pagamento e, depois, "Conta solicitada" com quem está vindo. */
export default function TableBillPage({ params }: PageProps<"/m/[token]/conta">) {
  return (
    <Suspense fallback={<p className="p-6 text-muted-foreground">{t.loading}</p>}>
      <Bill params={params} />
    </Suspense>
  );
}

async function Bill({ params }: Pick<PageProps<"/m/[token]/conta">, "params">) {
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
      <BillRequest token={token} reviewUrl={table.brand.googleReviewUrl ?? null} />
    </TableShell>
  );
}
