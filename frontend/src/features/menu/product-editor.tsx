"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useId, useState, type FormEvent } from "react";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { canManageTeam } from "@/features/team/team-rules";
import { useSession } from "@/lib/auth/use-session";
import { centsToInput, parseCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import {
  createProduct,
  deactivateProduct,
  fetchCategories,
  fetchProduct,
  updateProduct,
  type MenuCategory,
  type Product,
  type ProductRequest,
} from "./menu-api";

const t = messages.menu.product;
const DESCRIPTION_MAX = 300;

/** Editar produto (desenho 12.3). Sem productId, é o cadastro de um produto novo. */
export function ProductEditor({ productId, initialCategoryId }: { productId?: string; initialCategoryId?: string }) {
  const { status, session } = useSession();
  const allowed = status === "authenticated" && canManageTeam(session.establishment.role);

  const categories = useQuery({ queryKey: ["menu-categories"], queryFn: fetchCategories, enabled: allowed });
  const product = useQuery({
    queryKey: ["product", productId],
    queryFn: () => fetchProduct(productId!),
    enabled: allowed && productId !== undefined,
    retry: false,
  });
  const [round, setRound] = useState(0);

  if (status !== "authenticated" || (allowed && (categories.isPending || (productId && product.isPending)))) {
    return <p className="px-4 py-6 text-muted-foreground sm:px-7">{t.loading}</p>;
  }
  if (!allowed) {
    return (
      <p role="alert" className="px-4 py-6 text-muted-foreground sm:px-7">
        {messages.team.noPermission}
      </p>
    );
  }
  if (categories.isError || product.isError || !categories.data) {
    return (
      <div className="flex flex-col items-start gap-4 px-4 py-6 sm:px-7">
        <p role="alert" className="text-destructive">
          {product.isError ? t.notFound : messages.menu.loadError}
        </p>
        <BackLink href="/admin/cardapio">{t.backToMenu}</BackLink>
      </div>
    );
  }

  // O Next guarda o estado das telas já visitadas. A chave faz o formulário recomeçar: o novo,
  // a cada produto criado; o de edição, sempre que o produto chega de novo do back.
  return (
    <ProductForm
      key={product.data ? `${product.data.id}-${product.dataUpdatedAt}` : `new-${round}`}
      product={product.data}
      categories={categories.data}
      initialCategoryId={initialCategoryId}
      onSaved={() => setRound((n) => n + 1)}
    />
  );
}

function BackLink({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <Link
      href={href}
      className="inline-flex min-h-11 items-center gap-1.5 rounded-lg text-sm text-muted-foreground hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
    >
      <ArrowLeft aria-hidden className="size-4" />
      {children}
    </Link>
  );
}

function ProductForm({
  product,
  categories,
  initialCategoryId,
  onSaved,
}: {
  product?: Product;
  categories: MenuCategory[];
  initialCategoryId?: string;
  onSaved: () => void;
}) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const firstCategory = categories.find((c) => c.id === initialCategoryId) ?? categories[0];

  const [categoryId, setCategoryId] = useState(product?.categoryId ?? firstCategory?.id ?? "");
  const [description, setDescription] = useState(product?.description ?? "");
  const [price, setPrice] = useState(product ? centsToInput(product.priceCents) : "");
  const [promo, setPromo] = useState(product?.promoPriceCents != null ? centsToInput(product.promoPriceCents) : "");
  const [available, setAvailable] = useState(product?.available ?? true);
  const [featured, setFeatured] = useState(product?.featured ?? false);
  const [confirming, setConfirming] = useState(false);
  // O Next mantém telas já visitadas escondidas na página: ids fixos se repetiriam, e um
  // rótulo apontaria para o campo da tela escondida. useId dá um id único a cada formulário.
  const id = useId();
  const formId = `${id}form`;

  const priceCents = parseCents(price);
  const promoCents = parseCents(promo);
  const priceInvalid = price !== "" && Number.isNaN(priceCents);
  const promoInvalid = Number.isNaN(promoCents);
  const category = categories.find((c) => c.id === (product?.categoryId ?? categoryId));
  const menuHref = `/admin/cardapio${category ? `?categoria=${category.id}` : ""}`;

  async function finish(savedCategoryId: string) {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["menu-categories"] }),
      queryClient.invalidateQueries({ queryKey: ["products"] }),
      queryClient.invalidateQueries({ queryKey: ["product"] }),
    ]);
    router.push(`/admin/cardapio?categoria=${savedCategoryId}`);
    onSaved();
  }

  const save = useMutation({
    mutationFn: async (body: ProductRequest) => (product ? updateProduct(product.id, body) : createProduct(body)),
    onSuccess: (saved) => finish(saved.categoryId),
  });

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (priceCents === null || Number.isNaN(priceCents) || promoInvalid) return;
    const form = new FormData(event.currentTarget);
    save.mutate({
      categoryId,
      name: String(form.get("name") ?? "").trim(),
      description: description.trim(),
      priceCents,
      // Sem promoção, o campo não vai: no PUT, isso apaga a promoção que havia.
      ...(promoCents !== null ? { promoPriceCents: promoCents } : {}),
      featured,
      available,
    });
  }

  /** Ao sair do campo, o preço fica no formato de sempre ("27,9" vira "27,90"). */
  function tidy(value: string, set: (value: string) => void) {
    const cents = parseCents(value);
    if (cents !== null && !Number.isNaN(cents)) set(centsToInput(cents));
  }

  return (
    <div className="flex flex-col gap-6 px-4 py-6 sm:px-7">
      <header className="flex flex-wrap items-end gap-4">
        <div className="mr-auto flex min-w-0 flex-col gap-1">
          <BackLink href={menuHref}>{category ? t.back(category.name) : t.backToMenu}</BackLink>
          <h1 className="text-2xl font-bold tracking-tight break-words">{product ? product.name : t.newTitle}</h1>
        </div>
        {product && (
          <Button
            variant="ghost"
            size="xl"
            className="text-destructive hover:text-destructive"
            onClick={() => setConfirming(true)}
          >
            {t.deactivate}
          </Button>
        )}
        <Button type="submit" form={formId} size="xl" disabled={save.isPending}>
          {save.isPending ? t.saving : product ? t.save : t.create}
        </Button>
      </header>

      <form
        id={formId}
        onSubmit={onSubmit}
        className="flex max-w-3xl flex-col gap-5 rounded-2xl border border-border bg-card p-5 sm:p-6"
      >
        <div className="flex flex-col gap-2">
          <Label htmlFor={`${id}name`} className="text-muted-foreground">
            {t.name}
          </Label>
          <Input
            id={`${id}name`}
            name="name"
            required
            maxLength={80}
            autoComplete="off"
            defaultValue={product?.name}
            placeholder={t.namePlaceholder}
            className="h-12 rounded-xl px-4 text-base"
          />
        </div>

        <div className="flex flex-col gap-2">
          <Label htmlFor={`${id}description`} className="text-muted-foreground">
            {t.description}
          </Label>
          <textarea
            id={`${id}description`}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            maxLength={DESCRIPTION_MAX}
            rows={3}
            aria-describedby={`${id}description-hint`}
            className="min-h-24 rounded-xl border border-input bg-transparent px-4 py-3 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 dark:bg-input/30"
          />
          <span id={`${id}description-hint`} className="text-sm text-muted-foreground">
            {t.descriptionHint(description.length, DESCRIPTION_MAX)}
          </span>
        </div>

        <div className="grid gap-5 sm:grid-cols-3">
          <div className="flex flex-col gap-2">
            <Label htmlFor={`${id}category`} className="text-muted-foreground">
              {t.category}
            </Label>
            <select
              id={`${id}category`}
              value={categoryId}
              onChange={(event) => setCategoryId(event.target.value)}
              required
              className="h-12 rounded-xl border border-input bg-transparent px-3 text-base dark:bg-input/30"
            >
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>
          <MoneyField
            id={`${id}price`}
            label={t.price}
            value={price}
            onChange={setPrice}
            onBlur={() => tidy(price, setPrice)}
            invalid={priceInvalid}
            hint={t.priceHint}
            required
          />
          <MoneyField
            id={`${id}promo`}
            label={`${t.promoPrice} (${t.optional})`}
            value={promo}
            onChange={setPromo}
            onBlur={() => tidy(promo, setPromo)}
            invalid={promoInvalid}
            hint={t.promoHint}
          />
        </div>

        <div className="border-t border-border" />

        <ToggleRow title={t.availableTitle} text={t.availableText} checked={available} onChange={setAvailable} />
        <ToggleRow title={t.featuredTitle} text={t.featuredText} checked={featured} onChange={setFeatured} />

        <p role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
          {save.error?.message ?? ""}
        </p>
      </form>

      {product && (
        <ConfirmDialog
          open={confirming}
          onOpenChange={setConfirming}
          title={messages.menu.confirm.deactivateProductTitle(product.name)}
          description={messages.menu.confirm.deactivateProductText}
          confirmLabel={messages.menu.confirm.deactivate}
          onConfirm={async () => {
            await deactivateProduct(product.id);
            await finish(product.categoryId);
          }}
        />
      )}
    </div>
  );
}

function MoneyField({
  id,
  label,
  value,
  onChange,
  onBlur,
  invalid,
  hint,
  required,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  onBlur: () => void;
  invalid: boolean;
  hint: string;
  required?: boolean;
}) {
  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={id} className="text-muted-foreground">
        {label}
      </Label>
      <div className="relative">
        <span
          aria-hidden
          className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-sm font-semibold text-muted-foreground"
        >
          R$
        </span>
        <Input
          id={id}
          inputMode="decimal"
          autoComplete="off"
          required={required}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          onBlur={onBlur}
          aria-invalid={invalid || undefined}
          aria-describedby={`${id}-hint`}
          className="h-12 rounded-xl pr-4 pl-11 font-mono text-base"
        />
      </div>
      <span id={`${id}-hint`} className={cn("text-sm", invalid ? "text-destructive" : "text-muted-foreground")}>
        {invalid ? t.invalidPrice : hint}
      </span>
    </div>
  );
}

/** Uma opção liga/desliga com título e explicação; a linha inteira é o interruptor. */
function ToggleRow({
  title,
  text,
  checked,
  onChange,
}: {
  title: string;
  text: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      onClick={() => onChange(!checked)}
      className="flex min-h-14 items-center justify-between gap-4 rounded-xl px-1 text-left focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
    >
      <span className="flex flex-col gap-0.5">
        <span className="font-medium">{title}</span>
        <span className="text-sm text-muted-foreground">{text}</span>
      </span>
      <span
        aria-hidden
        className={cn(
          "relative h-7 w-12 shrink-0 rounded-full border border-input bg-muted transition-colors",
          checked && "border-transparent bg-success",
        )}
      >
        <span
          className={cn(
            "absolute top-0.5 left-0.5 size-5.5 rounded-full bg-muted-foreground transition-transform",
            checked && "translate-x-5 bg-white",
          )}
        />
      </span>
    </button>
  );
}
