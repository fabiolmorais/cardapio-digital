"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback } from "react";
import { Button } from "@/components/ui/button";
import { messages } from "@/messages/pt-BR";
import { DeviceTvMode } from "@/features/calls/tv-mode";
import { claimPairing, DeviceNotLinkedError, fetchCurrentDevice, inviteFromHash } from "./device-api";
import { PairingScreen } from "./pairing-screen";
import { PinScreen } from "./pin-screen";

/**
 * Porta de entrada do aparelho da equipe (/aparelho). Pergunta ao back quem é este aparelho
 * (pelo cookie HttpOnly, que a página não lê):
 * - não vinculado: mostra o código para o gerente vincular;
 * - celular ou tablet: "Quem está entrando?", com o PIN;
 * - TV: o Modo TV direto, sem PIN e sem login (a fila vem pelo cookie do aparelho).
 *
 * Aberta pelo QR Code do gerente (/aparelho#convite=...), primeiro usa o convite: o aparelho
 * busca o token e já entra vinculado, sem código.
 */
export function DeviceEntry() {
  const queryClient = useQueryClient();
  const invite = useQuery({
    queryKey: ["device-invite"],
    queryFn: claimInvite,
    retry: false,
    staleTime: Infinity,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
  const device = useQuery({
    queryKey: ["current-device"],
    queryFn: fetchCurrentDevice,
    retry: false,
    refetchOnWindowFocus: false,
    enabled: !invite.isPending,
  });

  const recheck = useCallback(() => {
    void queryClient.invalidateQueries({ queryKey: ["current-device"] });
  }, [queryClient]);

  if (invite.isPending) {
    return <p className="p-8 text-muted-foreground">{messages.pairing.claiming}</p>;
  }
  if (device.isPending) {
    return <p className="p-8 text-muted-foreground">{messages.pin.loading}</p>;
  }
  if (device.error instanceof DeviceNotLinkedError) {
    return <PairingScreen onLinked={recheck} inviteFailed={invite.isError} />;
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
    return <DeviceTvMode establishmentName={device.data.establishmentName} onDeviceLost={recheck} />;
  }
  return <PinScreen current={device.data} onDeviceLost={recheck} />;
}

/**
 * O convite do QR Code, se a página veio dele. Tira o segredo do endereço antes de usar (não fica
 * no histórico nem num link copiado) e busca o token; o cookie chega na resposta. Sem convite,
 * não faz nada. Vencido ou já usado: erro, e a tela mostra o código com o aviso.
 */
async function claimInvite(): Promise<boolean> {
  const found = inviteFromHash(window.location.hash);
  if (!found) return false;
  window.history.replaceState(null, "", window.location.pathname + window.location.search);
  const result = await claimPairing(found.id, found.secret);
  if (result.status !== "LINKED") throw new Error(messages.pairing.inviteFailed);
  return true;
}
