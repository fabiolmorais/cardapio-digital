"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Minus, Plus, Search } from "lucide-react";
import Link from "next/link";
import { useEffect, useMemo, useRef, useState, type CSSProperties } from "react";
import { Button, buttonVariants } from "@/components/ui/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { useNow } from "@/features/calls/call-queue";
import { ItemChoicesText } from "@/features/orders/item-choices";
import { ChoicePicker } from "@/features/table/choice-picker";
import {
  choicesText,
  evaluate,
  groupsById,
  hasChoices,
  rulesOf,
  searchable,
  toChoices,
  type GroupsById,
  type Picked,
} from "@/features/table/choices";
import type { MenuItem } from "@/features/table/table-data";
import { useRequiredSession } from "@/lib/auth/use-required-session";
import { formatCents } from "@/lib/format";
import { subscribeStaffEvents } from "@/lib/realtime/shared-staff-events";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import {
  DuplicateOrderError,
  fetchStaffMenu,
  fetchStaffOrderSheet,
  placeStaffOrder,
  type AlreadyOrderedItem,
} from "./staff-order-api";

const t = messages.panel.staffOrder;
const itemStatus = messages.tablePage.orders.status;
const MAX_QUANTITY = 20;
const MAX_LINES = 30;
const MAX_NOTE = 140;

/** As escolhas usam o componente da mesa: aqui ele veste as cores do painel. */
const PANEL_COLORS = {
  "--mesa-fg": "var(--foreground)",
  "--mesa-muted": "var(--muted-foreground)",
  "--mesa-glass": "var(--card)",
  "--mesa-stroke": "var(--border)",
  "--brand": "var(--primary)",
} as CSSProperties;

type Mode = "menu" | "round";
type Line = {
  key: string;
  productId: string;
  name: string;
  unitCents: number;
  quantity: number;
  note: string;
  variantId?: string;
  options: { optionId: string; quantity: number }[];
  choices: string;
};
type Decision = "remove" | "confirm";

/** O código do envio. `randomUUID` só existe em HTTPS, e o teste no celular pela rede local é HTTP. */
function newRequestId() {
  if (crypto.randomUUID) return crypto.randomUUID();
  const b = crypto.getRandomValues(new Uint8Array(16));
  b[6] = (b[6] & 0x0f) | 0x40;
  b[8] = (b[8] & 0x3f) | 0x80;
  const hex = [...b].map((x) => x.toString(16).padStart(2, "0")).join("");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

function lineKey(productId: string, variantId: string | undefined, options: Line["options"]) {
  const chosen = [...options].sort((a, b) => a.optionId.localeCompare(b.optionId));
  return JSON.stringify([productId, variantId ?? null, chosen]);
}

const isOnTheWay = (item: AlreadyOrderedItem) => ["PENDING", "PREPARING", "READY"].includes(item.status);

export function StaffOrderScreen({ tableId }: { tableId: string }) {
  const { status } = useRequiredSession();
  if (status !== "authenticated") return <p className="px-4 py-6 text-muted-foreground sm:px-7">{t.loading}</p>;
  return <Screen tableId={tableId} />;
}

function Screen({ tableId }: { tableId: string }) {
  const queryClient = useQueryClient();
  const sheet = useQuery({
    queryKey: ["staff-order", tableId],
    queryFn: () => fetchStaffOrderSheet(tableId),
    retry: false,
  });
  const menu = useQuery({ queryKey: ["staff-menu"], queryFn: fetchStaffMenu, retry: false });

  // O que outro garçom ou um celular pede aparece aqui na hora.
  useEffect(
    () =>
      subscribeStaffEvents(
        (event) => {
          if (["ready", "order", "tab", "rodizio"].includes(event.name)) {
            void queryClient.invalidateQueries({ queryKey: ["staff-order", tableId] });
          }
        },
        () => {},
      ),
    [queryClient, tableId],
  );

  const back = (
    <Link href="/painel/mesas" className={cn(buttonVariants({ variant: "ghost" }), "h-11 self-start")}>
      <ArrowLeft aria-hidden />
      {t.back}
    </Link>
  );
  if (sheet.isPending || menu.isPending) return <p className="px-4 py-6 text-muted-foreground sm:px-7">{t.loading}</p>;
  if (!sheet.data || !menu.data) {
    return (
      <div className="flex flex-col gap-3 px-4 py-6 sm:px-7">
        {back}
        <p role="alert" className="text-destructive">
          {t.loadError}
        </p>
      </div>
    );
  }
  if (!sheet.data.orderingEnabled) {
    return (
      <div className="flex flex-col gap-3 px-4 py-6 sm:px-7">
        {back}
        <p role="alert">{t.disabled}</p>
      </div>
    );
  }
  return <OrderForm tableId={tableId} sheet={sheet.data} menu={menu.data} back={back} />;
}

function OrderForm({
  tableId,
  sheet,
  menu,
  back,
}: {
  tableId: string;
  sheet: Awaited<ReturnType<typeof fetchStaffOrderSheet>>;
  menu: Awaited<ReturnType<typeof fetchStaffMenu>>;
  back: React.ReactNode;
}) {
  const queryClient = useQueryClient();
  const [mode, setMode] = useState<Mode>("menu");
  const [carts, setCarts] = useState<Record<Mode, Line[]>>({ menu: [], round: [] });
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState<string | null>(null);
  const [choosing, setChoosing] = useState<MenuItem | null>(null);
  const [reviewing, setReviewing] = useState(false);
  const [duplicates, setDuplicates] = useState<AlreadyOrderedItem[]>([]);
  const [sent, setSent] = useState<number | null>(null);
  const [full, setFull] = useState(false);
  // O mesmo código enquanto o pedido não muda: repetir por falha de rede não cria outro pedido.
  const requestId = useRef<string | null>(null);

  const round = mode === "round";
  const lines = carts[mode];
  const groups = useMemo(() => groupsById(menu.optionGroups), [menu.optionGroups]);
  const rodizioProducts = useMemo(() => new Set(sheet.rodizio?.productIds ?? []), [sheet.rodizio]);
  const query = searchable(search.trim());
  const count = lines.reduce((sum, l) => sum + l.quantity, 0);
  const total = lines.reduce((sum, l) => sum + l.unitCents * l.quantity, 0);

  const categories = menu.categories
    .map((c) => ({
      ...c,
      products: c.products.filter((p) => (round ? rodizioProducts.has(p.id) : p.aLaCarte)),
    }))
    .filter((c) => c.products.length > 0);
  const shown = categories
    .filter((c) => query || category === null || c.id === category)
    .map((c) => ({
      ...c,
      products: c.products.filter((p) => !query || searchable(p.name).includes(query)),
    }))
    .filter((c) => c.products.length > 0);

  function changeLines(next: (current: Line[]) => Line[]) {
    requestId.current = null;
    setSent(null);
    setFull(false);
    setCarts((all) => ({ ...all, [mode]: next(all[mode]) }));
  }

  function add(line: Omit<Line, "key">) {
    const key = lineKey(line.productId, line.variantId, line.options);
    if (!lines.some((l) => l.key === key) && lines.length >= MAX_LINES) {
      setFull(true);
      return;
    }
    changeLines((current) =>
      current.some((l) => l.key === key)
        ? current.map((l) =>
            l.key === key
              ? { ...l, quantity: Math.min(MAX_QUANTITY, l.quantity + line.quantity), note: l.note || line.note }
              : l,
          )
        : [...current, { ...line, key }],
    );
  }

  function setQuantity(key: string, quantity: number) {
    changeLines((current) =>
      quantity <= 0
        ? current.filter((l) => l.key !== key)
        : current.map((l) => (l.key === key ? { ...l, quantity } : l)),
    );
  }

  function addSimple(item: MenuItem) {
    add({
      productId: item.id,
      name: item.name,
      unitCents: round ? 0 : (item.promoPriceCents ?? item.priceCents),
      quantity: 1,
      note: "",
      options: [],
      choices: "",
    });
  }

  const mutation = useMutation({
    mutationFn: ({ items, confirmed }: { items: Line[]; confirmed: string[] }) => {
      requestId.current ??= newRequestId();
      return placeStaffOrder(tableId, {
        clientRequestId: requestId.current,
        round,
        confirmedDuplicates: confirmed,
        items: items.map((l) => ({
          productId: l.productId,
          quantity: l.quantity,
          note: l.note.trim() || undefined,
          variantId: l.variantId,
          options: l.options,
        })),
      });
    },
    onSuccess: (order) => {
      requestId.current = null;
      setCarts((all) => ({ ...all, [mode]: [] }));
      setReviewing(false);
      setDuplicates([]);
      setSent(order.number);
      void queryClient.invalidateQueries({ queryKey: ["staff-order", tableId] });
    },
    onError: (error) => {
      if (error instanceof DuplicateOrderError) {
        setReviewing(false);
        setDuplicates(error.duplicates);
        void queryClient.invalidateQueries({ queryKey: ["staff-order", tableId] });
      }
    },
  });

  function resolveDuplicates(decisions: Record<string, Decision>) {
    const kept = lines.filter((l) => decisions[l.productId] !== "remove");
    const confirmed = Object.keys(decisions).filter((id) => decisions[id] === "confirm");
    changeLines(() => kept);
    setDuplicates([]);
    if (kept.length > 0) mutation.mutate({ items: kept, confirmed });
  }

  const orderedCount = (productId: string) =>
    sheet.alreadyOrdered
      .filter((i) => i.productId === productId && isOnTheWay(i))
      .reduce((sum, i) => sum + i.quantity, 0);
  const orderedRecently = (productId: string) => sheet.alreadyOrdered.some((i) => i.productId === productId);
  const failure = mutation.error instanceof DuplicateOrderError ? null : mutation.error?.message;

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-4 px-4 pt-4 sm:px-7">
      {back}
      <header className="flex flex-col gap-0.5">
        <h1 className="text-[28px] leading-[34px] font-bold tracking-tight">{t.heading(sheet.tableLabel)}</h1>
        <p className="text-sm text-muted-foreground">{t.hint}</p>
      </header>

      {sent !== null && (
        <p role="status" className="rounded-xl bg-success/15 px-3.5 py-3 text-sm font-medium text-success">
          {t.sent(sent)}
        </p>
      )}

      {sheet.alreadyOrdered.length > 0 && (
        <section
          aria-label={t.alreadyTitle}
          className="flex flex-col gap-2 rounded-2xl border border-border bg-card p-3.5"
        >
          <h2 className="text-sm font-semibold">{t.alreadyTitle}</h2>
          <AlreadyList items={sheet.alreadyOrdered} />
        </section>
      )}

      {sheet.rodizio && (
        <div className="grid grid-cols-2 gap-2" aria-label={t.modeLabel} role="group">
          {(["menu", "round"] as const).map((m) => (
            <Button
              key={m}
              type="button"
              variant={mode === m ? "default" : "outline"}
              aria-pressed={mode === m}
              className="h-11"
              onClick={() => {
                setMode(m);
                setCategory(null);
                setSent(null);
              }}
            >
              {m === "menu" ? t.modeMenu : t.modeRound(sheet.rodizio!.name)}
            </Button>
          ))}
        </div>
      )}

      <div className="relative">
        <Search aria-hidden className="absolute top-1/2 left-3.5 size-4.5 -translate-y-1/2 text-muted-foreground" />
        <Input
          type="search"
          aria-label={t.search}
          placeholder={t.search}
          autoComplete="off"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="h-12 rounded-xl pl-10"
        />
      </div>

      {!query && categories.length > 1 && (
        <nav aria-label={t.categories} className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1">
          {[{ id: null, name: t.allCategories }, ...categories].map((c) => (
            <Button
              key={c.id ?? "all"}
              type="button"
              size="sm"
              variant={category === c.id ? "default" : "outline"}
              aria-pressed={category === c.id}
              className="h-10 shrink-0 rounded-full px-4"
              onClick={() => setCategory(c.id)}
            >
              {c.name}
            </Button>
          ))}
        </nav>
      )}

      {shown.length === 0 && <p className="py-6 text-center text-muted-foreground">{t.noResults}</p>}
      {shown.map((c) => (
        <section key={c.id} aria-label={c.name} className="flex flex-col gap-1">
          <h2 className="pt-1 text-sm font-semibold text-muted-foreground">{c.name}</h2>
          <ul className="flex flex-col divide-y divide-border">
            {c.products.map((item) => {
              const inCart = lines.filter((l) => l.productId === item.id);
              const quantity = inCart.reduce((sum, l) => sum + l.quantity, 0);
              const simple = !hasChoices(item);
              const onTheWay = orderedCount(item.id);
              return (
                <li key={item.id} className="flex min-h-14 items-center gap-3 py-2">
                  <span className="flex min-w-0 flex-1 flex-col">
                    <span className={cn("font-medium", !item.available && "text-muted-foreground")}>{item.name}</span>
                    <span className="flex flex-wrap items-center gap-x-2 text-[13px] text-muted-foreground">
                      {!round && (
                        <span>
                          {item.fromPriceCents != null
                            ? t.from(formatCents(item.fromPriceCents))
                            : formatCents(item.promoPriceCents ?? item.priceCents)}
                        </span>
                      )}
                      {!item.available && <span>{t.soldOut}</span>}
                      {onTheWay > 0 ? (
                        <span className="font-medium text-warning">{t.onTheWay(onTheWay)}</span>
                      ) : (
                        orderedRecently(item.id) && <span className="font-medium text-warning">{t.justOrdered}</span>
                      )}
                    </span>
                  </span>
                  {simple && quantity > 0 ? (
                    <Stepper
                      name={item.name}
                      quantity={quantity}
                      onChange={(next) => setQuantity(inCart[0].key, next)}
                    />
                  ) : (
                    <Button
                      type="button"
                      variant="outline"
                      disabled={!item.available}
                      aria-label={t.add(item.name)}
                      className="h-11 min-w-11 rounded-xl"
                      onClick={() => (simple ? addSimple(item) : setChoosing(item))}
                    >
                      <Plus aria-hidden />
                      {quantity > 0 && quantity}
                    </Button>
                  )}
                </li>
              );
            })}
          </ul>
        </section>
      ))}

      {full && (
        <p role="alert" className="text-sm text-destructive">
          {t.full}
        </p>
      )}

      {count > 0 && (
        <div className="sticky bottom-0 z-20 -mx-4 border-t border-border bg-background/95 px-4 py-3 backdrop-blur sm:-mx-7 sm:px-7">
          <Button type="button" className="h-13 w-full" onClick={() => setReviewing(true)}>
            {t.review(count, round ? null : formatCents(total))}
          </Button>
        </div>
      )}

      {choosing && (
        <ChoiceDialog
          key={choosing.id}
          item={choosing}
          groups={groups}
          round={round}
          onClose={() => setChoosing(null)}
          onAdd={(line) => {
            add(line);
            setChoosing(null);
          }}
        />
      )}

      <Dialog open={reviewing} onOpenChange={setReviewing}>
        <DialogContent className="max-h-[90dvh] overflow-y-auto sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>{t.reviewTitle(sheet.tableLabel)}</DialogTitle>
          </DialogHeader>
          <ul className="flex flex-col gap-3">
            {lines.map((line) => (
              <li key={line.key} className="flex flex-col gap-2 rounded-xl border border-border p-3">
                <div className="flex items-center gap-3">
                  <span className="flex min-w-0 flex-1 flex-col">
                    <span className="font-medium">{line.name}</span>
                    {line.choices && <span className="text-[13px] text-muted-foreground">{line.choices}</span>}
                    {!round && (
                      <span className="text-[13px] text-muted-foreground">
                        {formatCents(line.unitCents * line.quantity)}
                      </span>
                    )}
                  </span>
                  <Stepper name={line.name} quantity={line.quantity} onChange={(next) => setQuantity(line.key, next)} />
                </div>
                <Input
                  aria-label={t.noteOf(line.name)}
                  placeholder={t.notePlaceholder}
                  maxLength={MAX_NOTE}
                  value={line.note}
                  onChange={(e) =>
                    changeLines((current) =>
                      current.map((l) => (l.key === line.key ? { ...l, note: e.target.value } : l)),
                    )
                  }
                  className="h-11 rounded-xl"
                />
              </li>
            ))}
          </ul>
          {failure && (
            <p role="alert" className="text-sm text-destructive">
              {failure}
            </p>
          )}
          <DialogFooter>
            <Button
              type="button"
              className="h-12 w-full"
              disabled={lines.length === 0 || mutation.isPending}
              onClick={() => mutation.mutate({ items: lines, confirmed: [] })}
            >
              {mutation.isPending ? t.sending : t.send(round ? null : formatCents(total))}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {duplicates.length > 0 && (
        <DuplicateDialog
          key={duplicates.map((d) => `${d.productId}${d.orderedAt}`).join()}
          duplicates={duplicates}
          tableLabel={sheet.tableLabel}
          onCancel={() => {
            setDuplicates([]);
            setReviewing(true);
          }}
          onResolve={resolveDuplicates}
        />
      )}
    </div>
  );
}

function Stepper({
  name,
  quantity,
  onChange,
}: {
  name: string;
  quantity: number;
  onChange: (quantity: number) => void;
}) {
  return (
    <span className="flex shrink-0 items-center gap-1">
      <Button
        type="button"
        variant="outline"
        size="icon-lg"
        className="size-11 rounded-xl"
        aria-label={t.less(name)}
        onClick={() => onChange(quantity - 1)}
      >
        <Minus aria-hidden />
      </Button>
      <span aria-label={t.quantityOf(name)} className="w-7 text-center font-semibold">
        {quantity}
      </span>
      <Button
        type="button"
        variant="outline"
        size="icon-lg"
        className="size-11 rounded-xl"
        aria-label={t.more(name)}
        disabled={quantity >= MAX_QUANTITY}
        onClick={() => onChange(quantity + 1)}
      >
        <Plus aria-hidden />
      </Button>
    </span>
  );
}

/** O que a mesa já pediu: quanto, há quanto tempo, quem pediu e em que pé está. */
function AlreadyList({ items }: { items: AlreadyOrderedItem[] }) {
  const now = useNow();
  return (
    <ul className="flex flex-col gap-1.5 text-sm">
      {items.map((item) => {
        const minutes = Math.max(0, Math.floor((now - Date.parse(item.orderedAt)) / 60_000));
        const who = item.byStaff ? (item.placedByName ?? t.team) : t.phone;
        return (
          <li key={`${item.productId}-${item.orderedAt}-${item.quantity}`} className="flex flex-col">
            <span className="font-medium">
              {item.quantity}× {item.productName}
            </span>
            <ItemChoicesText of={item.choices} className="text-[13px]" />
            <span className="text-[13px] text-muted-foreground">
              {t.ago(minutes)} · {who} · {itemStatus[item.status]}
            </span>
          </li>
        );
      })}
    </ul>
  );
}

/** A trava de duplicados: para cada produto repetido, tirar do pedido ou confirmar que é outro. */
function DuplicateDialog({
  duplicates,
  tableLabel,
  onCancel,
  onResolve,
}: {
  duplicates: AlreadyOrderedItem[];
  tableLabel: string;
  onCancel: () => void;
  onResolve: (decisions: Record<string, Decision>) => void;
}) {
  const [decisions, setDecisions] = useState<Record<string, Decision>>({});
  const products = [...new Map(duplicates.map((d) => [d.productId, d.productName])).entries()];
  const decided = products.every(([id]) => decisions[id]);
  return (
    <Dialog open onOpenChange={(open) => !open && onCancel()}>
      <DialogContent className="max-h-[90dvh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{t.duplicateTitle(tableLabel)}</DialogTitle>
        </DialogHeader>
        <div className="flex flex-col gap-3">
          {products.map(([id, name]) => (
            <section key={id} aria-label={name} className="flex flex-col gap-2.5 rounded-xl border border-border p-3">
              <AlreadyList items={duplicates.filter((d) => d.productId === id)} />
              <div className="grid gap-2 sm:grid-cols-2">
                <Button
                  type="button"
                  variant={decisions[id] === "remove" ? "default" : "outline"}
                  aria-pressed={decisions[id] === "remove"}
                  aria-label={t.removeOf(name)}
                  className="h-11"
                  onClick={() => setDecisions((d) => ({ ...d, [id]: "remove" }))}
                >
                  {t.remove}
                </Button>
                <Button
                  type="button"
                  variant={decisions[id] === "confirm" ? "default" : "outline"}
                  aria-pressed={decisions[id] === "confirm"}
                  aria-label={t.confirmOf(name)}
                  className="h-auto min-h-11 whitespace-normal"
                  onClick={() => setDecisions((d) => ({ ...d, [id]: "confirm" }))}
                >
                  {t.confirm}
                </Button>
              </div>
            </section>
          ))}
        </div>
        <DialogFooter>
          <Button type="button" className="h-12 w-full" disabled={!decided} onClick={() => onResolve(decisions)}>
            {t.continue}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

/** O tamanho, as opções, a quantidade e a observação de um produto com escolhas. */
function ChoiceDialog({
  item,
  groups,
  round,
  onClose,
  onAdd,
}: {
  item: MenuItem;
  groups: GroupsById;
  round: boolean;
  onClose: () => void;
  onAdd: (line: Omit<Line, "key">) => void;
}) {
  // Na rodada vale o tamanho do rodízio; com um tamanho só, ele já vem escolhido.
  const [variantId, setVariantId] = useState<string | null>(() =>
    round
      ? ((item.variants.find((v) => v.rodizioDefault) ?? item.variants[0])?.id ?? null)
      : item.variants.length === 1
        ? item.variants[0].id
        : null,
  );
  const [picked, setPicked] = useState<Picked>({});
  const [quantity, setQuantity] = useState(1);
  const [note, setNote] = useState("");
  const evaluation = evaluate(item, groups, variantId, picked, round);
  const ready = evaluation.missing === undefined;

  function chooseVariant(next: string) {
    setVariantId(next);
    // Num tamanho com menos escolhas, o que passou do novo máximo sai.
    const kept = { ...picked };
    for (const link of item.optionGroups) {
      const group = groups.get(link.groupId);
      if (!group) continue;
      const max = rulesOf(group, link, next).max;
      const mine = group.items.filter((o) => kept[o.id]);
      if (mine.reduce((sum, o) => sum + kept[o.id], 0) > max) mine.forEach((o) => delete kept[o.id]);
    }
    setPicked(kept);
  }

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[90dvh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{item.name}</DialogTitle>
        </DialogHeader>
        <div style={PANEL_COLORS} className="flex flex-col gap-4">
          <ChoicePicker
            item={item}
            groups={groups}
            variantId={variantId}
            onVariant={chooseVariant}
            picked={picked}
            onPicked={setPicked}
            round={round}
          />
        </div>
        <Input
          aria-label={t.noteOf(item.name)}
          placeholder={t.notePlaceholder}
          maxLength={MAX_NOTE}
          value={note}
          onChange={(e) => setNote(e.target.value)}
          className="h-11 rounded-xl"
        />
        <DialogFooter className="flex-row items-center gap-3">
          <Stepper name={item.name} quantity={quantity} onChange={(next) => setQuantity(Math.max(1, next))} />
          <Button
            type="button"
            className="h-11 flex-1"
            disabled={!ready}
            onClick={() =>
              onAdd({
                productId: item.id,
                name: item.name,
                unitCents: evaluation.unitCents,
                quantity,
                note,
                variantId: variantId ?? undefined,
                options: Object.entries(picked).map(([optionId, times]) => ({ optionId, quantity: times })),
                choices: choicesText(toChoices(item, groups, variantId, picked)),
              })
            }
          >
            {ready
              ? t.addLine(round ? null : formatCents(evaluation.unitCents * quantity))
              : t.missing(evaluation.missing ?? t.size)}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
