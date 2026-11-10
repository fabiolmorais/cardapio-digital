"use client";

import Link from "next/link";
import { formatCents } from "@/lib/format";
import { tablePage } from "@/messages/table.pt-BR";
import { useCart } from "./cart";

const t = tablePage.cartBar;

/** "Ver pedido · 3 itens · R$ 161,70": aparece acima das abas quando o carrinho tem algo. */
export function CartBar({ token }: { token: string }) {
  const { count, totalCents } = useCart(token);
  if (count === 0) return null;
  return (
    <div className="mx-auto w-full max-w-md pb-2.5">
      <Link
        href={`/m/${token}/carrinho`}
        className="flex h-15 items-center gap-3 rounded-[18px] bg-(--brand) pr-4 pl-2.5 font-semibold text-white shadow-[0_14px_30px_rgba(0,0,0,.4)] focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:ring-offset-2 focus-visible:ring-offset-(--mesa-bg) focus-visible:outline-none"
      >
        {/* Desenho 3.1: a quantidade num quadrado, "Ver pedido / Ainda não enviado" e o total. */}
        <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-black/20 font-bold">
          <span className="sr-only">{t.items(count)}</span>
          <span aria-hidden>{count}</span>
        </span>
        <span className="flex flex-1 flex-col leading-tight">
          <span>{t.see}</span>
          <span className="text-xs font-medium opacity-85">{t.notSent}</span>
        </span>
        <span className="text-lg">{formatCents(totalCents)}</span>
      </Link>
    </div>
  );
}
