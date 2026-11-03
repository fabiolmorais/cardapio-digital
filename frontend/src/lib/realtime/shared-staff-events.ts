import type { ServerEvent } from "./sse-parser";
import { connectStaffEvents } from "./staff-events";

type Listener = { onEvent: (event: ServerEvent) => void; onConnectedChange: (connected: boolean) => void };

const listeners = new Set<Listener>();
let disconnect: (() => void) | null = null;
let connected = false;

/**
 * O canal de tempo real da equipe, UM por aba, repartido entre as listas da tela (os chamados,
 * as entregas, os pedidos para confirmar). Abre com a primeira lista e fecha com a última: sem
 * isso, cada lista abriria a sua conexão, e o navegador limita quantas ficam abertas por site.
 *
 * Mesma assinatura do connectStaffEvents: devolve a função que desliga esta lista.
 */
export function subscribeStaffEvents(
  onEvent: (event: ServerEvent) => void,
  onConnectedChange: (connected: boolean) => void,
): () => void {
  const listener = { onEvent, onConnectedChange };
  listeners.add(listener);
  if (disconnect) {
    onConnectedChange(connected);
  } else {
    disconnect = connectStaffEvents(
      (event) => listeners.forEach((l) => l.onEvent(event)),
      (next) => {
        connected = next;
        listeners.forEach((l) => l.onConnectedChange(next));
      },
    );
  }
  return () => {
    listeners.delete(listener);
    onConnectedChange(false);
    if (listeners.size === 0 && disconnect) {
      const stop = disconnect;
      disconnect = null;
      connected = false;
      stop();
    }
  };
}
