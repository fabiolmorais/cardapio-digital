import {
  Beef,
  Beer,
  CakeSlice,
  CupSoda,
  Fish,
  IceCreamCone,
  Pizza,
  Salad,
  Sandwich,
  Soup,
  UtensilsCrossed,
  Wine,
  type LucideIcon,
} from "lucide-react";

/**
 * O ícone de uma categoria na página da mesa (desenho 2.1), escolhido pelo nome que o restaurante
 * deu a ela. Só apresentação: um nome que não casa com nada fica com os talheres.
 */
const RULES: [RegExp, LucideIcon][] = [
  [/sushi|sashimi|temaki|uramaki|niguiri|nigiri|hossomaki|combinado|peixe|frutos do mar|camar/, Fish],
  [/quente|sopa|lamen|ramen|caldo|yakisoba|tempura/, Soup],
  [/cerveja|chopp?|chope/, Beer],
  [/vinho|drink|coquetel|caipirinha|sake|saque/, Wine],
  [/bebida|suco|refri|agua|cafe|cha\b/, CupSoda],
  [/sorvete|gelato|acai/, IceCreamCone],
  [/doce|sobremesa|bolo|torta/, CakeSlice],
  [/entrada|salada|petisco|porc/, Salad],
  [/pizza/, Pizza],
  [/lanche|burger|sanduiche/, Sandwich],
  [/carne|churrasco|grelhad|espeto/, Beef],
];

export function CategoryIcon({ name, className }: { name: string; className?: string }) {
  const plain = name
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase();
  const Icon = RULES.find(([pattern]) => pattern.test(plain))?.[1] ?? UtensilsCrossed;
  return <Icon aria-hidden className={className} strokeWidth={1.8} />;
}
