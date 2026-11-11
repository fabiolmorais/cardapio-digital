package br.com.cardapio.domain.tablesession.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

/** O celular (a sessão da mesa) não existe ou é de outro restaurante. */
public class TableSessionNotFoundException extends NotFoundException {

    public TableSessionNotFoundException() {
        super("Celular não encontrado");
    }
}
