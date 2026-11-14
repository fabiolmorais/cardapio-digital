"use client";

import { DevicesPanel } from "@/features/devices/devices-panel";
import { useSession } from "@/lib/auth/use-session";
import { TeamManager } from "./team-manager";
import { canManageTeam } from "./team-rules";

/**
 * Equipe à esquerda e aparelhos à direita, como no desenho 12.5. Abaixo de 1536 px, um embaixo do
 * outro: lado a lado, a tabela da equipe não cabe (o notebook de 1280 cortava os botões).
 */
export function TeamPage() {
  const { status, session } = useSession();
  const showDevices = status === "authenticated" && canManageTeam(session.establishment.role);

  return (
    <div className="flex flex-col gap-6 px-4 py-6 sm:px-7 2xl:flex-row 2xl:items-start">
      <TeamManager />
      {showDevices && <DevicesPanel />}
    </div>
  );
}
