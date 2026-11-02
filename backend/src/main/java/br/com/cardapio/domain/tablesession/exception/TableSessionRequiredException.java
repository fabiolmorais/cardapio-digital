package br.com.cardapio.domain.tablesession.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/**
 * 401: a ação da mesa (chamar, pedir) precisa da sessão do celular naquela mesa, e ela não veio,
 * venceu ou é de outra mesa. A página abre uma sessão nova sozinha ao recarregar.
 */
public class TableSessionRequiredException extends DomainException {

    public TableSessionRequiredException() {
        super("Sua sessão nesta mesa terminou. Abra o QR Code de novo.");
    }
}
