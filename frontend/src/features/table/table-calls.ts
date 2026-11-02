"use client";

import { useCallback, useEffect, useState } from "react";
import { baseUrl } from "@/lib/api/base";
import type { components } from "@/lib/api/schema";
import { tablePage } from "@/messages/table.pt-BR";

export type TableCall = components["schemas"]["TableCall"];
export type CallRequest = components["schemas"]["CallRequest"];
export type PaymentMethod = NonNullable<CallRequest["paymentMethod"]>;

/** Enquanto o tempo real não chega (Guia 14), a página pergunta de tempos em tempos. */
const REFRESH_MS = 8000;

/**
 * As chamadas da mesa, com fetch direto (como a sessão): o cliente da API do painel traria junto
 * o login da equipe, que a página do cliente não usa e só pesaria no JavaScript.
 *
 * Os botões precisam da sessão do celular (cookie table_session). Se ela venceu ou ainda não
 * existe (401), abre uma sessão e tenta de novo, uma vez.
 */
async function send(token: string, path: string, init: RequestInit = {}): Promise<Response> {
  const base = `${baseUrl}/api/v1/public/tables/${encodeURIComponent(token)}`;
  const go = () => fetch(`${base}${path}`, { ...init, credentials: "include" });
  let response = await go();
  if (response.status === 401) {
    await fetch(`${base}/session`, { method: "POST", credentials: "include" });
    response = await go();
  }
  return response;
}

/** A mensagem do back (Problem Details) ou uma genérica. */
async function failure(response: Response): Promise<Error> {
  const body = await response.json().catch(() => null);
  return new Error(typeof body?.detail === "string" ? body.detail : tablePage.calls.failed);
}

export async function listCalls(token: string): Promise<TableCall[]> {
  const response = await send(token, "/calls");
  if (!response.ok) throw await failure(response);
  return response.json();
}

export async function requestCall(token: string, request: CallRequest): Promise<TableCall> {
  const response = await send(token, "/calls", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
  if (!response.ok) throw await failure(response);
  return response.json();
}

export async function cancelCall(token: string, id: string): Promise<void> {
  const response = await send(token, `/calls/${encodeURIComponent(id)}`, { method: "DELETE" });
  // 409: o chamado já tinha sido encerrado. Para quem cancela, o resultado é o mesmo.
  if (!response.ok && response.status !== 409) throw await failure(response);
}

/**
 * Os chamados abertos da mesa, atualizados de tempos em tempos (só com a página visível), e as
 * ações. `run` faz uma ação, mostra o erro, se houver, e atualiza a lista depois.
 */
export function useTableCalls(token: string) {
  const [calls, setCalls] = useState<TableCall[] | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    try {
      setCalls(await listCalls(token));
    } catch {
      // Falha ao atualizar não apaga o que a tela já mostra: tenta de novo na próxima volta.
    }
  }, [token]);

  useEffect(() => {
    // A primeira busca fica para logo depois da montagem (e não dentro do efeito), como as outras.
    const first = window.setTimeout(() => void refresh(), 0);
    const timer = window.setInterval(() => {
      if (document.visibilityState === "visible") void refresh();
    }, REFRESH_MS);
    const onVisible = () => document.visibilityState === "visible" && void refresh();
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      window.clearTimeout(first);
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [refresh]);

  async function run(key: string, action: () => Promise<unknown>) {
    setBusy(key);
    setError(null);
    try {
      await action();
    } catch (e) {
      setError((e as Error).message);
    }
    await refresh();
    setBusy(null);
  }

  return {
    calls,
    waiter: calls?.find((c) => c.type === "WAITER") ?? null,
    bill: calls?.find((c) => c.type === "BILL") ?? null,
    busy,
    error,
    run,
  };
}
