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
    // A barra de abas flutuante (vidro mais fechado) e o verde de "No salão".
    "--mesa-bar": dark ? "rgba(28,25,23,.78)" : "rgba(255,255,255,.88)",
    "--mesa-ok": dark ? "#4ADE80" : "#15803D",
    "--mesa-ok-soft": dark ? "rgba(74,222,128,.14)" : "rgba(22,163,74,.12)",
    // Os avisos, como no desenho: azul = informativo, amarelo = atenção, vermelho = importante.
    "--mesa-info": dark ? "#7DB4FF" : "#1D4ED8",
    "--mesa-info-soft": dark ? "rgba(96,165,250,.16)" : "rgba(37,99,235,.10)",
    "--mesa-warn": dark ? "#FBBF24" : "#A16207",
    "--mesa-warn-soft": dark ? "rgba(251,191,36,.14)" : "rgba(234,179,8,.16)",
    "--mesa-danger": dark ? "#FF8A8A" : "#B91C1C",
    "--mesa-danger-soft": dark ? "rgba(248,113,113,.16)" : "rgba(220,38,38,.10)",
    "--brand": primary,
    "--brand-text": accent,
    colorScheme: dark ? "dark" : "light",
  } as CSSProperties;
}
