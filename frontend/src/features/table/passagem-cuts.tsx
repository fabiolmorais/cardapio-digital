"use client";

import { useState } from "react";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";
import { MesaNotice } from "./mesa-notice";
import type { MenuItem } from "./table-data";
import { placeOrder, type TableOrders } from "./table-orders";

const t = tablePage.rodizio.cuts;
const soldOut = tablePage.menu.soldOut;
const focus = "focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none";
const ON_THE_WAY = ["PENDING", "PREPARING", "READY"];

/**
 * "Pedir um corte específico" (desenho Rodízio de passagem): os cortes do rodízio, cada um com o
 * "Pedir", que abre o ponto (mal passada, ao ponto, bem passada ou tanto faz). O pedido é uma
 * rodada de um item, com o ponto na observação, e vai para os passadores. O corte que já está a
 * caminho mostra o ponto pedido e "A caminho"; o esgotado aparece apagado, sem o "Pedir". Se o
 * pedido não passar (pedidos demais em pouco tempo, o corte acabou), o aviso fica no próprio corte.
 */
export function PassagemCuts({
  token,
  items,
  orders,
}: {
  token: string;
  items: MenuItem[];
  orders: TableOrders["orders"];
}) {
  const [choosing, setChoosing] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<{ itemId: string; message: string } | null>(null);
  // O que está a caminho, pelo nome do corte (o item do pedido guarda o nome copiado).
  const pending = new Map(
    orders
      .filter((o) => o.status !== "REJECTED")
      .flatMap((o) => o.items)
      .filter((i) => i.rodizio && ON_THE_WAY.includes(i.status))
      .map((i) => [i.productName, i.note ?? ""]),
  );

  async function ask(item: MenuItem, point: string) {
    setBusy(true);
    setError(null);
    try {
      await placeOrder(token, [{ productId: item.id, quantity: 1, note: point }], "/rounds");
      setChoosing(null);
    } catch (e) {
      setError({ itemId: item.id, message: (e as Error).message });
    }
    setBusy(false);
  }

  return (
    <section aria-labelledby="cortes" className="flex flex-col gap-2.5">
      <div className="flex flex-col gap-0.5 pt-1">
        <h2 id="cortes" className="text-xl font-bold tracking-tight">
          {t.title}
        </h2>
        <span className="text-sm text-(--mesa-muted)">{t.text}</span>
      </div>
      {items.length === 0 && <p className="text-sm text-(--mesa-muted)">{t.none}</p>}
      {items.map((item) => {
        const onTheWay = pending.get(item.name);
        const open = choosing === item.id && item.available;
        return (
          <div
            key={item.id}
            role="group"
            aria-label={item.name}
            className={cn(
              "flex flex-col gap-3 rounded-[20px] border p-3.5",
              onTheWay !== undefined
                ? "border-(--brand-text)/55 bg-(--brand)/10"
                : "border-(--mesa-stroke) bg-(--mesa-glass)",
            )}
          >
            <div className="flex items-center gap-3">
              <span className="flex flex-1 flex-col">
                <span className={cn("font-semibold", !item.available && "text-(--mesa-muted)")}>{item.name}</span>
                {onTheWay && <span className="text-sm text-(--mesa-muted)">{onTheWay}</span>}
              </span>
              {onTheWay !== undefined ? (
                <span className="rounded-full border border-(--brand-text) px-2.5 py-0.5 text-xs font-semibold text-(--brand-text)">
                  {t.onTheWay}
                </span>
              ) : !item.available ? (
                <span className="rounded-full border border-(--mesa-stroke) px-2.5 py-0.5 text-xs font-semibold text-(--mesa-muted)">
                  {soldOut}
                </span>
              ) : (
                <button
                  type="button"
                  aria-expanded={open}
                  aria-label={open ? t.close : t.askNamed(item.name)}
                  onClick={() => {
                    setError(null);
                    setChoosing(open ? null : item.id);
                  }}
                  className={cn("h-10 rounded-xl border border-(--mesa-stroke) px-4 text-sm font-semibold", focus)}
                >
                  {open ? t.close : t.ask}
                </button>
              )}
            </div>
            {open && (
              <fieldset className="flex flex-col gap-2">
                <legend className="pb-2 text-sm text-(--mesa-muted)">{t.point(item.name)}</legend>
                <div className="flex flex-wrap gap-2">
                  {[...t.points, t.any].map((point) => (
                    <button
                      key={point}
                      type="button"
                      disabled={busy}
                      onClick={() => ask(item, point === t.any ? "" : point)}
                      className={cn(
                        "h-11 rounded-full bg-(--brand) px-4 text-sm font-semibold text-white disabled:opacity-60",
                        focus,
                      )}
                    >
                      {busy ? t.sending : point}
                    </button>
                  ))}
                </div>
              </fieldset>
            )}
            {error?.itemId === item.id && (
              <MesaNotice tone="warning" role="alert">
                {error.message}
              </MesaNotice>
            )}
          </div>
        );
      })}
    </section>
  );
}
