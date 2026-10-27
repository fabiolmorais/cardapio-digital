"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { Delete } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { setLoginOrigin } from "@/lib/auth/login-origin";
import { startSession } from "@/lib/auth/session";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import {
  DeviceNotLinkedError,
  fetchPinMembers,
  pinLogin,
  PinLoginError,
  type CurrentDevice,
  type LoginResponse,
} from "./device-api";

const t = messages.pin;
const roleNames = messages.panel.roles;

const MIN_PIN = 4;
const MAX_PIN = 6;
const DIGITS = ["1", "2", "3", "4", "5", "6", "7", "8", "9"] as const;

function initials(name: string) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}

type Props = {
  current: CurrentDevice;
  /** O back disse que o aparelho não está mais vinculado: volta para a tela do código. */
  onDeviceLost: () => void;
};

/**
 * "Quem está entrando?": a pessoa toca no próprio nome, digita o PIN no teclado e entra no
 * painel. Os erros (PIN errado, bloqueio) vêm prontos do back e aparecem na tela.
 */
export function PinScreen({ current, onDeviceLost }: Props) {
  const router = useRouter();
  const [selected, setSelected] = useState<string | null>(null);
  const [pin, setPin] = useState("");
  const [showForgot, setShowForgot] = useState(false);

  const members = useQuery({ queryKey: ["pin-members"], queryFn: fetchPinMembers, retry: false });

  const login = useMutation<LoginResponse, PinLoginError, { memberId: string; pin: string }>({
    mutationFn: ({ memberId, pin: typed }) => pinLogin(memberId, typed),
    onSuccess: (data) => {
      startSession({
        accessToken: data.accessToken,
        expiresAt: data.expiresAt,
        userName: data.userName,
        establishment: data.establishment,
      });
      setLoginOrigin("device");
      router.push("/painel");
    },
    onError: (error) => {
      setPin("");
      // 404: a pessoa saiu da lista (desligada); a lista é recarregada.
      if (error.status === 404) {
        setSelected(null);
        void members.refetch();
      }
      // 401 também acontece quando o aparelho foi desvinculado: confere o aparelho.
      if (error.status === 401) onDeviceLost();
    },
  });

  useEffect(() => {
    if (members.error instanceof DeviceNotLinkedError) onDeviceLost();
  }, [members.error, onDeviceLost]);

  function pick(id: string) {
    setSelected(id);
    setPin("");
    setShowForgot(false);
    login.reset();
  }

  function press(digit: string) {
    if (!selected || pin.length >= MAX_PIN) return;
    setPin(pin + digit);
    if (login.isError) login.reset();
  }

  function submit() {
    if (selected && pin.length >= MIN_PIN) login.mutate({ memberId: selected, pin });
  }

  const establishmentName = current.establishmentName;
  const locked = login.error?.status === 429;
  const dots = Math.max(MIN_PIN, pin.length);
  const keyClass =
    "h-17 rounded-[20px] border border-border bg-muted text-[26px] font-semibold transition-colors hover:bg-secondary focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none disabled:opacity-40";
  const ghostKeyClass =
    "h-17 rounded-[20px] text-sm font-medium text-muted-foreground hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none";

  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col gap-6 px-6 pt-9 pb-7">
      <div className="flex items-center gap-2.5">
        <span
          aria-hidden
          className="flex size-10 items-center justify-center rounded-xl bg-primary text-[13px] font-bold text-primary-foreground"
        >
          {initials(establishmentName)}
        </span>
        <span className="flex min-w-0 flex-col">
          <span className="truncate font-semibold">{establishmentName}</span>
          <span className="truncate text-sm text-muted-foreground">{t.linkedAs(current.device.name)}</span>
        </span>
      </div>

      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">{t.title}</h1>
        <p className="text-muted-foreground">{t.subtitle}</p>
      </div>

      {members.isPending ? (
        <p className="text-muted-foreground">{t.loading}</p>
      ) : members.isError ? (
        <div className="flex flex-col items-start gap-3">
          <p role="alert" className="text-destructive">
            {t.loadError}
          </p>
          <Button variant="outline" size="xl" onClick={() => void members.refetch()}>
            {t.retry}
          </Button>
        </div>
      ) : members.data.length === 0 ? (
        <p className="text-muted-foreground">{t.empty}</p>
      ) : (
        <div role="group" aria-label={t.team} className="flex flex-wrap gap-x-6 gap-y-4">
          {members.data.map((member) => {
            const on = member.id === selected;
            return (
              <button
                key={member.id}
                type="button"
                aria-pressed={on}
                aria-label={`${member.name}, ${roleNames[member.role]}`}
                onClick={() => pick(member.id)}
                className={cn(
                  "flex w-18 flex-col items-center gap-1.5 rounded-xl p-1 text-[13px] font-semibold text-muted-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                  on && "text-foreground",
                )}
              >
                <span
                  aria-hidden
                  className={cn(
                    "flex size-15 items-center justify-center rounded-full border-2 border-transparent bg-secondary text-lg font-bold text-foreground",
                    on && "border-brand-text shadow-[0_0_0_4px_rgba(240,87,58,.18)]",
                  )}
                >
                  {initials(member.name)}
                </span>
                <span className="w-full truncate text-center">{member.name.split(/\s+/)[0]}</span>
              </button>
            );
          })}
        </div>
      )}

      <div className="flex flex-col items-center gap-2.5">
        <div className="flex justify-center gap-4" role="status" aria-label={t.digits(pin.length)}>
          {Array.from({ length: dots }, (_, index) => (
            <span
              key={index}
              aria-hidden
              className={cn(
                "size-4 rounded-full border-2 border-input",
                index < pin.length && "border-foreground bg-foreground",
              )}
            />
          ))}
        </div>
        <p id="pin-error" role="alert" aria-live="assertive" className="min-h-5 text-center text-sm text-destructive">
          {login.error?.message ?? ""}
        </p>
        {showForgot && <p className="text-center text-sm text-muted-foreground">{t.forgotHelp}</p>}
        {!selected && members.data && members.data.length > 0 && (
          <p className="text-center text-sm text-muted-foreground">{t.pickName}</p>
        )}
      </div>

      <div className="mt-auto grid grid-cols-3 gap-3">
        {DIGITS.map((digit) => (
          <button
            key={digit}
            type="button"
            className={keyClass}
            disabled={!selected || locked || login.isPending}
            onClick={() => press(digit)}
          >
            {digit}
          </button>
        ))}
        <button
          type="button"
          className={ghostKeyClass}
          aria-label={t.keys.forgotLabel}
          onClick={() => setShowForgot(true)}
        >
          {t.keys.forgot}
        </button>
        <button
          type="button"
          className={keyClass}
          disabled={!selected || locked || login.isPending}
          onClick={() => press("0")}
        >
          0
        </button>
        <button
          type="button"
          className={cn(ghostKeyClass, "flex items-center justify-center")}
          aria-label={t.keys.backspace}
          disabled={pin.length === 0}
          onClick={() => setPin(pin.slice(0, -1))}
        >
          <Delete aria-hidden className="size-6" />
        </button>
      </div>

      <Button
        size="xl"
        className="h-13"
        disabled={!selected || pin.length < MIN_PIN || login.isPending || locked}
        onClick={submit}
      >
        {login.isPending ? t.submitting : t.submit}
      </Button>
    </main>
  );
}
