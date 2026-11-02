"use client";

import { useCallback, useEffect, useState } from "react";
import { baseUrl } from "@/lib/api/base";
import type { components } from "@/lib/api/schema";
import { tablePage } from "@/messages/table.pt-BR";

export type TableCall = components["schemas"]["TableCall"];
export type CallRequest = components["schemas"]["CallRequest"];
export type PaymentMethod = NonNullable<CallRequest["paymentMethod"]>;

/** Sem o tempo real (conexão caída), a página volta a perguntar de tempos em tempos. */
const FALLBACK_REFRESH_MS = 15_000;
/** Quanto esperar para abrir o canal de novo quando o servidor o recusa (sessão vencida, por exemplo). */
const REOPEN_MS = 3000;

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
 * Os chamados abertos da mesa, atualizados na hora pelo tempo real, e as ações. `run` faz uma ação, mostra o erro, se houver, e atualiza a lista depois.
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
    let live = false;
    // A primeira busca fica para logo depois da montagem (e não dentro do efeito), como as outras.
    const first = window.setTimeout(() => void refresh(), 0);
    // Rede de segurança: enquanto o canal não está aberto, pergunta de tempos em tempos.
    const fallback = window.setInterval(() => {
      if (!live && document.visibilityState === "visible") void refresh();
    }, FALLBACK_REFRESH_MS);

    // O canal de tempo real da mesa (SSE): "ready" ao conectar e "call" quando um chamado dela
    // muda. Os dois pedem para buscar de novo; o aviso não traz dados, só o motivo.
    let source: EventSource | null = null;
    let reopen: number | undefined;
    const url = `${baseUrl}/api/v1/public/tables/${encodeURIComponent(token)}/events`;
    function open() {
      source = new EventSource(url, { withCredentials: true });
      const update = () => {
        live = true;
        void refresh();
      };
      source.addEventListener("ready", update);
      source.addEventListener("call", update);
      source.onerror = () => {
        live = false;
        // Se a conexão só caiu, o navegador reconecta sozinho. Se o servidor recusou (401: a
        // sessão venceu), ele desiste: aí a página renova a sessão e abre o canal de novo.
        if (source?.readyState === EventSource.CLOSED) {
          source = null;
          reopen = window.setTimeout(async () => {
            await fetch(`${baseUrl}/api/v1/public/tables/${encodeURIComponent(token)}/session`, {
              method: "POST",
              credentials: "include",
            }).catch(() => undefined);
            open();
          }, REOPEN_MS);
        }
      };
    }
    open();

    return () => {
      window.clearTimeout(first);
      window.clearInterval(fallback);
      window.clearTimeout(reopen);
      source?.close();
    };
  }, [refresh, token]);

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
