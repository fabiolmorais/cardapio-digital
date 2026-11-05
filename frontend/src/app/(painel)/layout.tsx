import { StaffAppSetup } from "@/features/pwa/staff-app-setup";
import { staffAppMetadata, staffAppViewport } from "@/features/pwa/staff-app-metadata";

export const metadata = staffAppMetadata;
export const viewport = staffAppViewport;

/** As telas da equipe: o painel, a cozinha, os passadores, o Modo TV e o vínculo de aparelho. */
export default function PanelGroupLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <StaffAppSetup />
      {children}
    </>
  );
}
