package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.menu.model.OptionGroupRules;
import br.com.cardapio.domain.menu.model.OptionItemId;
import br.com.cardapio.domain.shared.model.Money;

import java.util.List;
import java.util.Objects;

/**
 * O formulário de um grupo de opções: o nome, a pergunta, as regras e as opções, na ordem.
 *
 * @param name opcional: sem ele, a gestão mostra a própria pergunta
 */
public record OptionGroupForm(String name, String title, OptionGroupRules rules, List<Item> items) {

    /**
     * Uma opção do formulário.
     *
     * @param id null para uma opção nova; o id de uma opção que já existe para editá-la
     */
    public record Item(OptionItemId id, String name, Money price, boolean available) {
    }

    public OptionGroupForm {
        Objects.requireNonNull(rules);
        items = List.copyOf(Objects.requireNonNull(items));
    }
}
