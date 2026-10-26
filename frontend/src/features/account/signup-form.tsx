"use client";

import { useMutation } from "@tanstack/react-query";
import { CheckCircle2 } from "lucide-react";
import Link from "next/link";
import { useState, type ChangeEvent, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, type Schemas } from "@/lib/api/client";
import { DOCUMENT_MASKED_LENGTH, formatDocument, formatPhone, PHONE_MASKED_LENGTH } from "@/lib/masks";
import { messages } from "@/messages/pt-BR";

const t = messages.signup;

type SignupRequest = Schemas["SignupRequest"];
type FieldName = keyof typeof t.fields;

type SignupFailure = { message: string; fields: string[] };

type Field = {
  name: FieldName;
  type: string;
  autoComplete: string;
  maxLength: number;
  inputMode?: "tel" | "email";
  /** Meia largura no computador (dois campos por linha). */
  half?: boolean;
  /** Máscara aplicada enquanto a pessoa digita. */
  format?: (value: string) => string;
};

const FIELDS: Field[] = [
  { name: "establishmentName", type: "text", autoComplete: "organization", maxLength: 120 },
  { name: "ownerName", type: "text", autoComplete: "name", maxLength: 120 },
  {
    name: "whatsapp",
    type: "tel",
    autoComplete: "tel-national",
    inputMode: "tel",
    maxLength: PHONE_MASKED_LENGTH,
    half: true,
    format: formatPhone,
  },
  {
    name: "document",
    type: "text",
    autoComplete: "off",
    maxLength: DOCUMENT_MASKED_LENGTH,
    half: true,
    format: formatDocument,
  },
  { name: "email", type: "email", autoComplete: "email", inputMode: "email", maxLength: 254 },
  { name: "password", type: "password", autoComplete: "new-password", maxLength: 72, half: true },
  { name: "confirmPassword", type: "password", autoComplete: "new-password", maxLength: 72, half: true },
];

async function signup(body: SignupRequest): Promise<Schemas["SignupResponse"]> {
  const { data, error, response } = await api.POST("/api/v1/signup", { body }).catch(() => ({
    data: undefined,
    error: undefined,
    response: undefined,
  }));
  if (data) return data;
  const problem = error as { detail?: string; fields?: string[] } | undefined;
  const failure: SignupFailure = {
    message: problem?.detail && response?.status !== 500 ? problem.detail : t.errors.generic,
    fields: problem?.fields ?? (response?.status === 409 ? ["email"] : []),
  };
  if (problem?.fields?.length) failure.message = t.errors.fields;
  throw failure;
}

export function SignupForm() {
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [termsError, setTermsError] = useState(false);
  const [passwordMismatch, setPasswordMismatch] = useState(false);
  const [masked, setMasked] = useState<Partial<Record<FieldName, string>>>({});
  const mutation = useMutation<Schemas["SignupResponse"], SignupFailure, SignupRequest>({ mutationFn: signup });

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const value = (name: FieldName) => String(form.get(name) ?? "");
    if (value("password") !== value("confirmPassword")) {
      setPasswordMismatch(true);
      return;
    }
    if (!acceptedTerms) {
      setTermsError(true);
      return;
    }
    mutation.mutate({
      establishmentName: value("establishmentName"),
      ownerName: value("ownerName"),
      whatsapp: value("whatsapp"),
      document: value("document"),
      email: value("email"),
      password: value("password"),
      acceptedTerms,
    });
  }

  if (mutation.isSuccess) {
    return (
      <section role="status" className="flex flex-col items-start gap-5">
        <span className="flex size-16 items-center justify-center rounded-full bg-success/15 text-success">
          <CheckCircle2 aria-hidden className="size-8" />
        </span>
        <div className="flex flex-col gap-2">
          <h2 className="text-3xl font-bold tracking-tight">{t.success.title}</h2>
          <p className="text-muted-foreground">{t.success.text}</p>
          <p className="text-sm text-muted-foreground">{t.success.link(mutation.data.slug)}</p>
        </div>
        <Link
          href="/entrar"
          className="inline-flex h-12 items-center rounded-xl bg-primary px-5 text-[15px] font-semibold text-primary-foreground hover:bg-primary/80"
        >
          {t.success.cta}
        </Link>
      </section>
    );
  }

  const invalid = new Set(mutation.error?.fields ?? []);

  return (
    <form onSubmit={onSubmit} className="flex flex-col gap-4" aria-describedby="signup-error">
      <div className="grid gap-4 sm:grid-cols-2">
        {FIELDS.map((field) => (
          <div key={field.name} className={field.half ? "flex flex-col gap-2" : "flex flex-col gap-2 sm:col-span-2"}>
            <Label htmlFor={field.name} className="text-muted-foreground">
              {t.fields[field.name]}
            </Label>
            <Input
              id={field.name}
              name={field.name}
              type={field.type}
              inputMode={field.inputMode}
              autoComplete={field.autoComplete}
              autoCapitalize={field.name === "document" ? "characters" : undefined}
              placeholder={t.placeholders[field.name]}
              required
              minLength={field.type === "password" ? 10 : undefined}
              maxLength={field.maxLength}
              {...(field.format && {
                value: masked[field.name] ?? "",
                onChange: (e: ChangeEvent<HTMLInputElement>) =>
                  setMasked((current) => ({ ...current, [field.name]: field.format!(e.target.value) })),
              })}
              {...(field.name === "confirmPassword" && { onChange: () => setPasswordMismatch(false) })}
              aria-invalid={
                invalid.has(field.name) || (field.name === "confirmPassword" && passwordMismatch) || undefined
              }
              aria-describedby={field.type === "password" ? "password-hint" : undefined}
              className="h-12 rounded-xl px-4 text-base"
            />
          </div>
        ))}
        <p id="password-hint" className="text-xs text-muted-foreground sm:col-span-2">
          {t.passwordHint}
        </p>
      </div>

      <div className="flex items-start gap-3 py-1">
        <Checkbox
          id="acceptedTerms"
          checked={acceptedTerms}
          onCheckedChange={(checked) => {
            setAcceptedTerms(checked === true);
            setTermsError(false);
          }}
          aria-invalid={termsError || invalid.has("acceptedTerms") || undefined}
          className="mt-0.5 size-5"
        />
        <Label htmlFor="acceptedTerms" className="block text-sm leading-5 font-normal text-muted-foreground">
          {t.termsBefore}
          <a href="/termos" className="text-brand-text underline-offset-4 hover:underline">
            {t.terms}
          </a>
          {t.termsMiddle}
          <a href="/privacidade" className="text-brand-text underline-offset-4 hover:underline">
            {t.privacy}
          </a>
          .
        </Label>
      </div>

      <p id="signup-error" role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
        {passwordMismatch ? t.errors.passwordMismatch : termsError ? t.errors.terms : (mutation.error?.message ?? "")}
      </p>

      <Button type="submit" size="xl" disabled={mutation.isPending}>
        {mutation.isPending ? t.submitting : t.submit}
      </Button>

      <p className="text-center text-sm text-muted-foreground">
        {t.haveAccount}{" "}
        <Link href="/entrar" className="font-semibold text-brand-text underline-offset-4 hover:underline">
          {t.login}
        </Link>
      </p>
    </form>
  );
}
