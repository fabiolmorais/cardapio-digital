import { Bell, Check, ChefHat, ChevronDown, Flame, ShieldCheck, Tv, Video } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import type { ReactNode } from "react";
import { buttonVariants } from "@/components/ui/button";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import photo from "../../../public/images/cadastro.jpg";
import { BrandMark } from "./brand-mark";

const t = messages.home;
const wrap = "mx-auto w-full max-w-[1200px] px-6";

/** O topo: logo, links das seções e os botões de entrar e de testar. */
export function LandingHeader() {
  return (
    <header className={cn(wrap, "flex flex-wrap items-center gap-x-3 gap-y-4 py-5 sm:gap-x-7")}>
      <BrandMark />
      <nav aria-label={t.nav.label} className="mr-auto hidden flex-wrap gap-x-6 gap-y-1 md:flex">
        <Link href="#como-funciona" className="text-[15px] font-medium text-muted-foreground hover:text-foreground">{t.nav.how}</Link>
        <Link href="#recursos" className="text-[15px] font-medium text-muted-foreground hover:text-foreground">{t.nav.features}</Link>
        <Link href="#planos" className="text-[15px] font-medium text-muted-foreground hover:text-foreground">{t.nav.plans}</Link>
        <Link href="#perguntas" className="text-[15px] font-medium text-muted-foreground hover:text-foreground">{t.nav.faq}</Link>
      </nav>
      <div className="ml-auto flex gap-1.5 sm:gap-2.5 md:ml-0">
        <Link href="/entrar" className={cn(buttonVariants({ size: "xl", variant: "ghost" }), "h-11 px-3 text-sm sm:px-4")}>
          {t.nav.login}
        </Link>
        <Link href="/cadastro" className={cn(buttonVariants({ size: "xl" }), "h-11 px-3 text-sm sm:px-4")}>
          {t.nav.trial}
        </Link>
      </div>
    </header>
  );
}

/** Os tipos de negócio, em etiquetas. */
export function LandingSegments() {
  return (
    <section aria-labelledby="segmentos" className={cn(wrap, "flex flex-col items-center gap-4.5 pb-20")}>
      <h2 id="segmentos" className="text-[11px] font-semibold tracking-[.4px] text-muted-foreground uppercase">
        {t.segmentsTitle}
      </h2>
      <ul className="flex flex-wrap justify-center gap-2.5">
        {t.segments.map((segment) => (
          <li key={segment} className="flex h-10 items-center rounded-full border border-input px-4 text-sm font-medium text-muted-foreground">
            {segment}
          </li>
        ))}
      </ul>
    </section>
  );
}

function SectionHeading({ id, eyebrow, title, lead }: { id: string; eyebrow: string; title: string; lead?: string }) {
  return (
    <div className="mb-12 flex max-w-[720px] flex-col gap-3.5">
      <span className="text-[11px] font-semibold tracking-[.4px] text-brand-text uppercase">{eyebrow}</span>
      <h2 id={id} className="text-[clamp(30px,3.4vw,44px)] leading-[1.08] font-extrabold tracking-[-0.03em]">
        {title}
      </h2>
      {lead ? <p className="text-lg leading-7 text-muted-foreground">{lead}</p> : null}
    </div>
  );
}

/** Como funciona, em três passos numerados. */
export function LandingHowItWorks() {
  return (
    <section id="como-funciona" aria-labelledby="como-funciona-titulo" className="border-y border-border bg-card py-24">
      <div className={wrap}>
        <SectionHeading id="como-funciona-titulo" eyebrow={t.how.eyebrow} title={t.how.title} lead={t.how.lead} />
        <ol className="grid grid-cols-[repeat(auto-fit,minmax(260px,1fr))] gap-5">
          {t.how.steps.map((step, i) => (
            <li key={step.title} className="flex flex-col gap-3.5 rounded-3xl border border-border bg-muted p-7">
              <span aria-hidden className="flex size-11 items-center justify-center rounded-[14px] bg-primary text-lg font-extrabold text-white">
                {i + 1}
              </span>
              <h3 className="text-xl leading-[26px] font-semibold">{step.title}</h3>
              <p className="text-sm leading-5 text-muted-foreground">{step.text}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}

const featureIcons: { key: keyof typeof t.features & string; icon: ReactNode }[] = [
  { key: "calls", icon: <Bell aria-hidden className="size-[22px]" /> },
  { key: "media", icon: <Video aria-hidden className="size-[22px]" /> },
  { key: "kitchen", icon: <ChefHat aria-hidden className="size-[22px]" /> },
  { key: "rodizio", icon: <Flame aria-hidden className="size-[22px]" /> },
  { key: "tv", icon: <Tv aria-hidden className="size-[22px]" /> },
  { key: "presence", icon: <ShieldCheck aria-hidden className="size-[22px]" /> },
];

/** Os recursos, em cartões. */
export function LandingFeatures() {
  return (
    <section id="recursos" aria-labelledby="recursos-titulo" className="py-24">
      <div className={wrap}>
        <SectionHeading id="recursos-titulo" eyebrow={t.features.eyebrow} title={t.features.title} />
        <ul className="grid grid-cols-[repeat(auto-fit,minmax(300px,1fr))] gap-5">
          {featureIcons.map(({ key, icon }) => {
            const feature = t.features[key] as { title: string; text: string };
            return (
              <li key={key} className="flex flex-col gap-3 rounded-3xl border border-border p-7">
                <span className="flex size-12 items-center justify-center rounded-[14px] bg-primary/15 text-brand-text">{icon}</span>
                <h3 className="text-[19px] leading-[25px] font-bold">{feature.title}</h3>
                <p className="text-[15px] leading-[23px] text-muted-foreground">{feature.text}</p>
              </li>
            );
          })}
        </ul>
      </div>
    </section>
  );
}

/** Os diferenciais, numa faixa. */
export function LandingPerks() {
  return (
    <section aria-label={t.perksLabel} className="border-y border-border bg-card py-14">
      <ul className={cn(wrap, "grid grid-cols-[repeat(auto-fit,minmax(220px,1fr))] gap-4")}>
        {t.perks.map((perk) => (
          <li key={perk.title} className="flex items-start gap-3.5">
            <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-success/15 text-success">
              <Check aria-hidden className="size-5" strokeWidth={2.4} />
            </span>
            <span className="flex flex-col gap-1">
              <span className="font-semibold">{perk.title}</span>
              <span className="text-sm text-muted-foreground">{perk.text}</span>
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}

/** A seção de planos (o conteúdo vem da API, no PlanList). */
export function LandingPlans({ children }: { children: ReactNode }) {
  return (
    <section id="planos" aria-labelledby="planos-titulo" className="py-24">
      <div className={wrap}>{children}</div>
    </section>
  );
}

export function PlansHeading() {
  return (
    <div className="flex max-w-[720px] flex-col gap-3.5">
      <span className="text-[11px] font-semibold tracking-[.4px] text-brand-text uppercase">{t.plans.eyebrow}</span>
      <h2 id="planos-titulo" className="text-[clamp(30px,3.4vw,44px)] leading-[1.08] font-extrabold tracking-[-0.03em]">
        {t.plans.title}
      </h2>
      <p className="text-lg leading-7 text-muted-foreground">{t.plans.lead}</p>
    </div>
  );
}

/** As perguntas frequentes: abrem e fecham sem JavaScript (details/summary). */
export function LandingFaq() {
  return (
    <section id="perguntas" aria-labelledby="perguntas-titulo" className="border-y border-border bg-card py-24">
      <div className={cn(wrap, "flex flex-wrap gap-10")}>
        <div className="flex-[1_1_300px]">
          <SectionHeading id="perguntas-titulo" eyebrow={t.faq.eyebrow} title={t.faq.title} />
        </div>
        <div className="flex max-w-[860px] min-w-0 flex-[999_1_520px] flex-col gap-3">
          {t.faq.items.map((item, i) => (
            <details key={item.q} open={i === 0} className="group rounded-[18px] border border-border bg-muted">
              <summary className="flex min-h-16 cursor-pointer list-none items-center gap-4 px-5 py-4 text-[17px] font-semibold [&::-webkit-details-marker]:hidden">
                <span className="flex-1">{item.q}</span>
                <ChevronDown aria-hidden className="size-5 text-muted-foreground transition-transform group-open:rotate-180" />
              </summary>
              <p className="px-5 pb-5 text-[15px] leading-6 text-muted-foreground">{item.a}</p>
            </details>
          ))}
        </div>
      </div>
    </section>
  );
}

/** A chamada final, sobre a foto. */
export function LandingClosing() {
  return (
    <section aria-labelledby="chamada-final" className="py-24">
      <div className={wrap}>
        <div className="relative flex flex-col items-start gap-5.5 overflow-hidden rounded-[36px] px-[clamp(24px,6vw,72px)] py-18">
          <Image src={photo} alt="" fill sizes="(min-width: 1200px) 1152px, 100vw" className="object-cover" />
          <div aria-hidden className="absolute inset-0 bg-gradient-to-r from-background/95 via-background/80 to-background/35" />
          <h2 id="chamada-final" className="relative max-w-[620px] text-[clamp(30px,3.4vw,44px)] leading-[1.08] font-extrabold tracking-[-0.03em]">
            {t.closing.title}
          </h2>
          <p className="relative max-w-[560px] text-lg leading-7 text-foreground/85">{t.closing.text}</p>
          <div className="relative flex flex-wrap gap-3">
            <Link href="/cadastro" className={cn(buttonVariants({ size: "xl" }), "h-14 px-6 text-base")}>
              {t.trialCta}
            </Link>
            <Link href="#planos" className={cn(buttonVariants({ size: "xl", variant: "secondary" }), "h-14 px-6 text-base")}>
              {t.plansCta}
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}

/** O rodapé. Termos, privacidade e contato entram quando existirem (CNPJ). */
export function LandingFooter() {
  return (
    <footer className={cn(wrap, "flex flex-wrap gap-x-12 gap-y-8 border-t border-border pt-12 pb-10")}>
      <div className="flex flex-[1_1_280px] flex-col gap-3.5">
        <BrandMark size={32} />
        <p className="max-w-[300px] text-sm text-muted-foreground">{t.footer.tagline}</p>
      </div>
      <nav aria-label={t.footer.product} className="flex min-w-40 flex-col gap-2.5">
        <span className="text-[11px] font-semibold tracking-[.4px] text-muted-foreground uppercase">{t.footer.product}</span>
        <Link href="#como-funciona" className="text-sm text-muted-foreground hover:text-foreground">{t.nav.how}</Link>
        <Link href="#recursos" className="text-sm text-muted-foreground hover:text-foreground">{t.nav.features}</Link>
        <Link href="#planos" className="text-sm text-muted-foreground hover:text-foreground">{t.nav.plans}</Link>
        <Link href="/entrar" className="text-sm text-muted-foreground hover:text-foreground">{t.nav.login}</Link>
      </nav>
      <p className="basis-full border-t border-border pt-6 text-[13px] text-muted-foreground">{t.footer.copyright(2026)}</p>
    </footer>
  );
}
