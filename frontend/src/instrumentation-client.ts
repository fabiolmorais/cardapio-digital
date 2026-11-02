import { sentryDataCollection } from "@/lib/sentry-options";

/**
 * O Sentry no navegador é carregado SOB DEMANDA, e só quando há chave (NEXT_PUBLIC_SENTRY_DSN).
 * Importado direto no topo, o SDK (mais de 70 KB comprimido) entrava em toda página, mesmo sem
 * chave, e a página da mesa passava do orçamento de 200 KB de JavaScript (CLAUDE.md). Assim:
 * - sem chave (local e CI), o SDK nem entra no pacote: o if abaixo some no build;
 * - com chave (produção), ele chega depois que a página já abriu, sem atrasar o cardápio.
 */
let captureTransition: ((href: string, navigationType: string) => void) | undefined;

if (process.env.NEXT_PUBLIC_SENTRY_DSN) {
  void import("@sentry/nextjs").then((Sentry) => {
    Sentry.init({
      dsn: process.env.NEXT_PUBLIC_SENTRY_DSN,
      environment: process.env.NEXT_PUBLIC_SENTRY_ENVIRONMENT ?? "local",
      dataCollection: sentryDataCollection,
      tracesSampleRate: 0.1,
    });
    captureTransition = Sentry.captureRouterTransitionStart;
  });
}

export function onRouterTransitionStart(href: string, navigationType: string) {
  captureTransition?.(href, navigationType);
}
