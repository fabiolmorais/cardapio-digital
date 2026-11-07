"use client";

import type { ComponentProps, MouseEvent } from "react";

/**
 * Um link para uma seção da página inicial (ou para o topo, com section="topo"). Rola até lá e
 * troca o endereço com replaceState, guardando o estado do roteador do Next.
 *
 * Por que não o <Link> do Next: com a página aberta já numa âncora (/#perguntas), ele acumula as
 * âncoras (/#perguntas#planos) e um link para "/" não faz nada. E a âncora comum (<a href="#x">)
 * cria uma entrada no histórico que o roteador não conhece: voltar do cadastro para ela troca o
 * endereço sem trocar a tela.
 */
export function SectionLink({ section, onClick, ...props }: { section: string } & Omit<ComponentProps<"a">, "href">) {
  const href = section === "topo" ? "/" : `/#${section}`;

  function jump(event: MouseEvent<HTMLAnchorElement>) {
    onClick?.(event);
    // Ctrl/Cmd+clique (nova aba) e o botão do meio seguem o padrão do navegador.
    if (
      event.defaultPrevented ||
      event.button !== 0 ||
      event.metaKey ||
      event.ctrlKey ||
      event.shiftKey ||
      event.altKey
    )
      return;
    const target = section === "topo" ? null : document.getElementById(section);
    if (section !== "topo" && !target) return;
    event.preventDefault();
    if (target) target.scrollIntoView({ behavior: "smooth", block: "start" });
    else window.scrollTo({ top: 0, behavior: "smooth" });
    window.history.replaceState(window.history.state, "", href);
  }

  return <a href={href} onClick={jump} {...props} />;
}
