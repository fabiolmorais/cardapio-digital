package br.com.cardapio.application.port.in.rodizio;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;

/** Os rodízios ativos, por nome, com o preço de agora. */
public interface QuoteRodiziosUseCase {

    /** Para a equipe (o diálogo "Ativar rodízio"). */
    ResultPage<RodizioQuote> forStaff(EstablishmentId establishmentId, PageQuery page);

    /** Para o celular da mesa ("Escolha o rodízio"): precisa da sessão, como os pedidos. */
    ResultPage<RodizioQuote> forTable(String tableToken, String sessionToken, PageQuery page);
}
