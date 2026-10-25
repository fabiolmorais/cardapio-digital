"use client";

import { useMutation } from "@tanstack/react-query";
import { CheckCircle2 } from "lucide-react";
import Link from "next/link";
import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, type Schemas } from "@/lib/api/client";
import { messages } from "@/messages/pt-BR";

const t = messages.signup;

type SignupRequest = Schemas["SignupRequest"];
type FieldName = keyof typeof t.fields;

type SignupFailure = { message: string; fields: string[] };

const FIELDS: { name: FieldName; type: string; autoComplete: string; inputMode?: "tel" | "email" | "numeric" }[] = [
  { name: "establishmentName", type: "text", autoComplete: "organization" },
  { name: "ownerName", type: "text", autoComplete: "name" },
  { name: "whatsapp", type: "tel", autoComplete: "tel-national", inputMode: "tel" },
  { name: "document", type: "text", autoComplete: "off" },
  { name: "email", type: "email", autoComplete: "email", inputMode: "email" },
  { name: "password", type: "password", autoComplete: "new-password" },
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
  const mutation = useMutation<Schemas["SignupResponse"], SignupFailure, SignupRequest>({ mutationFn: signup });

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!acceptedTerms) {
      setTermsError(true);
      return;
    }
    const form = new FormData(event.currentTarget);
    const value = (name: FieldName) => String(form.get(name) ?? "");
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
          <div
            key={field.name}
            className={
              field.name === "whatsapp" || field.name === "document"
                ? "flex flex-col gap-2"
                : "flex flex-col gap-2 sm:col-span-2"
            }
          >
            <Label htmlFor={field.name} className="text-muted-foreground">
              {t.fields[field.name]}
            </Label>
            <Input
              id={field.name}
              name={field.name}
              type={field.type}
              inputMode={field.inputMode}
              autoComplete={field.autoComplete}
              placeholder={t.placeholders[field.name]}
              required
              minLength={field.name === "password" ? 10 : undefined}
              maxLength={field.name === "password" ? 72 : 254}
              aria-invalid={invalid.has(field.name) || undefined}
              aria-describedby={field.name === "password" ? "password-hint" : undefined}
              className="h-12 rounded-xl px-4 text-base"
            />
            {field.name === "password" && (
              <p id="password-hint" className="text-xs text-muted-foreground">
                {t.passwordHint}
              </p>
            )}
          </div>
        ))}
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
        {termsError ? t.errors.terms : (mutation.error?.message ?? "")}
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
