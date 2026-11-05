/** O evento do Chrome/Android que permite mostrar o "Instalar app" num botão nosso. */
type InstallPromptEvent = Event & { prompt: () => Promise<void>; userChoice: Promise<{ outcome: string }> };

let deferred: InstallPromptEvent | null = null;
const listeners = new Set<() => void>();
let listening = false;

function emit() {
  listeners.forEach((listener) => listener());
}

/** Guarda o convite de instalação assim que o navegador oferece (pode chegar antes da tela abrir). */
export function listenForInstallPrompt() {
  if (listening) return;
  listening = true;
  window.addEventListener("beforeinstallprompt", (event) => {
    event.preventDefault();
    deferred = event as InstallPromptEvent;
    emit();
  });
  window.addEventListener("appinstalled", () => {
    deferred = null;
    emit();
  });
}

export function subscribeToInstallPrompt(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function canPromptInstall(): boolean {
  return deferred !== null;
}

/** Abre o "Instalar app" do navegador. O convite só vale uma vez. */
export async function promptInstall(): Promise<boolean> {
  const event = deferred;
  if (!event) return false;
  deferred = null;
  emit();
  await event.prompt();
  return (await event.userChoice).outcome === "accepted";
}
