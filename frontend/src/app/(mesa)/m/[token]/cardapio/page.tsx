import type { Metadata } from "next";
import { connection } from "next/server";
import { Suspense } from "react";
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
      <header className="flex flex-col gap-0.5">
        <span className="text-sm text-(--mesa-muted)">
          {table.establishmentName} · {t.where(table.tableLabel, "")}
        </span>
        <h1 className="text-3xl font-bold tracking-tight">{t.menu.title}</h1>
      </header>
      {menu.categories.length === 0 ? (
        <p className="text-(--mesa-muted)">{t.menu.empty}</p>
      ) : (
        <MenuBrowser token={token} menu={menu} />
      )}
    </TableShell>
  );
}
