import { Check, UtensilsCrossed } from "lucide-react";
import type { Metadata } from "next";
import Image from "next/image";
import { SignupForm } from "@/features/account/signup-form";
import { messages } from "@/messages/pt-BR";
import cover from "../../../../public/images/cadastro.jpg";

const t = messages.signup;

export const metadata: Metadata = {
  title: `${t.title} · ${messages.app.name}`,
};

export default function SignupPage() {
  return (
    <div className="flex min-h-dvh flex-wrap">
      <main className="flex flex-[1_1_480px] flex-col justify-center gap-8 px-4 py-12 sm:px-12 lg:px-24">
        <div className="flex items-center gap-3">
          <span className="flex size-10 items-center justify-center rounded-xl bg-primary text-primary-foreground">
            <UtensilsCrossed aria-hidden className="size-5" />
          </span>
          <span className="font-semibold">{messages.app.name}</span>
        </div>
        <div className="flex max-w-xl flex-col gap-2">
          <h1 className="text-4xl font-bold tracking-tight">{t.title}</h1>
          <p className="text-muted-foreground">{t.subtitle}</p>
        </div>
        <div className="max-w-xl">
          <SignupForm />
        </div>
      </main>

      <aside className="relative hidden min-h-[420px] flex-[1_1_420px] overflow-hidden lg:block" aria-label={t.aside.headline}>
        <Image src={cover} alt={t.aside.imageAlt} fill priority sizes="50vw" className="object-cover" />
        <div aria-hidden className="absolute inset-0 bg-gradient-to-b from-background/20 to-background/95" />
        <div className="absolute inset-x-12 bottom-14 flex flex-col gap-5">
          <p className="max-w-md text-3xl leading-tight font-bold">{t.aside.headline}</p>
          <ul className="flex flex-col gap-3">
            {t.aside.perks.map((perk) => (
              <li key={perk} className="flex items-start gap-3">
                <span className="mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-full bg-success/15 text-success">
                  <Check aria-hidden className="size-4" />
                </span>
                <span>{perk}</span>
              </li>
            ))}
          </ul>
        </div>
      </aside>
    </div>
  );
}
