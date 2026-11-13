import type { Metadata } from "next";
import { connection } from "next/server";
import { Suspense } from "react";
import { PresenceBadge } from "@/features/table/presence-badge";
import { MenuBrowser } from "@/features/table/menu-browser";
import { getMenu, getTable } from "@/features/table/table-data";
import { TableShell } from "@/features/table/table-shell";
import { messages } from "@/messages/pt-BR";

const t = messages.tablePage;

export const metadata: Metadata = {
  title: `${t.menu.title} · ${messages.app.name}`,
  robots: { index: false, follow: false },
};

/** O cardápio da mesa (desenho Cardápio): abas por categoria, busca, "de/por" e "Esgotado hoje". */
export default function TableMenuPage({ params }: PageProps<"/m/[token]/cardapio">) {
  return (
    <Suspense fallback={<p className="p-6 text-muted-foreground">{t.loading}</p>}>
      <Menu params={params} />
    </Suspense>
  );
}

async function Menu({ params }: Pick<PageProps<"/m/[token]/cardapio">, "params">) {
  await connection();
  const { token } = await params;
  const [table, menu] = await Promise.all([getTable(token), getMenu(token)]);
  if (!table || !menu) {
    return (
      <p role="alert" className="p-6 text-muted-foreground">
        {t.error}
      </p>
    );
  }
  return (
    <TableShell token={token} table={table} current="menu">
      <header className="flex items-start gap-3">
        <div className="flex min-w-0 flex-1 flex-col gap-0.5">
          <span className="truncate text-sm text-(--mesa-muted)">
            {table.establishmentName} · {t.where(table.tableLabel, "")}
          </span>
          <h1 className="text-3xl font-bold tracking-tight">{t.menu.title}</h1>
        </div>
        <PresenceBadge token={token} />
      </header>
      {/* Mesmo sem nada à la carte, o cardápio monta: a aba Rodízio (e o cartão do passagem) mora nele. */}
      <MenuBrowser token={token} menu={menu} serving={table.serving} />
    </TableShell>
  );
}
