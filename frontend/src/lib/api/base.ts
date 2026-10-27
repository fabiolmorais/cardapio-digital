import createClient from "openapi-fetch";
import type { paths } from "./schema";

/**
 * Endereço da API. No servidor usa API_URL (rede interna); no navegador, NEXT_PUBLIC_API_URL.
 */
export const baseUrl =
  typeof window === "undefined"
    ? (process.env.API_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080")
    : (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080");

/**
 * No navegador, as chamadas levam e recebem cookies da API. É assim que o cookie HttpOnly
 * do refresh token é gravado no login e enviado no /auth/refresh e no /auth/logout.
 */
export const clientOptions = {
  baseUrl,
  credentials: typeof window === "undefined" ? undefined : ("include" as const),
};

/** Cliente sem token de acesso: usado só pelas rotas de sessão (/auth/refresh e /auth/logout). */
export const authApi = createClient<paths>(clientOptions);
