"use client";

import { DevicesPanel } from "@/features/devices/devices-panel";
import { useSession } from "@/lib/auth/use-session";
import { TeamManager } from "./team-manager";
import { canManageTeam } from "./team-rules";

/** Equipe à esquerda e aparelhos à direita (no celular, um embaixo do outro), como no desenho 12.5. */
export function TeamPage() {
  const { status, session } = useSession();
  const showDevices = status === "authenticated" && canManageTeam(session.establishment.role);

  return (
    <div className="flex flex-col gap-6 px-4 py-6 sm:px-7 xl:flex-row xl:items-start">
      <TeamManager />
      {showDevices && <DevicesPanel />}
    </div>
  );
}
