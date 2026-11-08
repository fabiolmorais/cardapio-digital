"use client";

import { ShoppingBag } from "lucide-react";
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
        className="flex h-14 items-center gap-3 rounded-2xl bg-(--brand) px-4 font-semibold text-white shadow-lg focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:ring-offset-2 focus-visible:ring-offset-(--mesa-bg) focus-visible:outline-none"
      >
        <ShoppingBag aria-hidden className="size-5" />
        <span className="flex-1">{t.see}</span>
        <span className="text-sm font-medium opacity-90">{t.items(count)}</span>
        <span aria-hidden className="h-5 w-px bg-white/40" />
        <span>{formatCents(totalCents)}</span>
      </Link>
    </div>
  );
}
