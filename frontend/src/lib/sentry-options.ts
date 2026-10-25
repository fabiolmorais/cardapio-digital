import type * as Sentry from "@sentry/nextjs";

type DataCollection = NonNullable<Parameters<typeof Sentry.init>[0]>["dataCollection"] & object;

/** LGPD: o Sentry não recebe dados pessoais, cookies, cabeçalhos nem corpos de requisição. */
export const sentryDataCollection: DataCollection = {
  userInfo: false,
  cookies: false,
  httpHeaders: false,
  httpBodies: [],
  urlQueryParams: false,
  databaseQueryData: false,
  stackFrameVariables: false,
};
