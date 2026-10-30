"use client";

import { useEffect, useRef, useState, type CSSProperties, type KeyboardEvent, type PointerEvent } from "react";

type Item = { id: string };

/** O arraste em andamento: medidas tiradas no início, para a conta não depender do DOM mexendo. */
type Drag = {
  id: string;
  from: number;
  startY: number;
  /** Centro de cada linha (na ordem original) no começo do arraste. */
  centers: number[];
  /** Quanto os vizinhos andam para abrir espaço: a altura da linha arrastada mais o espaço entre linhas. */
  slot: number;
};

/**
 * Reordenar uma lista arrastando uma alça, sem biblioteca, com mouse, toque ou caneta; e,
 * para quem usa teclado ou leitor de tela, as setas para cima e para baixo com a alça selecionada.
 *
 * Durante o arraste, a ordem no DOM não muda: a linha arrastada segue o ponteiro com
 * `transform`, e as outras deslizam para abrir espaço. (Mover o elemento no DOM no meio do
 * arraste faz o navegador perder o ponteiro, e o arraste trava.) Ao soltar, onCommit recebe
 * todos os ids na ordem nova, o formato que o back espera.
 */
export function useReorder<T extends Item>(
  items: T[],
  onCommit: (ids: string[]) => Promise<void>,
  describe: (item: T, position: number, total: number) => string,
) {
  const [drag, setDrag] = useState<Drag | null>(null);
  const [offset, setOffset] = useState(0);
  const [saved, setSaved] = useState<string[] | null>(null);
  const [announcement, setAnnouncement] = useState("");
  const rows = useRef(new Map<string, HTMLElement>());

  const byId = new Map(items.map((item) => [item.id, item]));
  const ordered = (saved ?? items.map((item) => item.id)).flatMap((id) => byId.get(id) ?? []);

  /** Onde a linha arrastada cairia: quantas das outras têm o centro acima do centro dela. */
  function targetIndex(state: Drag, dy: number) {
    const center = state.centers[state.from] + dy;
    return state.centers.filter((c, i) => i !== state.from && c < center).length;
  }

  async function commit(ids: string[]) {
    const unchanged = ids.every((id, i) => id === items[i]?.id);
    if (unchanged) return;
    setSaved(ids); // a ordem nova aparece já, enquanto o back grava
    try {
      await onCommit(ids);
    } finally {
      setSaved(null);
    }
  }

  // Enquanto arrasta, ouve o ponteiro na janela inteira: sair de cima da alça não perde o arraste.
  useEffect(() => {
    if (!drag) return;
    let dy = 0;
    const move = (event: globalThis.PointerEvent) => {
      dy = event.clientY - drag.startY;
      setOffset(dy);
    };
    const drop = () => {
      const ids = items.map((item) => item.id);
      const to = targetIndex(drag, dy);
      const next = ids.filter((id) => id !== drag.id);
      next.splice(to, 0, drag.id);
      setDrag(null);
      setOffset(0);
      const item = byId.get(drag.id);
      if (item && to !== drag.from) setAnnouncement(describe(item, to + 1, ids.length));
      void commit(next);
    };
    const cancel = () => {
      setDrag(null);
      setOffset(0);
    };
    window.addEventListener("pointermove", move);
    window.addEventListener("pointerup", drop);
    window.addEventListener("pointercancel", cancel);
    return () => {
      window.removeEventListener("pointermove", move);
      window.removeEventListener("pointerup", drop);
      window.removeEventListener("pointercancel", cancel);
    };
    // As funções acima leem items e drag do momento em que o arraste começou, de propósito.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [drag]);

  function handleProps(item: T) {
    return {
      onPointerDown(event: PointerEvent<HTMLElement>) {
        if (event.button !== 0 || saved || drag) return;
        event.preventDefault(); // sem seleção de texto nem rolagem começando junto
        const rects = items.map((x) => rows.current.get(x.id)?.getBoundingClientRect());
        if (rects.some((r) => r === undefined)) return;
        const measured = rects as DOMRect[];
        const from = items.findIndex((x) => x.id === item.id);
        const gap = measured.length > 1 ? Math.max(0, measured[1].top - measured[0].bottom) : 0;
        setDrag({
          id: item.id,
          from,
          startY: event.clientY,
          centers: measured.map((r) => r.top + r.height / 2),
          slot: measured[from].height + gap,
        });
        setOffset(0);
      },
      onKeyDown(event: KeyboardEvent<HTMLElement>) {
        if (event.key !== "ArrowUp" && event.key !== "ArrowDown") return;
        event.preventDefault();
        if (saved || drag) return; // ainda salvando a mudança anterior
        const ids = items.map((x) => x.id);
        const from = ids.indexOf(item.id);
        const to = event.key === "ArrowUp" ? from - 1 : from + 1;
        if (to < 0 || to >= ids.length) return;
        const next = ids.filter((id) => id !== item.id);
        next.splice(to, 0, item.id);
        setAnnouncement(describe(item, to + 1, ids.length));
        void commit(next);
      },
    };
  }

  /** O deslocamento de cada linha durante o arraste (e nada fora dele). */
  function rowStyle(id: string): CSSProperties | undefined {
    if (!drag) return undefined;
    if (id === drag.id) {
      return { transform: `translateY(${offset}px)`, position: "relative", zIndex: 10 };
    }
    const index = items.findIndex((x) => x.id === id);
    const to = targetIndex(drag, offset);
    let shift = 0;
    if (drag.from < to && index > drag.from && index <= to) shift = -drag.slot;
    if (to < drag.from && index >= to && index < drag.from) shift = drag.slot;
    return { transform: `translateY(${shift}px)`, transition: "transform 150ms ease" };
  }

  /** Para o hook medir a linha quando o arraste começa. */
  function rowRef(id: string) {
    return (element: HTMLElement | null) => {
      if (element) rows.current.set(id, element);
      else rows.current.delete(id);
    };
  }

  return {
    ordered,
    draggingId: drag?.id ?? null,
    saving: saved !== null,
    announcement,
    handleProps,
    rowRef,
    rowStyle,
  };
}
