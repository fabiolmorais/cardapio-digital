package br.com.cardapio.domain.report.model;

/**
 * Do QR ao pedido (REL-64): as comandas fechadas no período, as que tiveram algum celular na mesa e
 * as que tiveram pedido pelo celular.
 */
public record PhoneFunnel(int tabs, int scanned, int phoneOrdered) {
}
