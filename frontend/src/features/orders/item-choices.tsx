import { choicesText, type ItemChoices } from "@/features/table/choices";
import { cn } from "cn";

/**
 * O tamanho e as escolhas de um item (ADR 0010), embaixo do nome, nas telas da equipe: "Grande ·
 * ½ Calabresa · ½ Frango com catupiry · Catupiry". Item sem escolhas não mostra nada.
 */
export function ItemChoicesText({ of, className }: { of?: ItemChoices; className?: string }) {
  const text = choicesText(of);
  return text ? <span className={cn("block text-[13px] text-muted-foreground", className)}>{text}</span> : null;
}
