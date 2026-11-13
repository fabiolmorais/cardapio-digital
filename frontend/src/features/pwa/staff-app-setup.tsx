"use client";

import { useEffect } from "react";
import { restoreSound } from "@/features/calls/alert-sound";
import { listenForInstallPrompt } from "@/lib/pwa/install-prompt";
import { registerStaffServiceWorker, serviceWorkerSupported } from "@/lib/pwa/service-worker";

/**
 * Liga o app da equipe: registra o service worker (os avisos), guarda o convite de instalação e
 * volta com o som que a pessoa deixou ligado neste aparelho.
 */
export function StaffAppSetup() {
  useEffect(() => {
    listenForInstallPrompt();
    if (serviceWorkerSupported()) registerStaffServiceWorker().catch(() => {});
    return restoreSound();
  }, []);
  return null;
}
