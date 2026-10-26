import { UtensilsCrossed } from "lucide-react";
import type { Metadata } from "next";
import Image from "next/image";
import { LoginForm } from "@/features/account/login-form";
import { messages } from "@/messages/pt-BR";
import background from "../../../../public/images/cadastro.jpg";

const t = messages.login;

export const metadata: Metadata = {
  title: `${t.title} · ${messages.app.name}`,
};

export default function LoginPage() {
  return (
    <div className="relative flex min-h-dvh items-center justify-center overflow-hidden px-4 py-8">
      {/* Fundo decorativo: a foto do cadastro, desfocada. */}
      <Image
        src={background}
        alt={t.backgroundAlt}
        fill
        priority
        sizes="100vw"
        className="scale-110 object-cover opacity-30 blur-3xl saturate-150"
      />
      <main className="relative flex w-full max-w-[440px] flex-col gap-6 rounded-[28px] border border-border bg-card/80 px-6 py-9 shadow-[0_30px_80px_rgba(0,0,0,.5)] backdrop-blur-xl sm:px-8">
        <span className="flex size-12 items-center justify-center rounded-[14px] bg-primary text-primary-foreground">
          <UtensilsCrossed aria-hidden className="size-6" />
        </span>
        <div className="flex flex-col gap-1.5">
          <h1 className="text-2xl font-bold tracking-tight">{t.title}</h1>
          <p className="text-muted-foreground">{t.subtitle}</p>
        </div>
        <LoginForm />
      </main>
    </div>
  );
}
