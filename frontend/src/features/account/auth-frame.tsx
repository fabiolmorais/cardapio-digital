import Image from "next/image";
import type { ReactNode } from "react";
import { BrandMark } from "@/features/landing/brand-mark";
import background from "../../../public/images/cadastro.jpg";

/**
 * A moldura das telas de acesso (entrar, esqueci a senha, senha nova): a foto do cadastro
 * desfocada no fundo e o cartão no meio, com o título e o texto de apoio.
 */
export function AuthFrame({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  return (
    <div className="relative flex min-h-dvh items-center justify-center overflow-hidden px-4 py-8">
      {/* Fundo decorativo: a foto do cadastro, desfocada. */}
      <Image
        src={background}
        alt=""
        fill
        priority
        sizes="100vw"
        className="scale-110 object-cover opacity-30 blur-3xl saturate-150"
      />
      <main className="relative flex w-full max-w-[440px] flex-col gap-6 rounded-[28px] border border-border bg-card/80 px-6 py-9 shadow-[0_30px_80px_rgba(0,0,0,.5)] backdrop-blur-xl sm:px-8">
        <div className="self-start">
          <BrandMark />
        </div>
        <div className="flex flex-col gap-1.5">
          <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
          <p className="text-muted-foreground">{subtitle}</p>
        </div>
        {children}
      </main>
    </div>
  );
}
