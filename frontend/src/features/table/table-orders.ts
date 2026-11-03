"use client";

import { useCallback, useRef, useState } from "react";
import type { components } from "@/lib/api/schema";
import { tablePage } from "@/messages/table.pt-BR";
import type { CartLine } from "./cart";
import { failure, send } from "./table-calls";
import { useTableChannel } from "./table-channel";

export type TableOrders = components["schemas"]["TableOrders"];
export type TableOrder = components["schemas"]["TableOrder"];
export type TableOrderItem = components["schemas"]["TableOrderItem"];
type PlaceOrderRequest = components["schemas"]["PlaceOrderRequest"];

const t = tablePage.orders;
/** O canal de tempo real avisa quando um pedido da mesa muda, e quando o garçom fecha a conta. */
const ORDER_EVENTS = ["order", "tab"] as const;

/** A hora do pedido, no relógio do celular (que está no restaurante). */
export function timeOf(iso: string) {
  return new Date(iso).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
}

/** Os pedidos da comanda aberta, o total parcial e se o restaurante aceita pedido pelo celular. */
export async function listOrders(token: string): Promise<TableOrders> {
  const response = await send(token, "/orders");
  if (!response.ok) throw await failure(response, t.failed);
  return response.json();
}

/** Envia o carrinho: só o produto, a quantidade e a observação (o back copia o nome e o preço). */
export async function placeOrder(token: string, lines: CartLine[]): Promise<TableOrder> {
  const body: PlaceOrderRequest = {
    items: lines.map((l) => ({ productId: l.productId, quantity: l.quantity, note: l.note || undefined })),
  };
  const response = await send(token, "/orders", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!response.ok) throw await failure(response, t.failed);
  return response.json();
}

export async function cancelOrderItem(token: string, itemId: string): Promise<void> {
  const response = await send(token, `/order-items/${encodeURIComponent(itemId)}`, { method: "DELETE" });
  if (!response.ok) throw await failure(response, t.failed);
}

/**
 * Os pedidos da mesa, atualizados na hora pelo tempo real (a cozinha começou, ficou pronto…), e o
 * cancelamento de um item. `null` enquanto a primeira busca não volta. `closed` fica verdadeiro
 * quando o garçom fecha a conta.
 */
export function useTableOrders(token: string) {
  const [data, setData] = useState<TableOrders | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [closed, setClosed] = useState(false);
  const loaded = useRef(false);

  const refresh = useCallback(async () => {
    try {
      setData(await listOrders(token));
      loaded.current = true;
    } catch (e) {
      // Falha ao atualizar não apaga o que a tela já mostra; sem nada ainda, mostra o erro.
      if (!loaded.current) setError((e as Error).message);
    }
  }, [token]);

  const onUpdate = useCallback(
    (_fromChannel: boolean, event?: string) => {
      // A conta fechou: a sessão deste celular acabou, e a tela agradece em vez de buscar de novo.
      if (event === "tab") setClosed(true);
      else void refresh();
    },
    [refresh],
  );
  useTableChannel(token, ORDER_EVENTS, onUpdate);

  async function cancel(itemId: string) {
    setBusy(itemId);
    setError(null);
    try {
      await cancelOrderItem(token, itemId);
    } catch (e) {
      setError((e as Error).message);
    }
    await refresh();
    setBusy(null);
  }

  return { data, busy, error, closed, cancel };
}
