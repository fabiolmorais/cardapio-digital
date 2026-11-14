import { choicesText, type ItemChoices } from "./choices";

/** O tamanho e as escolhas embaixo do nome do item ("Grande · ½ Calabresa · ½ Frango"). */
export function Choices({ of }: { of?: ItemChoices }) {
  const text = choicesText(of);
  return text ? <span className="text-[13px] text-(--mesa-muted)">{text}</span> : null;
}
