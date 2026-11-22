"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Minus, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { messages } from "@/messages/pt-BR";
import { fetchTabPeople, saveTabPeople } from "./tabs-api";

const t = messages.panel.tab.people;

/** O máximo que o back aceita (TabPeople.MAX). */
const MAX = 50;

/**
 * Quantas pessoas estão na mesa fora do rodízio (REL-04, o tíquete por pessoa nos relatórios).
 * Opcional: cada toque salva na hora.
 */
export function TabPeople({ tabId }: { tabId: string }) {
  const queryClient = useQueryClient();
  const key = ["tab-people", tabId];
  const people = useQuery({ queryKey: key, queryFn: () => fetchTabPeople(tabId) });
  const save = useMutation({
    mutationFn: (next: number | null) => saveTabPeople(tabId, next),
    onSuccess: (saved) => queryClient.setQueryData(key, saved),
  });
  const current = save.isPending ? (save.variables ?? null) : (people.data ?? null);
  const busy = save.isPending || people.isPending;

  return (
    <section aria-label={t.label} className="flex flex-col gap-2 rounded-2xl border border-border bg-card p-4">
      <div className="flex flex-wrap items-center gap-3">
        <h2 className="mr-auto font-semibold">{t.label}</h2>
        <Button
          variant="outline"
          size="icon-lg"
          aria-label={t.less}
          disabled={busy || current === null}
          onClick={() => save.mutate(current !== null && current > 1 ? current - 1 : null)}
        >
          <Minus aria-hidden />
        </Button>
        <output aria-live="polite" className="min-w-24 text-center font-semibold tabular-nums">
          {current === null ? t.unknown : t.count(current)}
        </output>
        <Button
          variant="outline"
          size="icon-lg"
          aria-label={t.more}
          disabled={busy || (current ?? 0) >= MAX}
          onClick={() => save.mutate((current ?? 0) + 1)}
        >
          <Plus aria-hidden />
        </Button>
      </div>
      {save.isError && (
        <p role="alert" className="text-sm text-destructive">
          {t.error}
        </p>
      )}
    </section>
  );
}
