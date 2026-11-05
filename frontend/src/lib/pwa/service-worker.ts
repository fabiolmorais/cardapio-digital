/** O service worker do app da equipe vale só para as telas do painel: a página da mesa nunca vira app. */
export const STAFF_SW_SCOPE = "/painel";

let registering: Promise<ServiceWorkerRegistration> | null = null;

export function serviceWorkerSupported(): boolean {
  return typeof navigator !== "undefined" && "serviceWorker" in navigator;
}

/** Registra o /sw.js uma vez por aba (o navegador reaproveita o que já está instalado). */
export function registerStaffServiceWorker(): Promise<ServiceWorkerRegistration> {
  registering ??= navigator.serviceWorker.register("/sw.js", { scope: STAFF_SW_SCOPE }).catch((error: unknown) => {
    registering = null;
    throw error;
  });
  return registering;
}

/** O registro com o service worker já ativo: o push só se inscreve depois disso. */
export async function activeStaffRegistration(): Promise<ServiceWorkerRegistration> {
  const registration = await registerStaffServiceWorker();
  if (registration.active) return registration;
  const worker = registration.installing ?? registration.waiting;
  await new Promise<void>((resolve) => {
    worker?.addEventListener("statechange", () => {
      if (worker.state === "activated") resolve();
    });
  });
  return registration;
}

/** O app instalado (tela inicial), e não a aba do navegador. */
export function isStandalone(): boolean {
  return (
    window.matchMedia("(display-mode: standalone)").matches ||
    (navigator as Navigator & { standalone?: boolean }).standalone === true
  );
}

/** iPhone e iPad (o iPad novo se apresenta como Mac, mas tem toque). */
export function isIos(): boolean {
  const ua = navigator.userAgent;
  return /iPhone|iPad|iPod/.test(ua) || (ua.includes("Macintosh") && navigator.maxTouchPoints > 1);
}
