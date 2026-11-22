package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.domain.order.model.TabPeople;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Optional;

@Schema(name = "TabPeople", description = "Pessoas na mesa fora do rodízio, informado pelo garçom")
record TabPeopleResponse(@Schema(description = "null: ninguém informou") Integer people) {

    static TabPeopleResponse of(Optional<TabPeople> people) {
        return new TabPeopleResponse(people.map(TabPeople::value).orElse(null));
    }
}
