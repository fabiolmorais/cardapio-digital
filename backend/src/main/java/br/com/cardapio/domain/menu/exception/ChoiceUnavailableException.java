package br.com.cardapio.domain.menu.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/**
 * 409: um tamanho ou uma opção escolhida saiu do cardápio ou esgotou entre a escolha e o envio.
 * O cliente escolhe de novo.
 */
public class ChoiceUnavailableException extends ConflictException {

    public ChoiceUnavailableException(String productName, String choiceName) {
        super(choiceName == null
                ? "Uma escolha de " + productName + " não está mais no cardápio. Escolha de novo."
                : productName + ": " + choiceName + " acabou. Escolha outra opção.");
    }
}
