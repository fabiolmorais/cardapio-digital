import createClient from "openapi-fetch";
import type { components, paths } from "./schema";

/**
 * Cliente tipado da API. Os tipos vêm de `npm run gen:api` (não declarar tipos da API à mão).
 * No servidor usa API_URL (rede interna); no navegador, NEXT_PUBLIC_API_URL.
 */
const baseUrl =
  typeof window === "undefined"
    ? (process.env.API_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080")
    : (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080");

export const api = createClient<paths>({ baseUrl });

export type Schemas = components["schemas"];
