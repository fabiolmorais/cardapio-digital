package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Uma mudança no estado do produto que os relatórios acompanham (REL-22, 24, 25): a promoção, o
 * destaque e o "disponível", com o estado novo.
 *
 * @param promoPrice o preço da promoção que entrou; null nas outras mudanças e quando a promoção sai
 */
public record ProductStatusChange(EstablishmentId establishmentId, ProductId productId, Kind kind, boolean active,
                                  Money promoPrice, Instant at) {

    public enum Kind { PROMOTION, FEATURED, AVAILABLE }

    public ProductStatusChange {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(productId);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(at);
    }

    /**
     * O que mudou de {@code before} para {@code after}. Produto novo ({@code before} null): o estado
     * inicial dos três. A promoção conta como mudança também quando só o preço dela muda.
     */
    public static List<ProductStatusChange> between(Product before, Product after, Instant at) {
        Objects.requireNonNull(after);
        EstablishmentId establishment = after.establishmentId();
        ProductId product = after.id();
        Money promo = after.details().promoPrice();
        List<ProductStatusChange> changes = new ArrayList<>();
        if (before == null || !Objects.equals(before.details().promoPrice(), promo)) {
            changes.add(new ProductStatusChange(establishment, product, Kind.PROMOTION, promo != null, promo, at));
        }
        if (before == null || before.details().featured() != after.details().featured()) {
            changes.add(new ProductStatusChange(establishment, product, Kind.FEATURED, after.details().featured(),
                    null, at));
        }
        if (before == null || before.available() != after.available()) {
            changes.add(new ProductStatusChange(establishment, product, Kind.AVAILABLE, after.available(), null, at));
        }
        return changes;
    }
}
