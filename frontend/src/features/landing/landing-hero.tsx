import { ArrowRight, Bell, Check, ChefHat, QrCode, ReceiptText } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import combinado from "../../../public/images/combinado.jpg";
import photo from "../../../public/images/cadastro.jpg";
import temaki from "../../../public/images/temaki.jpg";

const t = messages.home;
const dishPhotos = [combinado, temaki];

/** A abertura: o título, os botões e, ao lado, o celular da mesa com os avisos chegando na equipe. */
export function LandingHero() {
  return (
    <section id="topo" className="mx-auto flex w-full max-w-[1200px] flex-wrap items-center gap-12 px-6 pt-10 pb-18">
      <div className="flex min-w-0 flex-[1_1_480px] flex-col gap-6">
        <span className="inline-flex h-8 items-center gap-2 self-start rounded-full bg-primary/15 px-3.5 text-[13px] font-semibold text-brand-text">
          <QrCode aria-hidden className="size-4" />
          {t.eyebrow}
        </span>
        <h1 className="text-[clamp(40px,5.4vw,68px)] leading-[1.02] font-extrabold tracking-[-0.03em]">
          {t.headlineStart}
          <span className="text-brand-text">{t.headlineAccent}</span>
        </h1>
        <p className="text-lg leading-7 text-muted-foreground">{t.subheadline}</p>
        <div className="flex flex-wrap gap-3">
          <Link href="/cadastro" className={cn(buttonVariants({ size: "xl" }), "h-14 px-6 text-base")}>
            {t.trialCta}
            <ArrowRight aria-hidden className="size-[18px]" />
          </Link>
          <Link href="#planos" className={cn(buttonVariants({ size: "xl", variant: "secondary" }), "h-14 px-6 text-base")}>
            {t.plansCta}
          </Link>
        </div>
        <ul className="flex flex-wrap gap-x-5 gap-y-2 text-sm text-muted-foreground">
          {t.trust.map((item) => (
            <li key={item} className="flex items-center gap-1.5">
              <Check aria-hidden className="size-4 text-success" strokeWidth={2.6} />
              {item}
            </li>
          ))}
        </ul>
      </div>

      <figure aria-label={t.demo.label} className="relative m-0 h-[600px] min-w-0 flex-[1_1_440px]">
        <div className="absolute inset-y-0 right-0 left-10 overflow-hidden rounded-[36px] bg-card">
          <Image src={photo} alt={t.demo.photoAlt} fill priority sizes="(min-width: 1024px) 560px, 100vw" className="object-cover" />
          <div aria-hidden className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-background/75" />
        </div>

        {/* O celular da mesa (ilustração: o leitor de tela já ouve a legenda da figura). */}
        <div aria-hidden className="absolute bottom-7 left-0 h-[480px] w-[236px] rounded-[40px] bg-black p-[9px] shadow-[0_40px_80px_rgba(0,0,0,.6)]">
          <div className="flex h-full flex-col gap-3 overflow-hidden rounded-[34px] bg-[#141110] px-3.5 py-5">
            <div className="flex items-center gap-2.5">
              <span className="flex size-[34px] items-center justify-center rounded-full bg-primary text-xs font-bold text-white">SS</span>
              <span className="flex flex-col">
                <span className="text-sm font-bold">{t.demo.restaurant}</span>
                <span className="text-[11px] text-muted-foreground">{t.demo.table}</span>
              </span>
            </div>
            <span className="text-2xl leading-[26px] font-extrabold tracking-tight">
              {t.demo.title}
              <br />
              <span className="text-brand-text">{t.demo.titleAccent}</span>
            </span>
            <div className="grid grid-cols-2 gap-2">
              <span className="flex flex-col gap-3.5 rounded-[18px] bg-primary p-3 text-white">
                <Bell className="size-5" />
                <span className="text-[13px] font-bold">{t.demo.call}</span>
              </span>
              <span className="flex flex-col gap-3.5 rounded-[18px] border border-border bg-muted p-3">
                <ReceiptText className="size-5" />
                <span className="text-[13px] font-bold">{t.demo.bill}</span>
              </span>
            </div>
            <span className="mt-1 text-[13px] font-bold">{t.demo.popular}</span>
            <div className="grid grid-cols-2 gap-2">
              {t.demo.dishes.map((dish, i) => (
                <span key={dish.name} className="flex flex-col overflow-hidden rounded-2xl bg-muted">
                  <Image src={dishPhotos[i]} alt="" className="h-[86px] w-full object-cover" sizes="120px" />
                  <span className="p-2 text-[11px] font-semibold">
                    {dish.name}
                    <br />
                    <span className="text-brand-text">{dish.price}</span>
                  </span>
                </span>
              ))}
            </div>
          </div>
        </div>

        <div aria-hidden className="absolute top-10 right-3 hidden sm:flex w-[240px] items-center gap-3 rounded-[20px] border border-input bg-card/90 px-4 py-3.5 shadow-[0_24px_50px_rgba(0,0,0,.5)] backdrop-blur-md">
          <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary text-white">
            <Bell className="size-5" />
          </span>
          <span className="flex flex-1 flex-col gap-0.5">
            <span className="text-[15px] font-bold">{t.demo.callAlert}</span>
            <span className="text-[13px] text-muted-foreground">{t.demo.callWhere}</span>
          </span>
          <span className="rounded-full bg-success/15 px-2.5 py-1 font-mono text-[13px] font-semibold text-success">{t.demo.callTimer}</span>
        </div>
        <div aria-hidden className="absolute top-[150px] right-8 hidden sm:flex w-[230px] items-center gap-3 rounded-[20px] border border-input bg-card/90 px-4 py-3.5 shadow-[0_24px_50px_rgba(0,0,0,.5)] backdrop-blur-md">
          <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-amber-400/15 text-amber-400">
            <ChefHat className="size-5" />
          </span>
          <span className="flex flex-1 flex-col gap-0.5">
            <span className="text-[15px] font-bold">{t.demo.order}</span>
            <span className="text-[13px] text-muted-foreground">{t.demo.orderWhere}</span>
          </span>
        </div>
      </figure>
    </section>
  );
}
