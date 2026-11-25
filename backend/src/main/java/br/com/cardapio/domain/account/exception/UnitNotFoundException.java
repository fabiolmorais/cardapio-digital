package br.com.cardapio.domain.account.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

/**
 * A unidade não existe ou a pessoa não tem vínculo ativo com ela (404). A resposta é a mesma nos
 * dois casos, para não revelar os restaurantes de outras empresas.
 */
public class UnitNotFoundException extends NotFoundException {

    public UnitNotFoundException() {
        super("Unidade não encontrada");
    }
}
