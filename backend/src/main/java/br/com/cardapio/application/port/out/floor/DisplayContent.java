package br.com.cardapio.application.port.out.floor;

/** O que vai impresso no display de uma mesa. url é o endereço que o QR Code abre. */
public record DisplayContent(String establishmentName, String tableLabel, String url) {
}
