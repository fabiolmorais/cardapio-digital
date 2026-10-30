const brl = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

/** Formata centavos em reais (ex.: 6990 → "R$ 69,90"). */
export function formatCents(cents: number): string {
  return brl.format(cents / 100);
}

const twoDecimals = new Intl.NumberFormat("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** Centavos para o campo de preço, sem o "R$" (ex.: 2790 → "27,90"). */
export function centsToInput(cents: number): string {
  return twoDecimals.format(cents / 100);
}

/**
 * Lê o que a pessoa digitou no campo de preço e devolve centavos, sem passar por número
 * com vírgula (que perderia precisão). Aceita "27,90", "27.90", "27", "1.234,5" e "R$ 27,90".
 * Vazio vira null; texto que não é preço vira NaN.
 */
export function parseCents(text: string): number | null {
  const clean = text.replace(/R\$|\s/g, "");
  if (clean === "") return null;
  // A última vírgula ou ponto seguida de 1 ou 2 dígitos é a dos centavos; os outros pontos são de milhar.
  const match = /^(\d{1,3}(?:\.\d{3})+|\d+)(?:[.,](\d{1,2}))?$/.exec(clean);
  if (!match) return Number.NaN;
  const reais = match[1].replaceAll(".", "");
  const cents = (match[2] ?? "").padEnd(2, "0");
  return Number(reais) * 100 + Number(cents);
}
