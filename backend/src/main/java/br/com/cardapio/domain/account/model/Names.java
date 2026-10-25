package br.com.cardapio.domain.account.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

/** Regra comum para nomes: obrigatório, sem espaços nas pontas e até 120 caracteres. */
final class Names {
    static final int MAX = 120;

    private Names() {
    }

    static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException(message);
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX) {
            throw new InvalidValueException("Nome longo demais (máximo " + MAX + " caracteres)");
        }
        return trimmed;
    }
}
