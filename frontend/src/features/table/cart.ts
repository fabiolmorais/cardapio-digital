"use client";

import { useSyncExternalStore } from "react";

/** Os limites do back: até 20 de cada item, 30 linhas e 140 letras de observação. */
export const MAX_QUANTITY = 20;
export const MAX_LINES = 30;
export const MAX_NOTE = 140;

/**
 * Uma linha do carrinho. O nome, o preço e a foto servem só para mostrar: o back usa o produto,
 * a quantidade e a observação, e copia o nome e o preço do cardápio de agora.
 */
export type CartLine = {
  key: string;
  productId: string;
  name: string;
  unitPriceCents: number;
  thumb: string | null;
  quantity: number;
  note: string;
};

const EMPTY: CartLine[] = [];
const listeners = new Set<() => void>();
const cache = new Map<string, CartLine[]>();

const storageKey = (token: string) => `cart:${token}`;

/**
 * O carrinho fica no próprio celular (localStorage), um por mesa: sobrevive à troca de página e
 * ao F5, e cada celular da mesa monta o seu. Sem o localStorage (aba anônima bloqueada), vale
 * só enquanto a página está aberta.
 */
function read(token: string): CartLine[] {
  const cached = cache.get(token);
  if (cached) return cached;
  let lines = EMPTY;
  try {
    const raw = window.localStorage.getItem(storageKey(token));
    if (raw) lines = JSON.parse(raw) as CartLine[];
  } catch {
    // Sem o armazenamento, ou com lixo nele: começa vazio.
  }
  cache.set(token, lines);
  return lines;
}

function write(token: string, lines: CartLine[]) {
  cache.set(token, lines);
  try {
    if (lines.length) window.localStorage.setItem(storageKey(token), JSON.stringify(lines));
    else window.localStorage.removeItem(storageKey(token));
  } catch {
    // Fica só na memória.
  }
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  // Outra aba da mesma mesa mexeu no carrinho: esquece o que estava na memória e lê de novo.
  const onStorage = (event: StorageEvent) => {
    if (event.key?.startsWith("cart:")) {
      cache.delete(event.key.slice("cart:".length));
      listener();
    }
  };
  window.addEventListener("storage", onStorage);
  return () => {
    listeners.delete(listener);
    window.removeEventListener("storage", onStorage);
  };
}

/** O carrinho desta mesa. No servidor (e na primeira pintura), vazio. */
export function useCart(token: string) {
  const lines = useSyncExternalStore(subscribe, () => read(token), () => EMPTY);
  const count = lines.reduce((sum, line) => sum + line.quantity, 0);
  const totalCents = lines.reduce((sum, line) => sum + line.quantity * line.unitPriceCents, 0);
  return { lines, count, totalCents };
}

/**
 * Põe no carrinho. O mesmo produto com a mesma observação soma na linha que já existe; com outra
 * observação, vira outra linha (um guioza "sem cebolinha" e outro normal).
 */
export function addToCart(token: string, line: Omit<CartLine, "key">): boolean {
  const note = line.note.trim().slice(0, MAX_NOTE);
  const key = `${line.productId}|${note}`;
  const lines = read(token);
  const existing = lines.find((l) => l.key === key);
  if (existing) {
    write(
      token,
      lines.map((l) => (l.key === key ? { ...l, quantity: Math.min(MAX_QUANTITY, l.quantity + line.quantity) } : l)),
    );
    return true;
  }
  if (lines.length >= MAX_LINES) return false;
  write(token, [...lines, { ...line, note, key, quantity: Math.min(MAX_QUANTITY, line.quantity) }]);
  return true;
}

/** Muda a quantidade; zero tira a linha. */
export function setQuantity(token: string, key: string, quantity: number) {
  const lines = read(token);
  write(
    token,
    quantity <= 0
      ? lines.filter((l) => l.key !== key)
      : lines.map((l) => (l.key === key ? { ...l, quantity: Math.min(MAX_QUANTITY, quantity) } : l)),
  );
}

/** Troca a observação de uma linha, no mesmo lugar. Se ficar igual a outra linha, as duas se juntam. */
export function setNote(token: string, key: string, text: string) {
  const lines = read(token);
  const line = lines.find((l) => l.key === key);
  if (!line) return;
  const note = text.trim().slice(0, MAX_NOTE);
  const newKey = `${line.productId}|${note}`;
  if (newKey === key) return;
  const twin = lines.find((l) => l.key === newKey);
  write(
    token,
    twin
      ? lines
          .filter((l) => l.key !== key)
          .map((l) => (l === twin ? { ...l, quantity: Math.min(MAX_QUANTITY, l.quantity + line.quantity) } : l))
      : lines.map((l) => (l.key === key ? { ...l, key: newKey, note } : l)),
  );
}

export function clearCart(token: string) {
  write(token, EMPTY);
}

const noop = () => () => {};

/** Falso no servidor e na primeira pintura; verdadeiro depois, quando o carrinho já foi lido. */
export function useHydrated() {
  return useSyncExternalStore(noop, () => true, () => false);
}
