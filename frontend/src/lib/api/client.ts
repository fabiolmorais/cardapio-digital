import createClient, { type Middleware } from "openapi-fetch";
import { getFreshAccessToken } from "@/lib/auth/session";
import { clientOptions } from "./base";
import type { components, paths } from "./schema";

/**
 * Cliente tipado da API. Os tipos vêm de `npm run gen:api` (não declarar tipos da API à mão).
 */
export const api = createClient<paths>(clientOptions);

/**
 * No navegador, envia o token de quem entrou no painel em toda chamada, renovando antes se
 * ele estiver para vencer. As rotas de /auth nunca levam o token: um token vencido faria o
 * Spring Security responder 401 mesmo onde não é preciso estar logado.
 */
const auth: Middleware = {
  async onRequest({ request, schemaPath }) {
    if (typeof window === "undefined" || schemaPath.startsWith("/api/v1/auth/")) return;
    const token = await getFreshAccessToken();
    if (token) request.headers.set("Authorization", `Bearer ${token}`);
  },
};
api.use(auth);

export type Schemas = components["schemas"];
