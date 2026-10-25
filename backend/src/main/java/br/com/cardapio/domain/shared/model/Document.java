package br.com.cardapio.domain.shared.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Locale;

/**
 * CPF (11 dígitos) ou CNPJ (14 caracteres), guardado sem pontuação.
 * Desde julho de 2026 o CNPJ pode ter letras nas 12 primeiras posições;
 * os 2 dígitos verificadores continuam numéricos.
 */
public record Document(String value) {

    private static final int[] CNPJ_WEIGHTS_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] CNPJ_WEIGHTS_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public Document {
        if (value == null) {
            throw new InvalidValueException("Informe o CPF ou CNPJ");
        }
        value = value.toUpperCase(Locale.ROOT).replaceAll("[^0-9A-Z]", "");
        boolean valid = isCpf(value) ? validCpf(value) : isCnpj(value) && validCnpj(value);
        if (!valid) {
            throw new InvalidValueException("CPF ou CNPJ inválido");
        }
    }

    public boolean isCompany() {
        return value.length() == 14;
    }

    private static boolean isCpf(String v) {
        return v.matches("\\d{11}");
    }

    private static boolean isCnpj(String v) {
        return v.matches("[0-9A-Z]{12}\\d{2}");
    }

    private static boolean allSameChar(String v) {
        return v.chars().distinct().count() == 1;
    }

    private static boolean validCpf(String v) {
        if (allSameChar(v)) {
            return false;
        }
        return cpfDigit(v, 9) == v.charAt(9) - '0' && cpfDigit(v, 10) == v.charAt(10) - '0';
    }

    private static int cpfDigit(String v, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (v.charAt(i) - '0') * (length + 1 - i);
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }

    private static boolean validCnpj(String v) {
        if (allSameChar(v)) {
            return false;
        }
        return cnpjDigit(v, CNPJ_WEIGHTS_1) == v.charAt(12) - '0'
                && cnpjDigit(v, CNPJ_WEIGHTS_2) == v.charAt(13) - '0';
    }

    /** Cada caractere vale (código ASCII − 48): '0' = 0 … '9' = 9, 'A' = 17 … 'Z' = 42. */
    private static int cnpjDigit(String v, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (v.charAt(i) - '0') * weights[i];
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }
}
