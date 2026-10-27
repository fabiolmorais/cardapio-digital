"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Tv } from "lucide-react";
import { useCallback } from "react";
import { Button } from "@/components/ui/button";
import { messages } from "@/messages/pt-BR";
import { DeviceNotLinkedError, fetchCurrentDevice } from "./device-api";
import { PairingScreen } from "./pairing-screen";
import { PinScreen } from "./pin-screen";

/**
 * Porta de entrada do aparelho da equipe (/aparelho). Pergunta ao back quem é este aparelho
 * (pelo cookie HttpOnly, que a página não lê):
 * - não vinculado: mostra o código para o gerente vincular;
 * - celular ou tablet: "Quem está entrando?", com o PIN;
 * - TV: aviso, porque na TV ninguém entra com PIN.
 */
export function DeviceEntry() {
  const queryClient = useQueryClient();
  const device = useQuery({
    queryKey: ["current-device"],
    queryFn: fetchCurrentDevice,
    retry: false,
    refetchOnWindowFocus: false,
  });

  const recheck = useCallback(() => {
    void queryClient.invalidateQueries({ queryKey: ["current-device"] });
  }, [queryClient]);

  if (device.isPending) {
    return <p className="p-8 text-muted-foreground">{messages.pin.loading}</p>;
  }
  if (device.error instanceof DeviceNotLinkedError) {
    return <PairingScreen onLinked={recheck} />;
  }
  if (device.isError) {
    return (
      <main className="mx-auto flex w-full max-w-md flex-col items-start gap-4 px-6 py-10">
        <p role="alert" className="text-destructive">
          {messages.pin.loadError}
        </p>
        <Button variant="outline" size="xl" onClick={recheck}>
          {messages.pin.retry}
        </Button>
      </main>
    );
  }
  if (device.data.device.type === "TV") {
    return (
      <main className="mx-auto flex w-full max-w-md flex-col items-start gap-4 px-6 py-10">
        <Tv aria-hidden className="size-12 text-muted-foreground" />
        <h1 className="text-2xl font-bold tracking-tight">{messages.pin.tv.title}</h1>
        <p className="text-muted-foreground">{messages.pin.tv.text}</p>
      </main>
    );
  }
  return <PinScreen current={device.data} onDeviceLost={recheck} />;
}
