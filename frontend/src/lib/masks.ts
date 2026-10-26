/**
 * Máscaras de digitação. Só formatam o que a pessoa vê; quem valida é o back-end.
 */

/** Telefone com DDD: (81) 99999-9999 (celular) ou (81) 3333-4444 (fixo). Máximo de 11 números. */
export const PHONE_MAX_DIGITS = 11;
export const PHONE_MASKED_LENGTH = 15; // "(81) 99999-9999"

export function formatPhone(value: string): string {
  const digits = value.replace(/\D/g, "").slice(0, PHONE_MAX_DIGITS);
  if (digits.length === 0) return "";
  if (digits.length <= 2) return `(${digits}`;
  const ddd = digits.slice(0, 2);
  const rest = digits.slice(2);
  // Com 11 números, o celular tem 5 dígitos antes do traço; com até 10, o fixo tem 4.
  const split = digits.length === PHONE_MAX_DIGITS ? 5 : 4;
  if (rest.length <= split) return `(${ddd}) ${rest}`;
  return `(${ddd}) ${rest.slice(0, split)}-${rest.slice(split)}`;
}

/**
 * CPF (111.111.111-22) ou CNPJ (11.222.333/0001-81).
 * Desde julho de 2026 o CNPJ pode ter letras nas 12 primeiras posições; os 2 últimos são sempre números.
 * Vira CNPJ quando passa de 11 caracteres ou quando aparece uma letra.
 */
export const DOCUMENT_MAX_CHARS = 14;
export const DOCUMENT_MASKED_LENGTH = 18; // "11.222.333/0001-81"

export function formatDocument(value: string): string {
  const raw = value
    .toUpperCase()
    .replace(/[^0-9A-Z]/g, "")
    .slice(0, DOCUMENT_MAX_CHARS);
  const isCnpj = raw.length > 11 || /[A-Z]/.test(raw);
  return isCnpj ? formatCnpj(raw) : formatCpf(raw);
}

function formatCpf(digits: string): string {
  return applyPattern(digits, [3, 3, 3, 2], [".", ".", "-"]);
}

function formatCnpj(raw: string): string {
  // Os 2 dígitos verificadores (posições 13 e 14) só aceitam números.
  const base = raw.slice(0, 12);
  const check = raw.slice(12).replace(/\D/g, "");
  return applyPattern(base + check, [2, 3, 3, 4, 2], [".", ".", "/", "-"]);
}

/** Corta o texto nos tamanhos dados e junta com os separadores, sem separador sobrando no fim. */
function applyPattern(raw: string, sizes: number[], separators: string[]): string {
  const parts: string[] = [];
  let start = 0;
  for (const size of sizes) {
    if (start >= raw.length) break;
    parts.push(raw.slice(start, start + size));
    start += size;
  }
  return parts.reduce((text, part, i) => (i === 0 ? part : text + separators[i - 1] + part), "");
}
