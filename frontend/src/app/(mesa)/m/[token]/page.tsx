import type { Metadata } from "next";
import { connection } from "next/server";
import { Suspense } from "react";
import { TableHome } from "@/features/table/table-home";
import { getMenu, getTable } from "@/features/table/table-data";
import { messages } from "@/messages/pt-BR";

const t = messages.tablePage;

export const metadata: Metadata = {
  title: messages.app.name,
  // A URL leva o token da mesa: não é para aparecer em busca nenhuma.
  robots: { index: false, follow: false },
};

/**
 * A página que o QR Code abre ("Como podemos ajudar?"). Montada no servidor: chega pronta ao
 * celular, rápida no 4G, com pouco JavaScript (só o da sessão).
 */
export default function TablePage({ params }: PageProps<"/m/[token]">) {
  return (
    <Suspense fallback={<p className="p-6 text-muted-foreground">{t.loading}</p>}>
      <Home params={params} />
    </Suspense>
  );
}

async function Home({ params }: Pick<PageProps<"/m/[token]">, "params">) {
  await connection();
  const { token } = await params;
  // As duas buscas em paralelo: a página espera a mais lenta, e não a soma das duas.
  const [table, menu] = await Promise.all([getTable(token), getMenu(token)]);
  if (!table) {
    return (
      <p role="alert" className="p-6 text-muted-foreground">
        {t.error}
      </p>
    );
  }
  return <TableHome token={token} table={table} menu={menu} />;
}
