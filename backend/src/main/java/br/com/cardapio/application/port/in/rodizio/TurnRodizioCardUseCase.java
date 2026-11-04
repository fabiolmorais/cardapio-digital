package br.com.cardapio.application.port.in.rodizio;

import br.com.cardapio.domain.rodizio.model.TabRodizioStatus;

/** O celular da mesa vira o cartão verde/vermelho. */
public interface TurnRodizioCardUseCase {

    TableRodizio turn(String tableToken, String sessionToken, TabRodizioStatus status);
}
