import * as Sentry from "@sentry/nextjs";
import { sentryDataCollection } from "@/lib/sentry-options";

export function register() {
  if (!process.env.SENTRY_DSN) return;
  Sentry.init({
    dsn: process.env.SENTRY_DSN,
    environment: process.env.SENTRY_ENVIRONMENT ?? "local",
    dataCollection: sentryDataCollection,
    tracesSampleRate: 0.1,
  });
}

export const onRequestError = Sentry.captureRequestError;
