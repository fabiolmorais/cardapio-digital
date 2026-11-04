import type { PublicMenu } from "./table-data";

/**
 * O cardápio de sempre: sem os produtos só do rodízio (eles ficam na aba Rodízio, sem preço), e
 * sem as categorias que ficaram vazias por isso.
 */
export function aLaCarte(menu: PublicMenu): PublicMenu["categories"] {
  return menu.categories
    .map((c) => ({ ...c, products: c.products.filter((p) => p.aLaCarte) }))
    .filter((c) => c.products.length > 0);
}
