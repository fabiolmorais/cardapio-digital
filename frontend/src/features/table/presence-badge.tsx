"use client";

import { tablePage } from "@/messages/table.pt-BR";
import { useTableCalls } from "./table-calls";

/** O selo verde "No salão" (desenhos 2.1 e 3.1): aparece com a presença confirmada. */
export function PresenceBadge({ token }: { token: string }) {
  const { present } = useTableCalls(token);
  if (!present) return null;
  return (
    <span className="flex h-6 shrink-0 items-center gap-1.5 rounded-full bg-(--mesa-ok-soft) px-2.5 text-xs font-semibold text-(--mesa-ok)">
      <span aria-hidden className="size-1.5 rounded-full bg-current" />
      {tablePage.present}
    </span>
  );
}
