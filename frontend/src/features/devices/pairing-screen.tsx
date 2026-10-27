"use client";

import { Clock, UtensilsCrossed } from "lucide-react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { Button, buttonVariants } from "@/components/ui/button";
import { messages } from "@/messages/pt-BR";
import { claimPairing, PairingGoneError, startPairing, type PairingStarted } from "./device-api";

const t = messages.pairing;

/** De quanto em quanto tempo o aparelho pergunta se o gerente já vinculou. */
const POLL_MS = 3_000;
/** Validade do código no back; usada só se o relógio do aparelho estiver muito diferente do servidor. */
const DEFAULT_TTL_MS = 10 * 60_000;

/** O que aconteceu com o código atual depois que ele apareceu na tela. */
type Outcome = { kind: "linked" } | { kind: "expired" } | null;

type Pairing = { pairing: PairingStarted; receivedAt: number; expiresAtLocal: number };

/**
 * Prazo do código no relógio deste aparelho. Se o relógio estiver muito adiantado ou atrasado
 * em relação ao servidor, a conta com expiresAt sairia errada; aí vale a validade padrão.
 */
function localExpiry(expiresAt: string): number {
  const ttl = Date.parse(expiresAt) - Date.now();
  return Date.now() + (ttl > 0 && ttl <= DEFAULT_TTL_MS + 60_000 ? ttl : DEFAULT_TTL_MS);
}

function formatRemaining(ms: number): string {
  const total = Math.max(0, Math.ceil(ms / 1000));
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, "0")}`;
}

/**
 * Tela do aparelho novo: mostra o código para o gerente digitar no painel e fica perguntando
 * ao back até ser vinculado. O token do aparelho chega num cookie HttpOnly, que esta tela
 * nunca vê; quando chega, avisa quem a usa (onLinked) para trocar pela tela do PIN.
 */
export function PairingScreen({ onLinked }: { onLinked: () => void }) {
  const [outcome, setOutcome] = useState<Outcome>(null);
  // Começa vazio: Date.now() na renderização daria um valor diferente no servidor e no navegador.
  const [now, setNow] = useState<number | null>(null);

  // Pede o código uma vez ao abrir a tela (e de novo só pelo botão "Gerar outro código").
  const code = useQuery({
    queryKey: ["device-pairing"],
    queryFn: async (): Promise<Pairing> => {
      const pairing = await startPairing();
      return { pairing, receivedAt: Date.now(), expiresAtLocal: localExpiry(pairing.expiresAt) };
    },
    staleTime: Infinity,
    gcTime: 0,
    retry: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
  const waiting = code.data && outcome === null && !code.isFetching ? code.data : null;

  function newCode() {
    setOutcome(null);
    void code.refetch();
  }

  // Relógio da contagem regressiva.
  useEffect(() => {
    if (!waiting) return;
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [waiting]);

  // Pergunta ao back até o gerente vincular (ou o pedido deixar de existir).
  useEffect(() => {
    if (!waiting) return;
    const { id, secret } = waiting.pairing;
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout>;

    async function poll() {
      try {
        const result = await claimPairing(id, secret);
        if (cancelled) return;
        if (result.status === "LINKED") {
          setOutcome({ kind: "linked" });
          onLinked();
          return;
        }
      } catch (error) {
        if (cancelled) return;
        if (error instanceof PairingGoneError) {
          setOutcome({ kind: "expired" });
          return;
        }
        // Sem rede ou erro passageiro: tenta de novo na próxima volta.
      }
      timer = setTimeout(poll, POLL_MS);
    }

    timer = setTimeout(poll, POLL_MS);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [waiting, onLinked]);

  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col gap-7 px-6 pt-10 pb-8">
      <div className="flex items-center gap-2.5">
        <span className="flex size-10 items-center justify-center rounded-xl bg-secondary">
          <UtensilsCrossed aria-hidden className="size-5" />
        </span>
        <span className="font-semibold">{messages.app.name}</span>
      </div>

      {outcome?.kind === "linked" ? (
        <p role="status" className="text-muted-foreground">
          {t.loading}
        </p>
      ) : (
        <>
          <div className="flex flex-col gap-2.5">
            <h1 className="text-2xl font-bold tracking-tight">{t.title}</h1>
            <p className="text-muted-foreground">{t.subtitle}</p>
          </div>

          <section className="flex flex-col items-center gap-4 rounded-2xl border border-border bg-card px-4 py-6">
            <span className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">{t.codeLabel}</span>
            {waiting ? (
              <>
                <div
                  className="flex gap-2"
                  role="img"
                  aria-label={t.codeAria(waiting.pairing.code)}
                  data-testid="pairing-code"
                  data-code={waiting.pairing.code}
                >
                  {waiting.pairing.code.split("").map((digit, index) => (
                    <span
                      key={index}
                      aria-hidden
                      className="flex h-15 w-12 items-center justify-center rounded-[14px] border border-input bg-muted font-mono text-[28px] font-bold"
                    >
                      {digit}
                    </span>
                  ))}
                </div>
                <span className="flex items-center gap-1.5 text-sm text-muted-foreground">
                  <Clock aria-hidden className="size-3.5" />
                  {t.validFor(formatRemaining(waiting.expiresAtLocal - (now ?? waiting.receivedAt)))}
                </span>
              </>
            ) : code.isFetching ? (
              <p className="py-5 text-muted-foreground">{t.loading}</p>
            ) : (
              <div className="flex flex-col items-center gap-3 py-2">
                <p role="alert" className="text-destructive">
                  {outcome?.kind === "expired" ? t.expired : t.error}
                </p>
                <Button size="xl" onClick={newCode}>
                  {t.newCode}
                </Button>
              </div>
            )}
          </section>

          <ol className="flex flex-col gap-3.5">
            {t.steps.map((step, index) => (
              <li key={step} className="flex items-start gap-3">
                <span
                  aria-hidden
                  className="flex size-6.5 shrink-0 items-center justify-center rounded-full bg-primary/15 text-[13px] font-semibold text-brand-text"
                >
                  {index + 1}
                </span>
                <span className="text-muted-foreground">{step}</span>
              </li>
            ))}
          </ol>

          <div className="mt-auto flex flex-col gap-2.5">
            {waiting && (
              <p className="text-center text-sm text-muted-foreground" role="status">
                {t.waiting}
              </p>
            )}
            <Link href="/entrar" className={buttonVariants({ variant: "outline", size: "xl" })}>
              {t.passwordLogin}
            </Link>
          </div>
        </>
      )}
    </main>
  );
}
