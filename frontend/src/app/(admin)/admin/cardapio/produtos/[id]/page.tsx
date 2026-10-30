import type { Metadata } from "next";
import { Suspense } from "react";
import { ProductEditor } from "@/features/menu/product-editor";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.menu.title} · ${messages.app.name}`,
};

export default function EditProductPage({ params }: PageProps<"/admin/cardapio/produtos/[id]">) {
  return (
    <Suspense fallback={<p className="px-4 py-6 text-muted-foreground sm:px-7">{messages.menu.product.loading}</p>}>
      <EditProduct params={params} />
    </Suspense>
  );
}

async function EditProduct({ params }: Pick<PageProps<"/admin/cardapio/produtos/[id]">, "params">) {
  const { id } = await params;
  return <ProductEditor productId={id} />;
}
