"use client";

import { keepPreviousData, useQuery, useQueryClient } from "@tanstack/react-query";
import { GripVertical, Plus, Search } from "lucide-react";
import Link from "next/link";
import { useDeferredValue, useId, useState } from "react";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { ToggleSwitch } from "@/components/toggle-switch";
import { Badge } from "@/components/ui/badge";
import { Button, buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { canManageTeam } from "@/features/team/team-rules";
import { useSession } from "@/lib/auth/use-session";
import { formatCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { CategoryFormDialog } from "./category-form-dialog";
import {
  changeAvailability,
  createCategory,
  deactivateCategory,
  fetchCategories,
  fetchProducts,
  renameCategory,
  reorderCategories,
  reorderProducts,
  type MenuCategory,
  type Product,
} from "./menu-api";
import { useReorder } from "./use-reorder";

const t = messages.menu;

type DialogState =
  | { kind: "new-category" }
  | { kind: "rename-category"; category: MenuCategory }
  | { kind: "deactivate-category"; category: MenuCategory }
  | null;

/** Cardápio (desenho 12.2): categorias à esquerda, produtos da categoria (ou da busca) à direita. */
export function MenuManager({ initialCategoryId }: { initialCategoryId?: string }) {
  const { status, session } = useSession();
  const queryClient = useQueryClient();
  const allowed = status === "authenticated" && canManageTeam(session.establishment.role);

  const [selectedId, setSelectedId] = useState<string | null>(initialCategoryId ?? null);
  const [search, setSearch] = useState("");
  // O Next guarda esta tela quando se sai dela. Ao voltar com outra ?categoria= (depois de
  // salvar um produto), a tela segue o endereço, e não o estado guardado.
  const [lastInitial, setLastInitial] = useState(initialCategoryId);
  if (initialCategoryId !== lastInitial) {
    setLastInitial(initialCategoryId);
    setSelectedId(initialCategoryId ?? null);
    setSearch("");
  }
  const [dialog, setDialog] = useState<DialogState>(null);
  const [failure, setFailure] = useState<string | null>(null);
  const q = useDeferredValue(search.trim());
  const hintId = useId();

  const categories = useQuery({ queryKey: ["menu-categories"], queryFn: fetchCategories, enabled: allowed });
  const categoryList = categories.data ?? [];
  // A categoria escolhida pode ter sido desativada noutra aba: cai na primeira.
  const selected = categoryList.find((c) => c.id === selectedId) ?? categoryList[0] ?? null;
  const products = useQuery({
    queryKey: ["products", q ? { q } : { categoryId: selected?.id }],
    queryFn: () => fetchProducts(q ? { q } : { categoryId: selected!.id }),
    enabled: allowed && (q !== "" || selected !== null),
    // Na busca, a lista anterior fica na tela enquanto a nova chega (sem piscar a cada letra).
    placeholderData: q ? keepPreviousData : undefined,
  });

  async function refresh() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["menu-categories"] }),
      queryClient.invalidateQueries({ queryKey: ["products"] }),
      queryClient.invalidateQueries({ queryKey: ["product"] }),
    ]);
  }

  /** Erro ao reordenar ou trocar a disponibilidade: mostra a frase do back e recarrega a lista. */
  async function attempt(action: () => Promise<unknown>) {
    setFailure(null);
    try {
      await action();
    } catch (error) {
      setFailure((error as Error).message);
    }
    await refresh();
  }

  function select(category: MenuCategory) {
    setSelectedId(category.id);
    setSearch("");
    // A categoria vai na URL: ao voltar da edição de um produto, a tela abre nela.
    window.history.replaceState(null, "", `?categoria=${category.id}`);
  }

  const close = (open: boolean) => !open && setDialog(null);

  if (status !== "authenticated" || (allowed && categories.isPending)) {
    return <p className="px-4 py-6 text-muted-foreground sm:px-7">{t.loading}</p>;
  }
  if (!allowed) {
    return (
      <p role="alert" className="px-4 py-6 text-muted-foreground sm:px-7">
        {messages.team.noPermission}
      </p>
    );
  }
  if (categories.isError) {
    return (
      <p role="alert" className="px-4 py-6 text-destructive sm:px-7">
        {t.loadError}
      </p>
    );
  }

  const totalProducts = categoryList.reduce((sum, c) => sum + c.activeProducts, 0);
  const newProductHref = `/admin/cardapio/produtos/novo${selected ? `?categoria=${selected.id}` : ""}`;

  return (
    <div className="flex flex-col gap-6 px-4 py-6 sm:px-7">
      <header className="flex flex-wrap items-end gap-4">
        <div className="mr-auto flex flex-col gap-1">
          <h1 className="text-2xl font-bold tracking-tight">{t.title}</h1>
          <p className="text-sm text-muted-foreground">{t.subtitle(totalProducts, categoryList.length)}</p>
        </div>
        {categoryList.length > 0 && (
          <Link href={newProductHref} className={buttonVariants({ size: "xl" })}>
            <Plus aria-hidden />
            {t.newProduct}
          </Link>
        )}
      </header>

      {failure && (
        <p role="alert" className="text-sm text-destructive">
          {failure}
        </p>
      )}

      {categoryList.length === 0 ? (
        <section className="flex max-w-lg flex-col items-start gap-3 rounded-2xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold">{t.start.title}</h2>
          <p className="text-muted-foreground">{t.start.text}</p>
          <Button size="xl" onClick={() => setDialog({ kind: "new-category" })}>
            {t.start.action}
          </Button>
        </section>
      ) : (
        <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
          <CategoryList
            categories={categoryList}
            selectedId={q ? null : (selected?.id ?? null)}
            onSelect={select}
            hintId={hintId}
            onNew={() => setDialog({ kind: "new-category" })}
            onReorder={(ids) =>
              attempt(async () => {
                await reorderCategories(ids);
              })
            }
          />

          <section className="flex min-w-0 flex-1 flex-col gap-3.5">
            <div className="flex flex-wrap items-center gap-2">
              <div className="relative w-full max-w-sm">
                <Search
                  aria-hidden
                  className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-muted-foreground"
                />
                <Input
                  type="search"
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder={t.search}
                  aria-label={t.search}
                  maxLength={80}
                  className="h-11 rounded-xl pr-3 pl-10 text-base"
                />
              </div>
              {selected && !q && (
                <>
                  <Button
                    variant="outline"
                    className="h-11 px-3"
                    onClick={() => setDialog({ kind: "rename-category", category: selected })}
                  >
                    {t.renameCategory}
                  </Button>
                  <Button
                    variant="ghost"
                    className="h-11 px-3 text-destructive hover:text-destructive"
                    onClick={() => setDialog({ kind: "deactivate-category", category: selected })}
                  >
                    {t.deactivateCategory}
                  </Button>
                </>
              )}
            </div>

            {q && products.data && (
              <p role="status" className="text-sm text-muted-foreground">
                {t.searchResults(products.data.total)}
              </p>
            )}

            <ProductTable
              key={q ? "search" : selected?.id}
              products={products.data?.products ?? []}
              loading={products.isPending}
              failed={products.isError}
              searching={q !== ""}
              hintId={hintId}
              onReorder={(ids) =>
                attempt(async () => {
                  if (selected) await reorderProducts(selected.id, ids);
                })
              }
              onToggle={(product) => attempt(() => changeAvailability(product.id, !product.available))}
            />

            {products.data && products.data.total > products.data.products.length && (
              <p className="text-sm text-muted-foreground">
                {t.partial(products.data.products.length, products.data.total)}
              </p>
            )}
            <p className="text-sm text-muted-foreground">{t.soldOutHint}</p>
          </section>
        </div>
      )}

      <CategoryFormDialog
        key={dialog?.kind === "rename-category" ? dialog.category.id : "new-category"}
        open={dialog?.kind === "new-category" || dialog?.kind === "rename-category"}
        onOpenChange={close}
        category={dialog?.kind === "rename-category" ? dialog.category : undefined}
        onSave={async (name) => {
          if (dialog?.kind === "rename-category") {
            await renameCategory(dialog.category.id, name);
          } else {
            select(await createCategory(name));
          }
          await refresh();
        }}
      />
      <ConfirmDialog
        open={dialog?.kind === "deactivate-category"}
        onOpenChange={close}
        title={dialog?.kind === "deactivate-category" ? t.confirm.deactivateCategoryTitle(dialog.category.name) : ""}
        description={t.confirm.deactivateCategoryText}
        confirmLabel={t.confirm.deactivate}
        onConfirm={async () => {
          if (dialog?.kind !== "deactivate-category") return;
          await deactivateCategory(dialog.category.id);
          setSelectedId(null);
          await refresh();
        }}
      />
    </div>
  );
}

/** A alça de arrastar: um botão, para o teclado alcançar e o leitor de tela anunciar. */
function DragHandle({
  label,
  hintId,
  ...props
}: { label: string; hintId: string } & ReturnType<ReturnType<typeof useReorder>["handleProps"]>) {
  return (
    <button
      type="button"
      aria-label={label}
      aria-describedby={hintId}
      className="flex h-11 w-8 shrink-0 cursor-grab touch-none items-center justify-center rounded-lg text-muted-foreground hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none active:cursor-grabbing"
      {...props}
    >
      <GripVertical aria-hidden className="size-4" />
    </button>
  );
}

function CategoryList({
  categories,
  selectedId,
  hintId,
  onSelect,
  onNew,
  onReorder,
}: {
  categories: MenuCategory[];
  selectedId: string | null;
  hintId: string;
  onSelect: (category: MenuCategory) => void;
  onNew: () => void;
  onReorder: (ids: string[]) => Promise<void>;
}) {
  const reorder = useReorder(categories, onReorder, (c, position, total) =>
    t.reorder.moved(c.name, position, total),
  );

  return (
    <nav aria-label={t.categories} className="w-full rounded-2xl border border-border bg-card p-2 lg:w-64 lg:shrink-0">
      <p id={hintId} className="sr-only">
        {t.reorder.hint}
      </p>
      <p aria-live="polite" className="sr-only">
        {reorder.announcement}
      </p>
      <ul aria-busy={reorder.saving} className={cn("flex flex-col gap-0.5", reorder.saving && "opacity-60")}>
        {reorder.ordered.map((category) => (
          <li
            key={category.id}
            ref={reorder.rowRef(category.id)}
            style={reorder.rowStyle(category.id)}
            className={cn("flex items-center rounded-xl", reorder.draggingId === category.id && "bg-card shadow-lg ring-1 ring-border")}
          >
            <DragHandle label={t.reorder.handle(category.name)} hintId={hintId} {...reorder.handleProps(category)} />
            <button
              type="button"
              aria-pressed={category.id === selectedId}
              onClick={() => onSelect(category)}
              className={cn(
                "flex h-11 min-w-0 flex-1 items-center justify-between gap-2 rounded-xl px-3 text-left text-sm font-medium transition-colors hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                category.id === selectedId && "bg-secondary text-foreground",
              )}
            >
              <span className="truncate">{category.name}</span>
              <span className="text-xs text-muted-foreground">
                <span aria-hidden>{category.activeProducts}</span>
                <span className="sr-only">{t.categoryCount(category.activeProducts)}</span>
              </span>
            </button>
          </li>
        ))}
      </ul>
      <div className="mx-1 my-1.5 border-t border-border" />
      <Button variant="ghost" className="h-11 w-full justify-start px-3 text-primary" onClick={onNew}>
        <Plus aria-hidden />
        {t.newCategory}
      </Button>
    </nav>
  );
}

function ProductTable({
  products,
  loading,
  failed,
  searching,
  hintId,
  onReorder,
  onToggle,
}: {
  products: Product[];
  loading: boolean;
  failed: boolean;
  searching: boolean;
  hintId: string;
  onReorder: (ids: string[]) => Promise<void>;
  onToggle: (product: Product) => Promise<void>;
}) {
  const reorder = useReorder(products, onReorder, (p, position, total) => t.reorder.moved(p.name, position, total));

  return (
    // relative: o texto sr-only (absolute) fica dentro da caixa com rolagem (ver a tela da equipe).
    <div className="relative overflow-x-auto rounded-2xl border border-border bg-card">
      <p aria-live="polite" className="sr-only">
        {reorder.announcement}
      </p>
      <table
        aria-busy={reorder.saving}
        className={cn("w-full border-collapse text-sm", reorder.saving && "opacity-60")}
      >
        <thead>
          <tr className="border-b border-border text-left text-xs text-muted-foreground uppercase sm:tracking-wide">
            <th scope="col" className="w-10 px-1 py-3 font-semibold sm:w-12 sm:px-2">
              <span className="sr-only">{t.columns.order}</span>
            </th>
            <th scope="col" className="px-2 py-3 font-semibold">
              {t.columns.product}
            </th>
            <th scope="col" className="px-2 py-3 font-semibold sm:px-4">
              {t.columns.price}
            </th>
            <th scope="col" className="px-2 py-3 font-semibold sm:px-4">
              {t.columns.available}
            </th>
          </tr>
        </thead>
        <tbody>
          {(loading || failed || products.length === 0) && (
            <tr>
              <td colSpan={4} className={cn("px-4 py-6", failed ? "text-destructive" : "text-muted-foreground")}>
                {loading ? t.loading : failed ? t.loadError : searching ? t.emptySearch : t.emptyCategory}
              </td>
            </tr>
          )}
          {reorder.ordered.map((product) => (
            <tr
              key={product.id}
              ref={reorder.rowRef(product.id)}
              style={reorder.rowStyle(product.id)}
              className={cn(
                "border-b border-border last:border-0",
                reorder.draggingId === product.id && "bg-card shadow-lg ring-1 ring-border",
              )}
            >
              <td className="px-1 py-1 sm:px-2">
                {searching ? (
                  <span className="sr-only">{t.reorder.searching}</span>
                ) : (
                  <DragHandle label={t.reorder.handle(product.name)} hintId={hintId} {...reorder.handleProps(product)} />
                )}
              </td>
              <td className={cn("px-2 py-2", !product.available && "opacity-60")}>
                <Link
                  href={`/admin/cardapio/produtos/${product.id}`}
                  className="flex min-h-11 flex-col justify-center gap-0.5 rounded-lg px-2 hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
                >
                  <span className="font-medium">{product.name}</span>
                  {product.featured && (
                    <Badge variant="secondary" className="mt-0.5">
                      {t.featured}
                    </Badge>
                  )}
                </Link>
              </td>
              <td className={cn("px-2 py-2 whitespace-nowrap sm:px-4", !product.available && "opacity-60")}>
                <span className="font-mono">{formatCents(product.priceCents)}</span>
                {product.promoPriceCents != null && (
                  <span className="block text-xs text-primary">{t.promo(formatCents(product.promoPriceCents))}</span>
                )}
              </td>
              <td className="px-2 py-2 sm:px-4">
                <ToggleSwitch
                  checked={product.available}
                  label={t.availableLabel(product.name)}
                  onToggle={() => onToggle(product)}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
