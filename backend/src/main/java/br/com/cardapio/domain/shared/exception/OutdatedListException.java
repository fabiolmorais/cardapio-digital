package br.com.cardapio.domain.shared.exception;

/**
 * A tela mandou uma nova ordem para uma lista que mudou desde que ela carregou (alguém criou
 * ou apagou um item noutra aba). É um 409: o pedido brigou com o estado atual.
 */
public class OutdatedListException extends ConflictException {

    public OutdatedListException() {
        super("A lista mudou enquanto você organizava. Recarregue a página e tente de novo.");
    }
}
