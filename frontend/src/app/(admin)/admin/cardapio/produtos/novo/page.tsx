import type { Metadata } from "next";
import { Suspense } from "react";
import { ProductEditor } from "@/features/menu/product-editor";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.menu.product.newTitle} · ${messages.app.name}`,
};

/** ?categoria=… já deixa a categoria escolhida no formulário. */
export default function NewProductPage({ searchParams }: PageProps<"/admin/cardapio/produtos/novo">) {
  return (
    <Suspense fallback={<p className="px-4 py-6 text-muted-foreground sm:px-7">{messages.menu.product.loading}</p>}>
      <NewProduct searchParams={searchParams} />
    </Suspense>
  );
}

async function NewProduct({ searchParams }: Pick<PageProps<"/admin/cardapio/produtos/novo">, "searchParams">) {
  const { categoria } = await searchParams;
  return <ProductEditor initialCategoryId={typeof categoria === "string" ? categoria : undefined} />;
}
