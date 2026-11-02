package br.com.cardapio.application.port.out.floor;

import java.util.Objects;

/**
 * O que vai impresso no display de uma mesa. url é o endereço que o QR Code abre.
 *
 * @param accentColor a cor da marca para texto em fundo branco ("#B8361A")
 * @param logoPng     a logo pequena em PNG; null quando o restaurante usa as iniciais
 */
public record DisplayContent(String establishmentName, String tableLabel, String url, String accentColor,
                             byte[] logoPng) {

    public DisplayContent {
        Objects.requireNonNull(accentColor);
    }
}
