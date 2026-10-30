"use client";

import { useRef, useState, type KeyboardEvent, type PointerEvent } from "react";

type Item = { id: string };

/**
 * Reordenar uma lista arrastando uma alça, sem biblioteca: eventos de ponteiro (mouse, toque
 * e caneta funcionam igual) e, para quem usa teclado ou leitor de tela, as setas para cima e
 * para baixo com a alça selecionada.
 *
 * Enquanto arrasta, a ordem nova só existe aqui (draft). Ao soltar, onCommit recebe todos os
 * ids na ordem nova (o formato que o back espera) e a lista volta a seguir `items` quando o
 * salvamento termina, com sucesso ou não.
 */
export function useReorder<T extends Item>(
  items: T[],
  onCommit: (ids: string[]) => Promise<void>,
  describe: (item: T, position: number, total: number) => string,
) {
  const [draft, setDraft] = useState<string[] | null>(null);
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const [announcement, setAnnouncement] = useState("");
  const rows = useRef(new Map<string, HTMLElement>());

  const byId = new Map(items.map((item) => [item.id, item]));
  const order = draft ?? items.map((item) => item.id);
  const ordered = order.flatMap((id) => byId.get(id) ?? []);

  async function commit(ids: string[]) {
    const unchanged = ids.length === items.length && ids.every((id, i) => id === items[i]?.id);
    if (unchanged) {
      setDraft(null);
      return;
    }
    setDraft(ids);
    try {
      await onCommit(ids);
    } finally {
      setDraft(null);
    }
  }

  function move(ids: string[], id: string, to: number) {
    const next = ids.filter((x) => x !== id);
    next.splice(to, 0, id);
    return next;
  }

  /** Onde o item cairia: quantas das outras linhas têm o meio acima do ponteiro. */
  function targetIndex(ids: string[], id: string, clientY: number) {
    return ids
      .filter((x) => x !== id)
      .filter((x) => {
        const rect = rows.current.get(x)?.getBoundingClientRect();
        return rect !== undefined && rect.top + rect.height / 2 < clientY;
      }).length;
  }

  function handleProps(item: T) {
    return {
      onPointerDown(event: PointerEvent<HTMLElement>) {
        if (event.button !== 0 || draft) return;
        event.currentTarget.setPointerCapture(event.pointerId);
        setDraggingId(item.id);
        setDraft(items.map((x) => x.id));
      },
      onPointerMove(event: PointerEvent<HTMLElement>) {
        if (draggingId !== item.id || !draft) return;
        const to = targetIndex(draft, item.id, event.clientY);
        if (draft.indexOf(item.id) !== to) setDraft(move(draft, item.id, to));
      },
      onPointerUp() {
        if (draggingId !== item.id || !draft) return;
        setDraggingId(null);
        void commit(draft);
      },
      onPointerCancel() {
        setDraggingId(null);
        setDraft(null);
      },
      onKeyDown(event: KeyboardEvent<HTMLElement>) {
        if (event.key !== "ArrowUp" && event.key !== "ArrowDown") return;
        event.preventDefault();
        if (draft) return; // ainda salvando a mudança anterior
        const ids = items.map((x) => x.id);
        const from = ids.indexOf(item.id);
        const to = event.key === "ArrowUp" ? from - 1 : from + 1;
        if (to < 0 || to >= ids.length) return;
        setAnnouncement(describe(item, to + 1, ids.length));
        void commit(move(ids, item.id, to));
      },
    };
  }

  /** Para o hook medir a linha durante o arraste. */
  function rowRef(id: string) {
    return (element: HTMLElement | null) => {
      if (element) rows.current.set(id, element);
      else rows.current.delete(id);
    };
  }

  return { ordered, draggingId, saving: draft !== null && draggingId === null, announcement, handleProps, rowRef };
}
