/**
 * O alerta sonoro dos chamados, gerado na hora (Web Audio), sem arquivo de som para baixar.
 *
 * O navegador só deixa uma página tocar som depois de um toque nela. Por isso o som começa
 * desligado, e o botão "Ativar som" chama enableSound() dentro do toque: é ali que o
 * AudioContext é criado e liberado. Depois disso, playChime() toca sozinho a cada chamado.
 * "Desligar som" (disableSound) pausa o AudioContext: o alerta fica mudo até ativar de novo.
 */
let context: AudioContext | null = null;
const listeners = new Set<() => void>();

export function isSoundOn(): boolean {
  return context?.state === "running";
}

export function subscribeToSound(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** Chamar dentro do toque no botão. Devolve se o som ficou ligado. */
export async function enableSound(): Promise<boolean> {
  try {
    context ??= new AudioContext();
    await context.resume();
    playChime();
  } catch {
    // Navegador sem Web Audio: segue sem som.
  }
  listeners.forEach((listener) => listener());
  return isSoundOn();
}

/** "Desligar som": pausa o áudio da página; os alertas ficam mudos até ativar de novo. */
export async function disableSound(): Promise<void> {
  try {
    await context?.suspend();
  } catch {
    // Sem Web Audio, não havia som para desligar.
  }
  listeners.forEach((listener) => listener());
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
