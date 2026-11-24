import { baseUrl } from "@/lib/api/base";
import type { Schemas } from "@/lib/api/client";

type MenuEvent = Schemas["MenuEventItem"];
type MenuEventsBody = Schemas["MenuEventsRequest"];

/** Quantos eventos vão num envio (o limite do servidor) e quantos esperam, no máximo, na fila. */
const MAX_BATCH = 50;
const MAX_QUEUED = 200;
/** De quanto em quanto tempo a fila sai, enquanto a pessoa usa o cardápio. */
const FLUSH_MS = 15_000;

const queues = new Map<string, MenuEvent[]>();
let timer: ReturnType<typeof setTimeout> | null = null;
let listening = false;

/**
 * Conta o que o celular fez no cardápio (etapa C2: prato aberto, reel visto, item no carrinho), para
 * os relatórios de visto x pedido. Anônimo e sem pressa: os eventos juntam numa fila e saem em lote a
 * cada 15 s; ao sair da página, pelo `sendBeacon`, que o navegador entrega mesmo depois de fechar. Se
 * falhar, o evento se perde: nada disso pode atrapalhar o pedido.
 */
export function trackMenuEvent(token: string, kind: MenuEvent["kind"], productId: string, quantity?: number) {
  if (typeof window === "undefined") return;
  const queue = queues.get(token) ?? [];
  if (queue.length >= MAX_QUEUED) return;
  queue.push(kind === "CART_ADD" ? { kind, productId, quantity } : { kind, productId });
  queues.set(token, queue);
  listen();
  timer ??= setTimeout(() => {
    timer = null;
    flush(false);
  }, FLUSH_MS);
}

/** Na saída da página (ou quando ela some atrás de outro app), manda o que estiver na fila. */
function listen() {
  if (listening) return;
  listening = true;
  window.addEventListener("pagehide", () => flush(true));
  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "hidden") flush(true);
  });
}

function flush(leaving: boolean) {
  for (const [token, queue] of queues) {
    while (queue.length > 0) {
      const body: MenuEventsBody = { events: queue.splice(0, MAX_BATCH) };
      send(token, JSON.stringify(body), leaving);
    }
  }
}

/**
 * Como texto, e não JSON: assim o envio não precisa da consulta prévia do CORS, que o `sendBeacon` não
 * faz. O cookie da sessão da mesa vai junto (o servidor confere a sessão desta mesa).
 */
function send(token: string, body: string, leaving: boolean) {
  const url = `${baseUrl}/api/v1/public/tables/${encodeURIComponent(token)}/events`;
  const blob = new Blob([body], { type: "text/plain;charset=UTF-8" });
  if (leaving && typeof navigator.sendBeacon === "function" && navigator.sendBeacon(url, blob)) return;
  void fetch(url, { method: "POST", credentials: "include", body: blob, keepalive: true }).catch(() => {});
}
