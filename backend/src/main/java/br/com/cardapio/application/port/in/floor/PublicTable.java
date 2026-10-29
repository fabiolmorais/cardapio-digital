package br.com.cardapio.application.port.in.floor;

/** O que a página pública da mesa mostra por enquanto: de que restaurante e qual mesa. */
public record PublicTable(String establishmentName, String tableLabel) {
}
