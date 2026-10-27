package br.com.cardapio.domain.device.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

/**
 * Código de 6 dígitos que o aparelho novo mostra na tela e o gerente digita no painel.
 * É curto para ser digitado; quem o protege é a validade curta (10 minutos) e o fato de que
 * só dono e gerente logados conseguem usá-lo.
 */
public record PairingCode(String value) {

    public static final int LENGTH = 6;

    public PairingCode {
        if (value == null || !value.matches("\\d{" + LENGTH + "}")) {
            throw new InvalidValueException("O código do aparelho tem " + LENGTH + " números");
        }
    }
}
