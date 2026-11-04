package br.com.cardapio.application.port.in.rodizio;

import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.rodizio.model.PriceTier;
import br.com.cardapio.domain.rodizio.model.RodizioRules;
import br.com.cardapio.domain.rodizio.model.RodizioType;

import java.util.List;
import java.util.Set;

/** O rodízio inteiro, como o formulário do admin manda (na criação e na edição). */
public record RodizioForm(String name, RodizioType type, String description, RodizioRules rules,
                          List<PriceTier> tiers, Set<ProductId> productIds) {

    public RodizioForm {
        tiers = List.copyOf(tiers);
        productIds = Set.copyOf(productIds);
    }
}
