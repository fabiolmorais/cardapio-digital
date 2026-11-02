"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useId, useState, type ChangeEvent, type CSSProperties, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { canManageTeam } from "@/features/team/team-rules";
import { refreshSession } from "@/lib/auth/session";
import { useSession } from "@/lib/auth/use-session";
import { initials } from "@/lib/initials";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { fetchBrand, removeLogo, updateBrand, uploadLogo, type Brand, type BrandRequest } from "./brand-api";

const t = messages.brand;
/** As mesmas regras do back (LogoFile), só para avisar antes de enviar. */
const LOGO_TYPES = ["image/png", "image/jpeg"];
const LOGO_MAX_BYTES = 2 * 1024 * 1024;

/** Marca e aparência (desenho 12.7): o formulário à esquerda e a prévia ao vivo no celular à direita. */
export function BrandEditor() {
  const { status, session } = useSession();
  const allowed = status === "authenticated" && canManageTeam(session.establishment.role);
  const brand = useQuery({ queryKey: ["brand"], queryFn: fetchBrand, enabled: allowed });

  if (status !== "authenticated" || (allowed && brand.isPending)) {
    return <p className="px-4 py-6 text-muted-foreground sm:px-7">{t.loading}</p>;
  }
  if (!allowed) {
    return (
      <p role="alert" className="px-4 py-6 text-muted-foreground sm:px-7">
        {messages.team.noPermission}
      </p>
    );
  }
  if (brand.isError || !brand.data) {
    return (
      <p role="alert" className="px-4 py-6 text-destructive sm:px-7">
        {t.loadError}
      </p>
    );
  }
  return <BrandForm brand={brand.data} />;
}

/** O formulário nasce do que o back tem; depois de salvar, a resposta do back atualiza o cache. */
function BrandForm({ brand }: { brand: Brand }) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<BrandRequest>({
    name: brand.name,
    color: brand.color,
    theme: brand.theme,
    instagram: brand.instagram ?? "",
    googleReviewUrl: brand.googleReviewUrl ?? "",
  });
  const [notice, setNotice] = useState<string | null>(null);
  const id = useId();

  const set = <K extends keyof BrandRequest>(key: K, value: BrandRequest[K]) => {
    setNotice(null);
    setForm((current) => ({ ...current, [key]: value }));
  };

  /** A resposta do back vira o cache: a tela não precisa buscar de novo. */
  function keep(saved: Brand) {
    queryClient.setQueryData(["brand"], saved);
  }

  const save = useMutation({
    mutationFn: () => updateBrand(form),
    onSuccess: async (saved) => {
      // A sessão traz o nome do restaurante (o menu lateral mostra): renova para ele mudar já.
      await refreshSession();
      keep(saved);
      setNotice(t.saved);
    },
  });

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    save.mutate();
  }

  const palette = brand.palette;
  const selected = palette.find((c) => c.color === form.color) ?? palette[0];

  return (
    <div className="flex flex-col gap-6 px-4 py-6 sm:px-7">
      <header className="flex flex-wrap items-end gap-4">
        <div className="mr-auto flex flex-col gap-1">
          <h1 className="text-2xl font-bold tracking-tight">{t.title}</h1>
          <p className="text-sm text-muted-foreground">{t.subtitle}</p>
        </div>
        <Button type="submit" form={`${id}form`} size="xl" disabled={save.isPending}>
          {save.isPending ? t.saving : t.save}
        </Button>
      </header>

      <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
        <div className="flex min-w-0 flex-1 flex-col gap-5 rounded-2xl border border-border bg-card p-5 sm:p-6 lg:max-w-3xl">
          <LogoField brand={brand} name={form.name} color={selected.primary} onChanged={keep} />

          <form id={`${id}form`} onSubmit={onSubmit} className="flex flex-col gap-5">
            <div className="flex flex-col gap-2">
              <Label htmlFor={`${id}name`} className="text-muted-foreground">
                {t.name}
              </Label>
              <Input
                id={`${id}name`}
                required
                maxLength={120}
                autoComplete="off"
                value={form.name}
                onChange={(event) => set("name", event.target.value)}
                className="h-12 rounded-xl px-4 text-base"
              />
            </div>

            <fieldset className="flex flex-col gap-2">
              <legend className="mb-2 text-sm font-medium text-muted-foreground">{t.color}</legend>
              <div className="flex flex-wrap gap-2.5">
                {palette.map((c) => (
                  <label
                    key={c.color}
                    title={c.label}
                    className="relative flex size-12 cursor-pointer rounded-xl ring-offset-2 ring-offset-card has-[:checked]:ring-2 has-[:checked]:ring-foreground has-[:focus-visible]:ring-3 has-[:focus-visible]:ring-ring"
                    style={{ backgroundColor: c.primary }}
                  >
                    <input
                      type="radio"
                      name={`${id}color`}
                      value={c.color}
                      checked={form.color === c.color}
                      onChange={() => set("color", c.color)}
                      className="absolute inset-0 cursor-pointer appearance-none rounded-xl"
                    />
                    <span className="sr-only">{c.label}</span>
                  </label>
                ))}
              </div>
              <span className="text-sm text-muted-foreground">
                {selected.label} · {t.colorHint}
              </span>
            </fieldset>

            <fieldset className="flex flex-col gap-2">
              <legend className="mb-2 text-sm font-medium text-muted-foreground">{t.theme}</legend>
              <div className="grid grid-cols-2 gap-2.5 sm:max-w-sm">
                {(["DARK", "LIGHT"] as const).map((theme) => (
                  <label
                    key={theme}
                    className="relative flex cursor-pointer flex-col items-center gap-2 rounded-xl border border-border p-3 text-sm font-semibold has-[:checked]:border-primary has-[:checked]:bg-primary/10 has-[:focus-visible]:ring-3 has-[:focus-visible]:ring-ring/50"
                  >
                    <input
                      type="radio"
                      name={`${id}theme`}
                      value={theme}
                      checked={form.theme === theme}
                      onChange={() => set("theme", theme)}
                      className="absolute inset-0 cursor-pointer appearance-none rounded-xl"
                    />
                    <span
                      aria-hidden
                      className={cn(
                        "h-10 w-full rounded-lg border border-border",
                        theme === "DARK" ? "bg-[#0F0D0C]" : "bg-[#F7F4F0]",
                      )}
                    />
                    {t.themes[theme]}
                  </label>
                ))}
              </div>
            </fieldset>

            <div className="grid gap-5 sm:grid-cols-2">
              <div className="flex flex-col gap-2">
                <Label htmlFor={`${id}instagram`} className="text-muted-foreground">
                  {t.instagram}
                </Label>
                <div className="relative">
                  <span
                    aria-hidden
                    className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 font-semibold text-muted-foreground"
                  >
                    @
                  </span>
                  <Input
                    id={`${id}instagram`}
                    maxLength={100}
                    autoComplete="off"
                    value={form.instagram ?? ""}
                    onChange={(event) => set("instagram", event.target.value)}
                    placeholder={t.instagramPlaceholder}
                    className="h-12 rounded-xl pr-4 pl-9 text-base"
                  />
                </div>
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor={`${id}review`} className="text-muted-foreground">
                  {t.review}
                </Label>
                <Input
                  id={`${id}review`}
                  type="url"
                  maxLength={300}
                  autoComplete="off"
                  value={form.googleReviewUrl ?? ""}
                  onChange={(event) => set("googleReviewUrl", event.target.value)}
                  placeholder={t.reviewPlaceholder}
                  aria-describedby={`${id}review-hint`}
                  className="h-12 rounded-xl px-4 text-base"
                />
                <span id={`${id}review-hint`} className="text-sm text-muted-foreground">
                  {t.reviewHint}
                </span>
              </div>
            </div>

            <p role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
              {save.error?.message ?? ""}
            </p>
            {notice && (
              <p role="status" className="-mt-4 text-sm text-success">
                {notice}
              </p>
            )}
          </form>
        </div>

        <aside className="flex w-full flex-col items-center gap-3 lg:w-auto lg:shrink-0">
          <span className="text-sm text-muted-foreground">{t.preview.title}</span>
          <PhonePreview
            name={form.name}
            theme={form.theme}
            primary={selected.primary}
            accent={form.theme === "DARK" ? selected.textOnDark : selected.textOnLight}
            logo={brand.logo?.small}
          />
        </aside>
      </div>
    </div>
  );
}

/** A logo (ou as iniciais, na cor da marca) com os botões de enviar e de voltar às iniciais. */
function LogoField({
  brand,
  name,
  color,
  onChanged,
}: {
  brand: Brand;
  name: string;
  color: string;
  onChanged: (brand: Brand) => void;
}) {
  const [percent, setPercent] = useState<number | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const inputId = useId();

  const send = useMutation({
    mutationFn: async (file: File) => {
      if (!LOGO_TYPES.includes(file.type)) throw new Error(t.logo.wrongType);
      if (file.size > LOGO_MAX_BYTES) throw new Error(t.logo.tooBig);
      setPercent(0);
      // Enviado o arquivo (100%), o back ainda confere e gera os tamanhos: "preparando…".
      return uploadLogo(file, (fraction) => setPercent(fraction < 1 ? Math.round(fraction * 100) : null));
    },
    onSuccess: (saved) => {
      onChanged(saved);
      setNotice(t.logo.sent);
    },
    onSettled: () => setPercent(null),
  });
  const remove = useMutation({
    mutationFn: removeLogo,
    onSuccess: (saved) => {
      onChanged(saved);
      setNotice(t.logo.removed);
    },
  });

  function onPick(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = ""; // a mesma imagem pode ser escolhida de novo
    if (!file) return;
    setNotice(null);
    remove.reset();
    send.mutate(file);
  }

  const busy = send.isPending || remove.isPending;
  const error = send.error ?? remove.error;

  return (
    <section aria-labelledby={`${inputId}title`} className="flex flex-wrap items-center gap-4">
      <LogoBadge logo={brand.logo?.large} name={name} color={color} className="size-18 text-2xl" />
      <div className="flex min-w-0 flex-col gap-2">
        <h2 id={`${inputId}title`} className="text-sm font-medium text-muted-foreground">
          {t.logo.title}
        </h2>
        <div className="flex flex-wrap gap-2">
          <input
            id={inputId}
            type="file"
            accept={LOGO_TYPES.join(",")}
            disabled={busy}
            onChange={onPick}
            className="peer sr-only"
          />
          <label
            htmlFor={inputId}
            className={cn(
              "inline-flex h-10 cursor-pointer items-center rounded-lg border border-border bg-background px-3 text-sm font-medium hover:bg-muted peer-focus-visible:ring-3 peer-focus-visible:ring-ring/50 dark:bg-input/30",
              busy && "pointer-events-none opacity-60",
            )}
          >
            {brand.logo ? t.logo.replace : t.logo.send}
          </label>
          {brand.logo && (
            <Button variant="ghost" className="h-10 px-3" disabled={busy} onClick={() => remove.mutate()}>
              {t.logo.useInitials}
            </Button>
          )}
        </div>
        <span className="text-sm text-muted-foreground">{t.logo.hint}</span>
        {send.isPending && (
          <p role="status" className="text-sm text-muted-foreground">
            {percent === null ? t.logo.processing : t.logo.sending(percent)}
          </p>
        )}
        {notice && !busy && (
          <p role="status" className="text-sm text-success">
            {notice}
          </p>
        )}
        {error && (
          <p role="alert" className="text-sm text-destructive">
            {error.message}
          </p>
        )}
      </div>
    </section>
  );
}

/** decorative: dentro da prévia, que já tem o próprio rótulo, a logo não é anunciada de novo. */
function LogoBadge({
  logo,
  name,
  color,
  className,
  decorative = false,
}: {
  logo?: string;
  name: string;
  color: string;
  className: string;
  decorative?: boolean;
}) {
  if (logo) {
    return (
      // eslint-disable-next-line @next/next/no-img-element -- logo já no tamanho certo, vinda do R2
      <img
        src={logo}
        alt={decorative ? "" : t.logo.alt(name)}
        className={cn("shrink-0 rounded-full object-contain", className)}
      />
    );
  }
  return (
    <span
      aria-hidden
      className={cn("flex shrink-0 items-center justify-center rounded-full font-bold text-white", className)}
      style={{ backgroundColor: color }}
    >
      {initials(name)}
    </span>
  );
}

/**
 * Um esboço da página da mesa (Etapa 5) com a marca escolhida. As cores vêm da paleta do back:
 * aqui só entram como variáveis CSS, porque o Tailwind não gera classes para cores que só se
 * conhecem na hora.
 */
function PhonePreview({
  name,
  theme,
  primary,
  accent,
  logo,
}: {
  name: string;
  theme: "DARK" | "LIGHT";
  primary: string;
  accent: string;
  logo?: string;
}) {
  const dark = theme === "DARK";
  const colors = {
    "--phone-bg": dark ? "#0F0D0C" : "#F7F4F0",
    "--phone-fg": dark ? "#F8F5F1" : "#16130F",
    "--phone-muted": dark ? "#BDB5AC" : "#5C554E",
    "--phone-glass": dark ? "rgba(255,255,255,.08)" : "rgba(255,255,255,.85)",
    "--phone-stroke": dark ? "rgba(255,255,255,.12)" : "rgba(22,19,15,.09)",
    "--brand": primary,
    "--brand-text": accent,
  } as CSSProperties;

  return (
    <div
      role="img"
      aria-label={`${t.preview.title}: ${name}`}
      style={colors}
      className="h-[600px] w-[300px] overflow-hidden rounded-[40px] border-8 border-[#2A2521] bg-(--phone-bg) text-(--phone-fg) shadow-2xl"
    >
      <div className="flex flex-col gap-4 px-4 py-5">
        <div className="flex items-center gap-2.5">
          <LogoBadge logo={logo} name={name} color={primary} className="size-9 text-xs" decorative />
          <span className="flex min-w-0 flex-col">
            <span className="truncate text-sm font-semibold">{name || " "}</span>
            <span className="text-[11px] text-(--phone-muted)">{t.preview.table}</span>
          </span>
        </div>
        <span className="text-[26px] leading-[29px] font-bold tracking-tight">
          {t.preview.headline}
          <br />
          <span className="text-(--brand-text)">{t.preview.highlight}</span>
        </span>
        <div className="grid grid-cols-2 gap-2">
          <span className="flex h-24 items-end rounded-[18px] bg-(--brand) p-3 text-[13px] font-semibold text-white">
            {t.preview.callWaiter}
          </span>
          <span className="flex h-24 items-end rounded-[18px] border border-(--phone-stroke) bg-(--phone-glass) p-3 text-[13px] font-semibold">
            {t.preview.askBill}
          </span>
        </div>
        <span className="flex h-12 items-center justify-center rounded-[14px] border border-(--phone-stroke) bg-(--phone-glass) text-sm font-semibold">
          {t.preview.menu}
        </span>
      </div>
    </div>
  );
}
