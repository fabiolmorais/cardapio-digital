package br.com.cardapio.domain.shared.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

/**
 * Telefone brasileiro com DDD, guardado só com dígitos (10 ou 11), sem o +55.
 */
public record PhoneNumber(String value) {

    public PhoneNumber {
        if (value == null) {
            throw new InvalidValueException("Informe o telefone");
        }
        value = value.replaceAll("\\D", "");
        if (value.length() > 11 && value.startsWith("55")) {
            value = value.substring(2);
        }
        if (!value.matches("[1-9]{2}\\d{8,9}")) {
            throw new InvalidValueException("Telefone inválido: use DDD + número");
        }
    }
}
