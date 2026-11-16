package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.OptionGroupId;

import java.util.List;

public interface UpdateOptionGroupPricesUseCase {

    /**
     * Troca a grade inteira: os preços das opções do grupo em todos os produtos passam a ser estes
     * (o que não vier fica em branco, e vale o preço da opção).
     */
    OptionGroupPrices update(EstablishmentId establishmentId, OptionGroupId groupId, List<OptionGroupPrices.Price> prices);
}
