package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.shared.model.Money;

import java.util.Objects;

/**
 * Uma escolha como fica copiada no pedido: o título do grupo, o nome da opção e os preços de agora.
 * Mudar o cardápio depois não muda o que foi pedido.
 *
 * @param splitsItem o grupo divide o item em partes (a cozinha mostra ½, ⅓, ¼)
 * @param price      o preço da opção neste tamanho, por vez
 * @param charged    quanto ela entrou de fato no preço do item, depois da regra do grupo (grátis,
 *                   maior preço, média)
 */
public record ChosenOption(OptionGroupId groupId, String groupTitle, boolean splitsItem, OptionItemId optionId,
                           String name, int quantity, Money price, Money charged) {

    public ChosenOption {
        Objects.requireNonNull(groupId);
        Objects.requireNonNull(groupTitle);
        Objects.requireNonNull(optionId);
        Objects.requireNonNull(name);
        Objects.requireNonNull(price);
        Objects.requireNonNull(charged);
        if (quantity < 1) {
            throw new IllegalArgumentException("quantidade da escolha deve ser positiva");
        }
    }
}
