"use client";

import { useMutation } from "@tanstack/react-query";
import Link from "next/link";
import type { FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { messages } from "@/messages/pt-BR";
import { requestReset } from "./password-reset-api";

const t = messages.passwordReset;

/** Pede o link por e-mail. A resposta é a mesma com ou sem conta: a tela não diz qual foi o caso. */
export function ForgotPasswordForm() {
  const mutation = useMutation({ mutationFn: requestReset });

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    mutation.mutate(String(new FormData(event.currentTarget).get("email") ?? "").trim());
  }

  return (
    <form onSubmit={onSubmit} className="flex flex-col gap-4" aria-describedby="forgot-status">
      <div className="flex flex-col gap-2">
        <Label htmlFor="email" className="text-muted-foreground">
          {t.email}
        </Label>
        <Input
          id="email"
          name="email"
          type="email"
          inputMode="email"
          autoComplete="username"
          placeholder={messages.login.placeholders.email}
          required
          maxLength={254}
          className="h-12 rounded-xl px-4 text-base"
        />
      </div>

      {mutation.isSuccess ? (
        <p id="forgot-status" role="status" className="rounded-xl bg-primary/10 px-4 py-3 text-sm">
          {t.sent}
        </p>
      ) : (
        <p id="forgot-status" role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
          {mutation.error?.message ?? ""}
        </p>
      )}

      <Button type="submit" size="xl" disabled={mutation.isPending}>
        {mutation.isPending ? t.sending : mutation.isSuccess ? t.sendAgain : t.send}
      </Button>

      <Link
        href="/entrar"
        className="self-center text-sm font-medium text-muted-foreground underline-offset-4 hover:text-foreground hover:underline"
      >
        {t.backToLogin}
      </Link>
    </form>
  );
}
