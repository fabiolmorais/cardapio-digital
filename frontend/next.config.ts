import type { NextConfig } from "next";
import { withSentryConfig } from "@sentry/nextjs/config";
import { adjustForOldSafari } from "./scripts/cores-safari-antigo.mjs";

const dev = process.env.NODE_ENV !== "production";
const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
// Origens a mais, separadas por espaço (no CI, o S3Mock: http://s3mock:9090).
const extraSources = (process.env.CSP_EXTRA_SOURCES ?? "").trim();

/**
 * O CSP (Guia 34): o que a página pode carregar e de onde. Scripts só deste site (o Next precisa
 * dos inline, sem nonce, porque as páginas são pré-renderizadas); imagens e vídeos de qualquer
 * HTTPS (R2 e Bunny, com domínios que mudam por ambiente); conexões só com a API, o envio de
 * arquivos (R2 e Bunny) e o Sentry. No desenvolvimento, o recarregamento ao vivo (eval e ws).
 */
const contentSecurityPolicy = [
  "default-src 'self'",
  `script-src 'self' 'unsafe-inline'${dev ? " 'unsafe-eval'" : ""}`,
  "style-src 'self' 'unsafe-inline'",
  `img-src 'self' data: blob: https: ${apiUrl} ${extraSources}`,
  `media-src 'self' blob: https: ${apiUrl} ${extraSources}`,
  "font-src 'self'",
  `connect-src 'self' ${apiUrl} https://*.r2.cloudflarestorage.com https://video.bunnycdn.com https://*.b-cdn.net https://*.sentry.io ${extraSources}${dev ? " ws: wss:" : ""}`,
  "worker-src 'self'",
  "manifest-src 'self'",
  "frame-ancestors 'none'",
  "object-src 'none'",
  "base-uri 'self'",
  "form-action 'self'",
]
  .map((directive) => directive.replace(/\s+/g, " ").trim())
  .join("; ");

const securityHeaders = [
  { key: "Content-Security-Policy", value: contentSecurityPolicy },
  { key: "X-Frame-Options", value: "DENY" },
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  { key: "Strict-Transport-Security", value: "max-age=31536000; includeSubDomains" },
  { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=(self)" },
];

const nextConfig: NextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  poweredByHeader: false,
  // Só em desenvolvimento: libera o acesso pelo IP da rede local (testar no celular).
  allowedDevOrigins: (process.env.DEV_ALLOWED_ORIGINS ?? "").split(",").map((o) => o.trim()).filter(Boolean),
  compiler: {
    // O iPhone com iOS 15 (ADR 0009): logo depois da compilação, antes de a Vercel copiar os
    // arquivos. O Sentry encadeia o envio dos source maps depois deste.
    runAfterProductionCompile: async ({ distDir }) => adjustForOldSafari(distDir),
  },
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
  async headers() {
    return [{ source: "/:path*", headers: securityHeaders }];
  },
};

// Envia source maps ao Sentry só quando houver SENTRY_AUTH_TOKEN (CI/Vercel).
export default withSentryConfig(nextConfig, {
  org: process.env.SENTRY_ORG,
  project: process.env.SENTRY_PROJECT,
  authToken: process.env.SENTRY_AUTH_TOKEN,
  silent: !process.env.CI,
  sourcemaps: { disable: !process.env.SENTRY_AUTH_TOKEN },
  telemetry: false,
});
