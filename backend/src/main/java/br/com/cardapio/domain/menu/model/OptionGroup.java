package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Objects;

/**
 * Um grupo de opções da biblioteca do restaurante (ADR 0010): uma pergunta ("Escolha os sabores",
 * "Ponto da carne", "Adicionais") ligada a quantos produtos o restaurante quiser. Mudar o grupo
 * muda em todos. Nunca é apagado (os pedidos apontam para ele): sai desativado.
 *
 * @param name  o nome na lista da gestão ("Sabores da pizza grande")
 * @param title o que o cliente vê ("Escolha os sabores")
 */
public record OptionGroup(OptionGroupId id, EstablishmentId establishmentId, String name, String title,
                          OptionGroupRules rules, boolean active) {

    public static final int TEXT_MAX = 60;
    /** Opções ativas num grupo. */
    public static final int MAX_ITEMS = 50;
    /** Grupos ativos num restaurante. */
    public static final int MAX_PER_ESTABLISHMENT = 200;
    /** Grupos ligados a um produto. */
    public static final int MAX_PER_PRODUCT = 10;

    public OptionGroup {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(rules);
        title = text(title, "Escreva a pergunta que o cliente vê (ex.: Escolha os sabores)");
        // Sem nome interno, a gestão mostra a própria pergunta.
        name = name == null || name.isBlank() ? title : text(name, "Nome do grupo longo demais");
    }

    public static OptionGroup create(EstablishmentId establishmentId, String name, String title,
                                     OptionGroupRules rules) {
        return new OptionGroup(OptionGroupId.newId(), establishmentId, name, title, rules, true);
    }

    public OptionGroup edit(String newName, String newTitle, OptionGroupRules newRules) {
        return new OptionGroup(id, establishmentId, newName, newTitle, newRules, active);
    }

    public OptionGroup deactivate() {
        return new OptionGroup(id, establishmentId, name, title, rules, false);
    }

    static String text(String value, String message) {
        String trimmed = value == null ? "" : value.strip();
        if (trimmed.isEmpty() || trimmed.length() > TEXT_MAX) {
            throw new InvalidValueException(message + " (até " + TEXT_MAX + " letras)");
        }
        return trimmed;
    }
}
