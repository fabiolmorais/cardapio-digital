"use client";

import { Flame, Minus, Plus } from "lucide-react";
import { useState } from "react";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";
import { useCart } from "./cart";
import { MenuItemCard } from "./menu-item";
import type { MenuItem } from "./table-data";
import { placeOrder, type TableOrders } from "./table-orders";

const t = tablePage.rodizio;
const focus = "focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none";

/**
 * A aba Rodízio (desenho Rodízio · rodada): a faixa do rodízio, os produtos incluídos SEM preço,
 * com + e −, e o "Enviar rodada", que manda tudo de uma vez para a cozinha (a preço zero). A
 * rodada é só desta tela: não passa pelo carrinho, que é das coisas com preço.
 */
export function RodizioRound({
  token,
  rodizio,
  items,
  rounds,
}: {
  token: string;
  rodizio: NonNullable<TableOrders["rodizio"]>;
  items: MenuItem[];
  rounds: number;
}) {
  const [counts, setCounts] = useState<Record<string, number>>({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);
  const { count: inCart } = useCart(token);
  const total = Object.values(counts).reduce((sum, n) => sum + n, 0);

  function change(id: string, delta: number) {
    setSent(false);
    setCounts((c) => ({ ...c, [id]: Math.max(0, Math.min(20, (c[id] ?? 0) + delta)) }));
  }

  async function send() {
    setBusy(true);
    setError(null);
    try {
      await placeOrder(
        token,
        Object.entries(counts)
          .filter(([, n]) => n > 0)
          .map(([productId, quantity]) => ({ productId, quantity, note: "" })),
        "/rounds",
      );
      setCounts({});
      setSent(true);
    } catch (e) {
      setError((e as Error).message);
    }
    setBusy(false);
  }

  return (
    <section id="rodizio" aria-labelledby="rodizio-titulo" className="flex scroll-mt-6 flex-col gap-2.5">
      <div className="flex items-center gap-3 rounded-[22px] bg-(--brand) px-3.5 py-3 text-white shadow-lg">
        <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-white/20">
          <Flame aria-hidden className="size-5" />
        </span>
        <span className="flex flex-col">
          <h2 id="rodizio-titulo" className="font-semibold">
            {t.banner(rodizio.name, rodizio.people)}
          </h2>
          <span className="text-sm opacity-90">{t.rounds(rounds)}</span>
        </span>
      </div>

      {items.length === 0 && <p className="text-(--mesa-muted)">{t.empty}</p>}
      {items.map((item) => {
        const n = counts[item.id] ?? 0;
        return (
          <MenuItemCard
            key={item.id}
            item={item}
            price={<span className="text-xs font-semibold tracking-wide text-(--brand-text) uppercase">{t.included}</span>}
            action={
              n > 0 ? (
                <span className="flex shrink-0 items-center gap-1.5">
                  <button
                    type="button"
                    aria-label={t.remove(item.name)}
                    onClick={() => change(item.id, -1)}
                    className={cn("flex size-10 items-center justify-center rounded-xl bg-(--mesa-stroke)", focus)}
                  >
                    <Minus aria-hidden className="size-4" />
                  </button>
                  <span aria-live="polite" className="min-w-5 text-center font-bold">
                    <span className="sr-only">{t.quantity(item.name, n)}</span>
                    <span aria-hidden>{n}</span>
                  </span>
                  <button
                    type="button"
                    aria-label={t.add(item.name)}
                    onClick={() => change(item.id, 1)}
                    className={cn("flex size-10 items-center justify-center rounded-xl bg-(--brand) text-white", focus)}
                  >
                    <Plus aria-hidden className="size-4" />
                  </button>
                </span>
              ) : (
                <button
                  type="button"
                  aria-label={t.add(item.name)}
                  onClick={() => change(item.id, 1)}
                  className={cn("flex size-10 shrink-0 items-center justify-center rounded-xl bg-(--brand) text-white", focus)}
                >
                  <Plus aria-hidden className="size-4" />
                </button>
              )
            }
          />
        );
      })}

      {/* Fica à vista enquanto a pessoa rola os pratos do rodízio, acima das abas (e da barra do carrinho). */}
      <div
        className={cn(
          "sticky z-10 flex flex-col gap-2 rounded-[22px] border border-(--mesa-stroke) bg-(--mesa-bg)/95 p-3 backdrop-blur",
          inCart > 0 ? "bottom-40" : "bottom-20",
        )}
      >
        <div className="flex justify-between text-sm text-(--mesa-muted)">
          <span>{t.count(total)}</span>
          <span>{t.noLimit}</span>
        </div>
        <button
          type="button"
          disabled={total === 0 || busy}
          onClick={send}
          className={cn("h-13 rounded-2xl bg-(--brand) font-semibold text-white disabled:opacity-60", focus)}
        >
          {busy ? t.sendingRound : total > 0 ? t.send(total) : t.choose}
        </button>
        <p role="status" className="text-center text-sm empty:hidden">
          {sent ? t.sent : ""}
        </p>
        {error && (
          <p role="alert" className="text-center text-sm font-medium text-(--brand-text)">
            {error}
          </p>
        )}
      </div>
    </section>
  );
}
