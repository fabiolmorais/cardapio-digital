import createClient, { type Middleware } from "openapi-fetch";
import { getAccessToken } from "@/lib/auth/session";
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

/** No navegador, envia o token de quem entrou no painel em toda chamada. */
const auth: Middleware = {
  onRequest({ request }) {
    if (typeof window === "undefined") return;
    const token = getAccessToken();
    if (token) request.headers.set("Authorization", `Bearer ${token}`);
  },
};
api.use(auth);

export type Schemas = components["schemas"];
