import { UtensilsCrossed } from "lucide-react";
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { connection } from "next/server";
import { Suspense } from "react";
import { api } from "@/lib/api/client";
import { messages } from "@/messages/pt-BR";

const t = messages.tablePage;

export const metadata: Metadata = {
  title: messages.app.name,
  // A URL leva o token da mesa: não é para aparecer em busca nenhuma.
  robots: { index: false, follow: false },
};

/**
 * A página que o QR Code abre. Renderizada no servidor (sem JavaScript de tela): abre rápido
 * no 4G. Por enquanto só confirma o restaurante e a mesa; o cardápio vem na Etapa 5.
 */
export default function TablePage({ params }: PageProps<"/m/[token]">) {
  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col justify-center gap-6 px-6 py-10">
      <Suspense fallback={<p className="text-muted-foreground">{t.loading}</p>}>
        <TableWelcome params={params} />
      </Suspense>
    </main>
  );
}

async function TableWelcome({ params }: Pick<PageProps<"/m/[token]">, "params">) {
  await connection();
  const { token } = await params;
  const { data, response } = await api
    .GET("/api/v1/public/tables/{token}", { params: { path: { token } } })
    .catch(() => ({ data: undefined, response: undefined }));

  if (response?.status === 404) notFound();
  if (!data) {
    return (
      <p role="alert" className="text-muted-foreground">
        {t.error}
      </p>
    );
  }

  return (
    <section className="flex flex-col gap-5 rounded-[28px] border border-border bg-card px-6 py-8">
      <span className="flex size-12 items-center justify-center rounded-[14px] bg-primary text-primary-foreground">
        <UtensilsCrossed aria-hidden className="size-6" />
      </span>
      <div className="flex flex-col gap-1">
        <p className="text-sm font-semibold tracking-wide text-brand-text uppercase">{data.establishmentName}</p>
        <h1 className="text-4xl font-bold tracking-tight">{t.table(data.tableLabel)}</h1>
      </div>
      <p className="text-lg">{t.welcome}</p>
      <p className="text-muted-foreground">{t.soon}</p>
    </section>
  );
}
