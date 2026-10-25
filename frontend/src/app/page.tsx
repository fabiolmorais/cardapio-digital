import { Suspense } from "react";
import { PlanList } from "@/features/plans/plan-list";
import { messages } from "@/messages/pt-BR";

const t = messages.home;

export default function HomePage() {
  return (
    <main className="mx-auto w-full max-w-6xl px-4 py-16">
      <section className="mb-16 max-w-2xl">
        <h1 className="text-4xl font-bold tracking-tight sm:text-5xl">{t.headline}</h1>
        <p className="mt-4 text-lg text-muted-foreground">{t.subheadline}</p>
      </section>

      <section aria-labelledby="plans-title">
        <h2 id="plans-title" className="mb-6 text-2xl font-semibold">
          {t.plansTitle}
        </h2>
        <Suspense fallback={<p className="text-muted-foreground">{t.plansLoading}</p>}>
          <PlanList />
        </Suspense>
      </section>
    </main>
  );
}
