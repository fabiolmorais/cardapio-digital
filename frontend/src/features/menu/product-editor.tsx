"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { fetchRodizios, setProductRodizios } from "@/features/rodizio/rodizio-api";
import { useEffect, useId, useState, type FormEvent } from "react";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { ToggleRow } from "@/components/toggle-row";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { canManageTeam } from "@/features/team/team-rules";
import { useSession } from "@/lib/auth/use-session";
import { centsToInput, parseCents } from "@/lib/format";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import type { OptionGroup } from "./options-api";
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
import { ProductOptionsEditor } from "./product-options-editor";
import { EMPTY_DRAFT, cheapestVariant, fromOptions, toRequest, type OptionsDraft } from "./product-options-draft";
import {
  OPTION_GROUPS_KEY,
  fetchOptionGroups,
  fetchProductOptions,
  updateProductOptions,
  type ProductOptions,
} from "./options-api";
import { ProductPhotos } from "./product-photos";

const t = messages.menu.product;
const DESCRIPTION_MAX = 300;

/** Editar produto (desenho 12.3). Sem productId, é o cadastro de um produto novo. */
/** Até quantas pessoas o prato pode servir (o back confere o mesmo teto). */
const SERVES_MAX = 20;
// O valor da opção "Não informar" (o campo de escolha não aceita valor vazio).
const NOT_INFORMED = "none";

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
  // Os tamanhos e os grupos do produto (ADR 0010) e a biblioteca de grupos para escolher.
  const options = useQuery({
    queryKey: ["product-options", productId],
    queryFn: () => fetchProductOptions(productId!),
    enabled: allowed && productId !== undefined,
    retry: false,
  });
  const library = useQuery({ queryKey: OPTION_GROUPS_KEY, queryFn: fetchOptionGroups, enabled: allowed });
  const [round, setRound] = useState(0);
  // O Next guarda as telas visitadas (escondidas, com o estado) para a volta ser instantânea. Ao
  // sair desta tela, os efeitos dela são desligados: é a hora de marcar uma visita nova, para o
  // formulário voltar em branco (e na categoria do endereço), e não com o rascunho abandonado.
  const [visit, setVisit] = useState(0);
  useEffect(() => () => setVisit((n) => n + 1), []);

  if (
    status !== "authenticated" ||
    (allowed && (categories.isPending || (productId && (product.isPending || options.isPending))))
  ) {
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

  // A chave faz o formulário recomeçar: a cada volta a esta tela; o novo, também a cada produto
  // criado e a cada categoria do endereço; o de edição, sempre que o produto chega de novo do back.
  return (
    <ProductForm
      key={
        product.data
          ? `${product.data.id}-${product.dataUpdatedAt}-${options.dataUpdatedAt}-${visit}`
          : `new-${initialCategoryId ?? ""}-${round}-${visit}`
      }
      product={product.data}
      options={options.data}
      library={library.data ?? []}
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
  options,
  library,
  categories,
  initialCategoryId,
  onSaved,
}: {
  product?: Product;
  options?: ProductOptions;
  library: OptionGroup[];
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
  const [aLaCarte, setALaCarte] = useState(product?.aLaCarte ?? true);
  // Quantas pessoas o prato serve: opcional ("" = não informar).
  const [serves, setServes] = useState(product?.serves != null ? String(product.serves) : "");
  // "Incluído no Rodízio X": uma chave por rodízio. Até a pessoa mexer, vale o que o cadastro diz.
  const rodizios = useQuery({ queryKey: ["rodizios"], queryFn: fetchRodizios });
  const [picked, setPicked] = useState<Set<string> | null>(null);
  const inRodizios =
    picked ??
    new Set((rodizios.data ?? []).filter((r) => product && r.productIds.includes(product.id)).map((r) => r.id));
  const [confirming, setConfirming] = useState(false);
  // Os tamanhos e as opções: só vão ao back se a pessoa mexeu (o rascunho começa do que está salvo).
  const [optionsDraft, setOptionsDraft] = useState<OptionsDraft>(() => (options ? fromOptions(options) : EMPTY_DRAFT));
  const [optionsTouched, setOptionsTouched] = useState(false);
  // Com tamanhos, o preço do produto deixa de valer: fica o menor preço dos tamanhos.
  const hasVariants = optionsDraft.variants.length > 0;
  // O Next mantém telas já visitadas escondidas na página: ids fixos se repetiriam, e um
  // rótulo apontaria para o campo da tela escondida. useId dá um id único a cada formulário.
  const id = useId();
  const formId = `${id}form`;

  const priceCents = hasVariants ? (cheapestVariant(optionsDraft) ?? 0) : parseCents(price);
  const promoCents = hasVariants ? null : parseCents(promo);
  const priceInvalid = price !== "" && Number.isNaN(priceCents);
  const promoInvalid = Number.isNaN(promoCents);
  const category = categories.find((c) => c.id === (product?.categoryId ?? categoryId));
  const menuHref = `/admin/cardapio${category ? `?categoria=${category.id}` : ""}`;

  async function finish(savedCategoryId: string) {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["menu-categories"] }),
      queryClient.invalidateQueries({ queryKey: ["products"] }),
      queryClient.invalidateQueries({ queryKey: ["product"] }),
      queryClient.invalidateQueries({ queryKey: ["product-options"] }),
      queryClient.invalidateQueries({ queryKey: OPTION_GROUPS_KEY }),
    ]);
    router.push(`/admin/cardapio?categoria=${savedCategoryId}`);
    onSaved();
  }

  const save = useMutation({
    mutationFn: async (body: ProductRequest) => {
      // Confere os tamanhos e as opções antes de salvar qualquer coisa (o erro sai na tela).
      const linkedOptions = new Set(
        library
          .filter((g) => optionsDraft.groups.some((l) => l.groupId === g.id))
          .flatMap((g) => g.items.map((i) => i.id)),
      );
      const optionsBody = optionsTouched ? toRequest(optionsDraft, linkedOptions) : null;
      const saved = await (product ? updateProduct(product.id, body) : createProduct(body));
      if (optionsBody) await updateProductOptions(saved.id, optionsBody);
      if (picked !== null) {
        await setProductRodizios(saved.id, [...picked]);
        await queryClient.invalidateQueries({ queryKey: ["rodizios"] });
      }
      return saved;
    },
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
      aLaCarte,
      // Sem a informação, o campo não vai: no PUT, isso apaga o que havia.
      ...(serves ? { serves: Number(serves) } : {}),
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

      <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
        <div className="flex min-w-0 flex-1 flex-col gap-6 lg:max-w-3xl">
          <form
            id={formId}
            onSubmit={onSubmit}
            className="flex flex-col gap-5 rounded-2xl border border-border bg-card p-5 sm:p-6"
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
                <Select
                  id={`${id}category`}
                  value={categoryId}
                  onValueChange={setCategoryId}
                  required
                  options={categories.map((c) => ({ value: c.id, label: c.name }))}
                />
              </div>
              <MoneyField
                id={`${id}price`}
                label={t.price}
                value={hasVariants ? centsToInput(priceCents ?? 0) : price}
                onChange={setPrice}
                onBlur={() => tidy(price, setPrice)}
                invalid={!hasVariants && priceInvalid}
                hint={hasVariants ? messages.options.product.priceLocked : t.priceHint}
                disabled={hasVariants}
                required
              />
              <MoneyField
                id={`${id}promo`}
                label={`${t.promoPrice} (${t.optional})`}
                value={hasVariants ? "" : promo}
                onChange={setPromo}
                onBlur={() => tidy(promo, setPromo)}
                invalid={promoInvalid}
                hint={t.promoHint}
                disabled={hasVariants}
              />
            </div>

            <div className="grid gap-5 sm:grid-cols-3">
              <div className="flex flex-col gap-2">
                <Label htmlFor={`${id}serves`} className="text-muted-foreground">
                  {`${t.serves} (${t.optional})`}
                </Label>
                <Select
                  id={`${id}serves`}
                  value={serves || NOT_INFORMED}
                  onValueChange={(value) => setServes(value === NOT_INFORMED ? "" : value)}
                  options={[
                    { value: NOT_INFORMED, label: t.servesNone },
                    ...Array.from({ length: SERVES_MAX }, (_, i) => ({
                      value: String(i + 1),
                      label: t.servesOption(i + 1),
                    })),
                  ]}
                />
                <span className="text-sm text-muted-foreground">{t.servesHint}</span>
              </div>
            </div>

            <div className="border-t border-border" />

            <ToggleRow title={t.availableTitle} text={t.availableText} checked={available} onChange={setAvailable} />
            <ToggleRow title={t.featuredTitle} text={t.featuredText} checked={featured} onChange={setFeatured} />
            <ToggleRow title={t.aLaCarteTitle} text={t.aLaCarteText} checked={aLaCarte} onChange={setALaCarte} />
            {(rodizios.data ?? []).map((rodizio) => (
              <ToggleRow
                key={rodizio.id}
                title={t.inRodizioTitle(rodizio.name)}
                text={rodizio.active ? t.inRodizioText : t.inRodizioPaused}
                checked={inRodizios.has(rodizio.id)}
                onChange={(on) => {
                  const next = new Set(inRodizios);
                  if (on) next.add(rodizio.id);
                  else next.delete(rodizio.id);
                  setPicked(next);
                }}
              />
            ))}

            <p role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
              {save.error?.message ?? ""}
            </p>
          </form>

          <ProductOptionsEditor
            draft={optionsDraft}
            library={library}
            onChange={(next) => {
              setOptionsDraft(next);
              setOptionsTouched(true);
            }}
          />
        </div>

        {/* As fotos precisam do produto já salvo: o caminho no armazenamento leva o id dele. */}
        <aside className="w-full lg:w-96 lg:shrink-0">
          {product ? (
            <ProductPhotos productId={product.id} productName={product.name} />
          ) : (
            <p className="rounded-2xl border border-dashed border-border p-5 text-sm text-muted-foreground">
              {messages.menu.photos.saveFirst}
            </p>
          )}
        </aside>
      </div>

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
  disabled,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  onBlur: () => void;
  invalid: boolean;
  hint: string;
  required?: boolean;
  disabled?: boolean;
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
          disabled={disabled}
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
