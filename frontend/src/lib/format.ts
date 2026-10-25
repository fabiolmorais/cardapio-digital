const brl = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

/** Formata centavos em reais (ex.: 6990 → "R$ 69,90"). */
export function formatCents(cents: number): string {
  return brl.format(cents / 100);
}
