import type { Metadata } from "next";
import { Suspense } from "react";
import { MenuManager } from "@/features/menu/menu-manager";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.menu.title} · ${messages.app.name}`,
};

/** ?categoria=… abre a tela já naquela categoria (é para lá que a edição de um produto volta). */
export default function MenuPage({ searchParams }: PageProps<"/admin/cardapio">) {
  return (
    <Suspense fallback={<p className="px-4 py-6 text-muted-foreground sm:px-7">{messages.menu.loading}</p>}>
      <Menu searchParams={searchParams} />
    </Suspense>
  );
}

async function Menu({ searchParams }: Pick<PageProps<"/admin/cardapio">, "searchParams">) {
  const { categoria } = await searchParams;
  return <MenuManager initialCategoryId={typeof categoria === "string" ? categoria : undefined} />;
}
