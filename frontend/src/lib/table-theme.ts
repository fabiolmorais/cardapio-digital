import type { CSSProperties } from "react";

/**
 * As cores da página da mesa como variáveis CSS: o fundo e o texto do tema (escuro ou claro) e
 * os dois tons da marca que vêm da API. O Tailwind usa as variáveis (bg-(--mesa-bg)…), porque
 * não gera classes para cores que só se conhecem na hora. Serve para a página de verdade e para
 * a prévia na tela "Marca e aparência".
 */
export function tableThemeVars(theme: "DARK" | "LIGHT", primary: string, accent: string): CSSProperties {
  const dark = theme === "DARK";
  return {
    "--mesa-bg": dark ? "#0F0D0C" : "#F7F4F0",
    "--mesa-fg": dark ? "#F8F5F1" : "#16130F",
    "--mesa-muted": dark ? "#BDB5AC" : "#5C554E",
    "--mesa-glass": dark ? "rgba(255,255,255,.08)" : "rgba(255,255,255,.85)",
    "--mesa-stroke": dark ? "rgba(255,255,255,.12)" : "rgba(22,19,15,.09)",
    "--brand": primary,
    "--brand-text": accent,
    colorScheme: dark ? "dark" : "light",
  } as CSSProperties;
}
