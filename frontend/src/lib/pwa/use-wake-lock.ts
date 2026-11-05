"use client";

import { useEffect } from "react";

/**
 * Mantém a tela acesa enquanto a página está à vista (cozinha, passadores, Modo TV). O navegador
 * solta a trava quando a página vai para o segundo plano; ao voltar, pede de novo. Sem suporte
 * (navegador antigo), a tela apaga como sempre.
 */
export function useWakeLock(enabled = true) {
  useEffect(() => {
    if (!enabled || !("wakeLock" in navigator)) return;
    let lock: WakeLockSentinel | null = null;
    let stopped = false;

    async function acquire() {
      if (document.visibilityState !== "visible" || (lock && !lock.released)) return;
      try {
        lock = await navigator.wakeLock.request("screen");
        if (stopped) void lock.release();
      } catch {
        // Bateria fraca ou sem permissão: segue sem a trava.
      }
    }

    const onVisibility = () => void acquire();
    void acquire();
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      stopped = true;
      document.removeEventListener("visibilitychange", onVisibility);
      void lock?.release();
    };
  }, [enabled]);
}
