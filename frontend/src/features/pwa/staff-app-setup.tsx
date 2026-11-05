"use client";

import { useEffect } from "react";
import { listenForInstallPrompt } from "@/lib/pwa/install-prompt";
import { registerStaffServiceWorker, serviceWorkerSupported } from "@/lib/pwa/service-worker";

/** Liga o app da equipe: registra o service worker (os avisos) e guarda o convite de instalação. */
export function StaffAppSetup() {
  useEffect(() => {
    listenForInstallPrompt();
    if (serviceWorkerSupported()) registerStaffServiceWorker().catch(() => {});
  }, []);
  return null;
}
