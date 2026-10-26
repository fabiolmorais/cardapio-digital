"use client";

import { useMutation } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, type Schemas } from "@/lib/api/client";
import { setSession } from "@/lib/auth/session";
import { messages } from "@/messages/pt-BR";

const t = messages.login;

type LoginRequest = Schemas["LoginRequest"];
type LoginResponse = Schemas["LoginResponse"];

async function login(body: LoginRequest): Promise<LoginResponse> {
  const { data, error, response } = await api.POST("/api/v1/auth/login", { body }).catch(() => ({
    data: undefined,
    error: undefined,
    response: undefined,
  }));
  if (data) return data;
  const problem = error as { detail?: string } | undefined;
  // 401 traz a mensagem do back ("E-mail ou senha incorretos"); o resto vira a mensagem genérica.
  if (response?.status === 401 && problem?.detail) throw new Error(problem.detail);
  if (response?.status === 400) throw new Error(t.errors.fields);
  throw new Error(t.errors.generic);
}

export function LoginForm() {
  const router = useRouter();
  const formRef = useRef<HTMLFormElement>(null);
  const mutation = useMutation<LoginResponse, Error, LoginRequest>({
    mutationFn: login,
    onSuccess: (data) => {
      setSession({
        accessToken: data.accessToken,
        expiresAt: data.expiresAt,
        userName: data.userName,
        establishment: data.establishment,
      });
      router.push("/painel");
    },
  });

  // O Next guarda páginas já visitadas com o estado delas. Toda vez que a tela de entrar
  // volta a aparecer, começa limpa: sem "Entrando…", sem erro antigo e sem a senha de antes.
  const { reset } = mutation;
  useEffect(() => {
    reset();
    formRef.current?.reset();
  }, [reset]);

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    mutation.mutate({
      email: String(form.get("email") ?? "").trim(),
      password: String(form.get("password") ?? ""),
    });
  }

  const failed = mutation.isError;

  return (
    <form ref={formRef} onSubmit={onSubmit} className="flex flex-col gap-4" aria-describedby="login-error">
      <div className="flex flex-col gap-2">
        <Label htmlFor="email" className="text-muted-foreground">
          {t.fields.email}
        </Label>
        <Input
          id="email"
          name="email"
          type="email"
          inputMode="email"
          autoComplete="username"
          placeholder={t.placeholders.email}
          required
          maxLength={254}
          aria-invalid={failed || undefined}
          className="h-12 rounded-xl px-4 text-base"
        />
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor="password" className="text-muted-foreground">
          {t.fields.password}
        </Label>
        <Input
          id="password"
          name="password"
          type="password"
          autoComplete="current-password"
          placeholder={t.placeholders.password}
          required
          maxLength={72}
          aria-invalid={failed || undefined}
          className="h-12 rounded-xl px-4 text-base"
        />
      </div>

      <p id="login-error" role="alert" aria-live="assertive" className="min-h-5 text-sm text-destructive">
        {mutation.error?.message ?? ""}
      </p>

      <Button type="submit" size="xl" disabled={mutation.isPending || mutation.isSuccess}>
        {mutation.isPending || mutation.isSuccess ? t.submitting : t.submit}
      </Button>

      <div aria-hidden className="h-px bg-border" />

      <p className="text-center text-sm text-muted-foreground">
        {t.noAccount}{" "}
        <Link href="/cadastro" className="font-semibold text-brand-text underline-offset-4 hover:underline">
          {t.signup}
        </Link>
      </p>
    </form>
  );
}
