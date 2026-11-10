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

/**
 * A vitrine do topo (início e cardápio): os "Mais pedidos" do último mês, que o back calcula, ou,
 * enquanto não há pedidos bastantes, os destaques disponíveis.
 */
export function showcase(menu: PublicMenu) {
  const products = aLaCarte(menu).flatMap((c) => c.products);
  const byId = new Map(products.map((p) => [p.id, p]));
  const mostOrdered = (menu.mostOrdered ?? []).flatMap((id) => byId.get(id) ?? []);
  return mostOrdered.length > 0
    ? { items: mostOrdered, mostOrdered: true }
    : {
        items: products.filter((p) => p.featured && p.available),
        mostOrdered: false,
      };
}
