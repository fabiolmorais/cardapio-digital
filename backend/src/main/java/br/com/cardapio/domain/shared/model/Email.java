package br.com.cardapio.domain.shared.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_EMAIL_LENGTH = 254;

    public Email {
        if (value == null) {
            throw new InvalidValueException("Informe o e-mail");
        }

        value = value.trim().toLowerCase(Locale.ROOT);

        if (value.length() > MAX_EMAIL_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new InvalidValueException("E-mail inválido");
        }
    }
}
