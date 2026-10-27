package br.com.cardapio.domain.account.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

/**
 * Regra do PIN da equipe: exatamente 6 dígitos, sem os PINs óbvios (todos iguais ou em
 * sequência). Tamanho fixo deixa a tela simples (6 bolinhas, entra ao digitar o último) e
 * dá 1 milhão de combinações; junto com o aparelho vinculado e o limite de tentativas, basta.
 */
public final class PinPolicy {

    public static final int LENGTH = 6;

    private PinPolicy() {
    }

    public static void check(String rawPin) {
        if (rawPin == null || !rawPin.matches("\\d{" + LENGTH + "}")) {
            throw new InvalidValueException("O PIN precisa ter " + LENGTH + " números");
        }
        if (allSameDigit(rawPin) || isSequence(rawPin, 1) || isSequence(rawPin, -1)) {
            throw new InvalidValueException("Esse PIN é fácil de adivinhar. Escolha outro");
        }
    }

    private static boolean allSameDigit(String pin) {
        return pin.chars().allMatch(c -> c == pin.charAt(0));
    }

    /** 123456, 345678 (step 1) ou 987654, 654321 (step -1). */
    private static boolean isSequence(String pin, int step) {
        for (int i = 1; i < pin.length(); i++) {
            if (pin.charAt(i) - pin.charAt(i - 1) != step) {
                return false;
            }
        }
        return true;
    }
}
