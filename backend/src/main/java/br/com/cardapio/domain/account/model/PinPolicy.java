package br.com.cardapio.domain.account.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

/**
 * Regra do PIN da equipe: 4 a 6 dígitos, sem os PINs óbvios (todos iguais ou em sequência).
 * O PIN é curto de propósito (digitado no salão, com pressa); quem o protege é o aparelho
 * vinculado e o limite de tentativas, não o tamanho.
 */
public final class PinPolicy {

    public static final int MIN_LENGTH = 4;
    public static final int MAX_LENGTH = 6;

    private PinPolicy() {
    }

    public static void check(String rawPin) {
        if (rawPin == null || !rawPin.matches("\\d{" + MIN_LENGTH + "," + MAX_LENGTH + "}")) {
            throw new InvalidValueException("O PIN precisa ter de " + MIN_LENGTH + " a " + MAX_LENGTH + " números");
        }
        if (allSameDigit(rawPin) || isSequence(rawPin, 1) || isSequence(rawPin, -1)) {
            throw new InvalidValueException("Esse PIN é fácil de adivinhar. Escolha outro");
        }
    }

    private static boolean allSameDigit(String pin) {
        return pin.chars().allMatch(c -> c == pin.charAt(0));
    }

    /** 1234, 456789 (step 1) ou 9876, 4321 (step -1). */
    private static boolean isSequence(String pin, int step) {
        for (int i = 1; i < pin.length(); i++) {
            if (pin.charAt(i) - pin.charAt(i - 1) != step) {
                return false;
            }
        }
        return true;
    }
}
