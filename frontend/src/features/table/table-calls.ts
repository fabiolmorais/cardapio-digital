"use client";

import { useCallback, useEffect, useState } from "react";
import { baseUrl } from "@/lib/api/base";
import type { components } from "@/lib/api/schema";
import { tablePage } from "@/messages/table.pt-BR";
import { useTableChannel } from "./table-channel";

export type TableCall = components["schemas"]["TableCall"];
export type CallRequest = components["schemas"]["CallRequest"];
export type PaymentMethod = NonNullable<CallRequest["paymentMethod"]>;
export type Presence = components["schemas"]["Presence"];
type Position = { latitude: number; longitude: number; accuracyMeters: number };

/** O canal de tempo real avisa quando um chamado da mesa muda, e quando o garçom fecha a conta. */
const CALL_EVENTS = ["call", "tab"] as const;

/**
 * As chamadas da mesa, com fetch direto (como a sessão): o cliente da API do painel traria junto
 * o login da equipe, que a página do cliente não usa e só pesaria no JavaScript.
 *
 * Os botões precisam da sessão do celular (cookie table_session) e da presença no salão. Se a
 * sessão venceu ou ainda não existe (401), abre uma; se a presença ainda não foi conferida (403),
 * confere (pela rede do salão). Nos dois casos, tenta de novo, uma vez.
 */
export async function send(token: string, path: string, init: RequestInit = {}): Promise<Response> {
  const base = `${baseUrl}/api/v1/public/tables/${encodeURIComponent(token)}`;
  const go = () => fetch(`${base}${path}`, { ...init, credentials: "include" });
  await startVisit(token); // a que a página já começou, se houver: assim não nascem duas sessões
  let response = await go();
  if (response.status === 401) {
    await startVisit(token, true);
    response = await go();
  } else if (response.status === 403) {
    await checkPresence(token);
    response = await go();
  }
  return response;
}

const visits = new Map<string, Promise<Presence | null>>();

/**
 * Abre (ou renova) a sessão do celular e confere a presença, uma vez por página. Quem chega
 * depois (um botão tocado logo ao abrir) espera a mesma, em vez de abrir outra sessão no meio:
 * duas ao mesmo tempo deixariam o navegador com o cookie da que ainda não conferiu a presença.
 *
 * @param fresh abre uma nova mesmo que já haja uma (a sessão venceu: 401)
 */
export function startVisit(token: string, fresh = false): Promise<Presence | null> {
  let visit = visits.get(token);
  if (!visit || fresh) {
    visit = fetch(`${baseUrl}/api/v1/public/tables/${encodeURIComponent(token)}/session`, {
      method: "POST",
      credentials: "include",
    })
      .then(() => checkPresence(token))
      .catch(() => null);
    visits.set(token, visit);
  }
  return visit;
}

/**
 * "Estou no salão?": sem a posição, pela rede (sem pedir nada ao cliente); com ela, também pela
 * localização. Devolve como está a presença, ou null se não deu para perguntar.
 */
export async function checkPresence(token: string, position?: Position): Promise<Presence | null> {
  try {
    const response = await fetch(`${baseUrl}/api/v1/public/tables/${encodeURIComponent(token)}/presence`, {
      method: "POST",
      credentials: "include",
      ...(position && { headers: { "Content-Type": "application/json" }, body: JSON.stringify(position) }),
    });
    return response.ok ? ((await response.json()) as Presence) : null;
  } catch {
    return null;
  }
}

/** A posição do celular, pedida ao navegador (ele pergunta ao cliente na primeira vez). */
function currentPosition(): Promise<Position> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error("unsupported"));
      return;
    }
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => resolve({ latitude: coords.latitude, longitude: coords.longitude, accuracyMeters: coords.accuracy }),
      reject,
      { enableHighAccuracy: true, timeout: 15_000, maximumAge: 0 },
    );
  });
}

/** A mensagem do back (Problem Details) ou uma genérica. */
export async function failure(response: Response, fallback: string = tablePage.calls.failed): Promise<Error> {
  const body = await response.json().catch(() => null);
  return new Error(typeof body?.detail === "string" ? body.detail : fallback);
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
 * Os chamados abertos da mesa e a presença no salão, atualizados na hora pelo tempo real, e as
 * ações. `run` faz uma ação, mostra o erro, se houver, e atualiza a lista depois.
 */
export function useTableCalls(token: string) {
  const [calls, setCalls] = useState<TableCall[] | null>(null);
  const [presence, setPresence] = useState<Presence | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [closed, setClosed] = useState(false);
  const present = presence?.present === true;

  /**
   * Pergunta de novo a cada aviso do tempo real: é assim que a liberação pelo garçom (e o bloqueio
   * pela equipe) aparecem na hora.
   */
  const refreshPresence = useCallback(async () => {
    const next = await checkPresence(token);
    if (next) setPresence(next);
  }, [token]);

  const refresh = useCallback(async () => {
    try {
      setCalls(await listCalls(token));
    } catch {
      // Falha ao atualizar não apaga o que a tela já mostra: tenta de novo na próxima volta.
    }
  }, [token]);

  useEffect(() => {
    let cancelled = false;
    void startVisit(token).then((first) => {
      if (first && !cancelled) setPresence(first);
    });
    return () => {
      cancelled = true;
    };
  }, [token]);

  // Cada aviso do canal busca os chamados e a presença; a rede de segurança, só os chamados.
  const onUpdate = useCallback(
    (fromChannel: boolean, event?: string) => {
      // A conta fechou: a sessão deste celular acabou, e a tela agradece em vez de buscar de novo.
      if (event === "tab") {
        setClosed(true);
        return;
      }
      void refresh();
      if (fromChannel) void refreshPresence();
    },
    [refresh, refreshPresence],
  );
  useTableChannel(token, CALL_EVENTS, onUpdate);

  /** "Usar minha localização": pede a posição ao navegador e manda para a conferência. */
  async function locate() {
    setBusy("locate");
    setError(null);
    try {
      const next = await checkPresence(token, await currentPosition());
      if (next) setPresence(next);
      if (!next?.present) setError(tablePage.presence.outside);
    } catch (e) {
      // 1 é "permissão negada" (GeolocationPositionError.PERMISSION_DENIED).
      setError((e as { code?: number }).code === 1 ? tablePage.presence.denied : tablePage.presence.noLocation);
    }
    setBusy(null);
  }

  /** "Já me conectei ao Wi-Fi": confere a rede de novo. */
  async function recheck() {
    setBusy("wifi");
    setError(null);
    const next = await checkPresence(token);
    if (next) setPresence(next);
    if (!next?.present) setError(tablePage.presence.notOnWifi);
    setBusy(null);
  }

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
    closed,
    presence,
    present,
    unlock: calls?.find((c) => c.type === "UNLOCK") ?? null,
    locate,
    recheck,
    waiter: calls?.find((c) => c.type === "WAITER") ?? null,
    bill: calls?.find((c) => c.type === "BILL") ?? null,
    rodizio: calls?.find((c) => c.type === "RODIZIO") ?? null,
    busy,
    error,
    run,
  };
}
