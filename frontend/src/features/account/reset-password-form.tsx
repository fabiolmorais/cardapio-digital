"use client";

import { useMutation } from "@tanstack/react-query";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Button, buttonVariants } from "@/components/ui/button";
import { PasswordInput } from "@/components/password-input";
import { Label } from "@/components/ui/label";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import { confirmReset, PasswordResetError } from "./password-reset-api";

const t = messages.passwordReset;

/** A senha nova, com o token que veio no link do e-mail (?token=…). */
export function ResetPasswordForm() {
  const token = useSearchParams().get("token") ?? "";
  const [mismatch, setMismatch] = useState(false);
  const mutation = useMutation({ mutationFn: (password: string) => confirmReset(token, password) });

  if (!token) {
    return <Expired text={t.noToken} />;
  }
  if (mutation.isSuccess) {
    return (
      <div className="flex flex-col gap-4">
        <p role="status" className="rounded-xl bg-primary/10 px-4 py-3 text-sm">
          {t.done}
        </p>
        <Link href="/entrar" className={cn(buttonVariants({ size: "xl" }))}>
          {t.goToLogin}
        </Link>
      </div>
    );
  }

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const password = String(form.get("password") ?? "");
    const same = password === String(form.get("confirm") ?? "");
    setMismatch(!same);
    if (same) mutation.mutate(password);
  }

  const error = mismatch ? t.mismatch : (mutation.error?.message ?? "");
  // O 422 é o link vencido ou já usado: o caminho é pedir outro, não tentar de novo.
  const linkGone = !mismatch && mutation.error instanceof PasswordResetError && mutation.error.status === 422;

  return (
    <form onSubmit={onSubmit} className="flex flex-col gap-4" aria-describedby="reset-error">
      <div className="flex flex-col gap-2">
        <Label htmlFor="password" className="text-muted-foreground">
          {t.newPassword}
        </Label>
        <PasswordInput
          id="password"
          name="password"
          autoComplete="new-password"
          required
          minLength={10}
          maxLength={72}
          className="h-12 rounded-xl px-4 text-base"
        />
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor="confirm" className="text-muted-foreground">
          {t.confirmPassword}
        </Label>
        <PasswordInput
          id="confirm"
          name="confirm"
          autoComplete="new-password"
          required
          minLength={10}
          maxLength={72}
          aria-invalid={mismatch || undefined}
          className="h-12 rounded-xl px-4 text-base"
        />
      </div>

      <p id="reset-error" role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
        {error}
      </p>
      {linkGone ? (
        <Link href="/esqueci-a-senha" className={cn(buttonVariants({ size: "xl" }))}>
          {t.askAnother}
        </Link>
      ) : (
        <Button type="submit" size="xl" disabled={mutation.isPending}>
          {mutation.isPending ? t.saving : t.save}
        </Button>
      )}
    </form>
  );
}

function Expired({ text }: { text: string }) {
  return (
    <div className="flex flex-col gap-4">
      <p role="alert" className="text-sm text-destructive">
        {text}
      </p>
      <Link href="/esqueci-a-senha" className={cn(buttonVariants({ size: "xl" }))}>
        {t.askAnother}
      </Link>
    </div>
  );
}
