/**
 * O alerta sonoro dos chamados, gerado na hora (Web Audio), sem arquivo de som para baixar.
 *
 * O navegador só deixa uma página tocar som depois de um toque nela. Por isso o botão "Ativar
 * som" chama enableSound() dentro do toque: é ali que o AudioContext é criado e liberado. Depois
 * disso, playChime() toca sozinho a cada chamado. "Desligar som" (disableSound) pausa o
 * AudioContext: o alerta fica mudo até ativar de novo.
 *
 * A escolha fica guardada no aparelho: quem deixou o som ligado não precisa confirmar de novo a
 * cada entrada. restoreSound() tenta religar na hora (o app instalado no Android costuma deixar)
 * e, se o navegador exigir o toque, religa no primeiro toque em qualquer lugar da tela.
 */
let context: AudioContext | null = null;
const listeners = new Set<() => void>();

const PREFERENCE = "ohchefe:sound";
/** O que a pessoa escolheu neste aparelho; null: ainda não escolheu (o aviso pergunta). */
export type SoundPreference = "on" | "off" | null;

export function soundPreference(): SoundPreference {
  try {
    const value = localStorage.getItem(PREFERENCE);
    return value === "on" || value === "off" ? value : null;
  } catch {
    return null;
  }
}

function remember(preference: "on" | "off") {
  try {
    localStorage.setItem(PREFERENCE, preference);
  } catch {
    // Armazenamento bloqueado: vale só até fechar a página.
  }
}

function notify() {
  listeners.forEach((listener) => listener());
}

export function isSoundOn(): boolean {
  return context?.state === "running";
}

export function subscribeToSound(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** Chamar dentro do toque no botão. Devolve se o som ficou ligado. */
export async function enableSound(): Promise<boolean> {
  remember("on");
  try {
    context ??= new AudioContext();
    await context.resume();
    playChime();
  } catch {
    // Navegador sem Web Audio: segue sem som.
  }
  notify();
  return isSoundOn();
}

/** "Desligar som": pausa o áudio da página; os alertas ficam mudos até ativar de novo. */
export async function disableSound(): Promise<void> {
  remember("off");
  try {
    await context?.suspend();
  } catch {
    // Sem Web Audio, não havia som para desligar.
  }
  notify();
}

/** Os toques que o navegador aceita para liberar o som (o iPhone libera no fim do toque). */
const UNLOCK_EVENTS = ["pointerdown", "touchend", "click", "keydown"] as const;

/**
 * Volta com o som que a pessoa deixou ligado neste aparelho, sem perguntar: liga na hora, se o
 * navegador deixar, ou no primeiro toque na tela. O toque no próprio botão do som fica de fora
 * (marcado com data-sound-toggle): ele liga e desliga sozinho. Devolve a função que para de ouvir.
 */
export function restoreSound(): () => void {
  if (soundPreference() !== "on" || isSoundOn()) return () => {};
  let audio: AudioContext;
  try {
    audio = context ??= new AudioContext();
  } catch {
    return () => {};
  }
  const stop = () => {
    UNLOCK_EVENTS.forEach((type) => document.removeEventListener(type, unlock, true));
    audio.removeEventListener("statechange", changed);
  };
  function unlock(event: Event) {
    if (soundPreference() !== "on") return stop();
    if (event.target instanceof Element && event.target.closest("[data-sound-toggle]")) return;
    audio.resume().catch(() => {});
  }
  function changed() {
    notify();
    if (audio.state === "running") stop();
  }
  audio.addEventListener("statechange", changed);
  UNLOCK_EVENTS.forEach((type) => document.addEventListener(type, unlock, true));
  // Sem toque: só liga se o navegador deixar; senão, fica esperando o primeiro toque.
  audio.resume().catch(() => {});
  return stop;
}

/** Dois pulsos curtos, no ritmo do "ding-dong". */
const VIBRATION = [200, 100, 200];

/**
 * "Ding-dong": duas notas curtas, fáceis de ouvir num salão barulhento. O celular também vibra,
 * para quem está com ele no bolso (no Android; o Safari do iPhone não deixa a página vibrar).
 */
export function playChime() {
  vibrate();
  if (!context || context.state !== "running") return;
  const start = context.currentTime;
  [
    { frequency: 988, at: 0 },
    { frequency: 740, at: 0.22 },
  ].forEach(({ frequency, at }) => {
    const oscillator = context!.createOscillator();
    const gain = context!.createGain();
    oscillator.type = "sine";
    oscillator.frequency.value = frequency;
    gain.gain.setValueAtTime(0.0001, start + at);
    gain.gain.exponentialRampToValueAtTime(0.5, start + at + 0.02);
    gain.gain.exponentialRampToValueAtTime(0.0001, start + at + 0.5);
    oscillator.connect(gain).connect(context!.destination);
    oscillator.start(start + at);
    oscillator.stop(start + at + 0.55);
  });
}

function vibrate() {
  try {
    // Sem suporte (iPhone, computador) ou antes do primeiro toque na página: não faz nada.
    navigator.vibrate?.(VIBRATION);
  } catch {
    // Alguns navegadores bloqueiam a vibração em vez de ignorar.
  }
}
