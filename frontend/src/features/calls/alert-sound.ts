/**
 * O alerta sonoro dos chamados, gerado na hora (Web Audio), sem arquivo de som para baixar.
 *
 * O navegador só deixa uma página tocar som depois de um toque nela. Por isso o som começa
 * desligado, e o botão "Ativar som" chama enableSound() dentro do toque: é ali que o
 * AudioContext é criado e liberado. Depois disso, playChime() toca sozinho a cada chamado.
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

/** "Ding-dong": duas notas curtas, fáceis de ouvir num salão barulhento. */
export function playChime() {
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
