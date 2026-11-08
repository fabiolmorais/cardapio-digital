import type { Metadata } from "next";
import { connection } from "next/server";
import { Suspense } from "react";
import { Reels } from "@/features/table/reels";
import { StartSession } from "@/features/table/start-session";
import { getMenu, getTable } from "@/features/table/table-data";
import { tableThemeVars } from "@/lib/table-theme";
import { messages } from "@/messages/pt-BR";

const t = messages.tablePage;

export const metadata: Metadata = {
  title: `${t.reels.title} · ${messages.app.name}`,
  robots: { index: false, follow: false },
};

/**
 * O Reels do cardápio: os pratos com foto ou vídeo, em tela cheia, a partir do que o cliente tocou
 * (?produto=). Sem as abas de baixo: a tela é toda do vídeo, e o "Fechar" volta ao cardápio.
 */
export default function TableReelsPage({ params, searchParams }: PageProps<"/m/[token]/reels">) {
  return (
    <Suspense fallback={<p className="p-6 text-muted-foreground">{t.reels.loading}</p>}>
      <ReelsPage params={params} searchParams={searchParams} />
    </Suspense>
  );
}

async function ReelsPage({ params, searchParams }: Pick<PageProps<"/m/[token]/reels">, "params" | "searchParams">) {
  await connection();
  const { token } = await params;
  const { produto, de } = await searchParams;
  const [table, menu] = await Promise.all([getTable(token), getMenu(token)]);
  if (!table || !menu) {
    return (
      <p role="alert" className="p-6 text-muted-foreground">
        {t.error}
      </p>
    );
  }
  const brand = table.brand;
  return (
    <div style={tableThemeVars(brand.theme, brand.primary, brand.accent)}>
      <StartSession token={token} />
      <Reels
        token={token}
        menu={menu}
        startId={typeof produto === "string" ? produto : null}
        establishmentName={table.establishmentName}
        backHref={de === "inicio" ? `/m/${token}` : `/m/${token}/cardapio`}
      />
    </div>
  );
}
