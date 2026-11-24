/**
 * A escolha da taxa de serviço na tela (RF39): o percentual do restaurante, sem serviço ou outro
 * valor (quando o restaurante permite). A mesa e o garçom usam a mesma conta: o que falta pagar é o
 * consumo mais o serviço escolhido, menos o que já foi pago.
 */
export type ServicePick = { kind: "PERCENT" } | { kind: "NONE" } | { kind: "CUSTOM"; cents: number | null };

/** O que a tela precisa saber da oferta que veio do back. */
type Offer = { accepted: boolean; customCents?: number | null; percentAmountCents: number };

/** Os atalhos de outro valor: R$ 5 e R$ 10. */
export const SERVICE_SHORTCUTS = [500, 1000] as const;

/** O maior outro valor aceito pelo back (R$ 1.000,00). */
export const MAX_CUSTOM_CENTS = 100_000;

/** A escolha como está gravada na comanda. */
export function pickOf(offer: Offer): ServicePick {
  if (!offer.accepted) return { kind: "NONE" };
  return offer.customCents != null ? { kind: "CUSTOM", cents: offer.customCents } : { kind: "PERCENT" };
}

/** O valor do serviço nesta escolha (zero sem serviço ou com outro valor ainda vazio). */
export function pickCents(offer: Offer, pick: ServicePick): number {
  if (pick.kind === "PERCENT") return offer.percentAmountCents;
  if (pick.kind === "CUSTOM") return validCustom(pick.cents) ? pick.cents : 0;
  return 0;
}

/** O outro valor digitado vale (de R$ 0,01 a R$ 1.000,00)? */
export function validCustom(cents: number | null): cents is number {
  return cents !== null && Number.isInteger(cents) && cents >= 1 && cents <= MAX_CUSTOM_CENTS;
}

/** A escolha pode ir para o back (outro valor vazio ou inválido, não). */
export function pickReady(pick: ServicePick): boolean {
  return pick.kind !== "CUSTOM" || validCustom(pick.cents);
}

/** O que falta pagar com esta escolha. */
export function dueWith(offer: Offer, pick: ServicePick, totalCents: number, paidCents: number): number {
  return Math.max(totalCents + pickCents(offer, pick) - paidCents, 0);
}

/** Os campos do pedido para o back. */
export function pickBody(pick: ServicePick): { serviceAccepted: boolean; serviceCustomCents?: number } {
  if (pick.kind === "CUSTOM" && validCustom(pick.cents))
    return { serviceAccepted: true, serviceCustomCents: pick.cents };
  return { serviceAccepted: pick.kind !== "NONE" };
}

/** As duas escolhas são a mesma? */
export function samePick(a: ServicePick, b: ServicePick): boolean {
  return a.kind === b.kind && (a.kind !== "CUSTOM" || a.cents === (b as { cents: number | null }).cents);
}
