package br.com.cardapio.domain.menu.model;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Os "Mais pedidos" do início da mesa: os pratos mais pedidos nos últimos 30 dias que o cliente
 * pode pedir agora (disponíveis e no cardápio à la carte). Com menos de 3, a lista não diz nada
 * ainda (restaurante novo): fica vazia, e a mesa mostra os destaques do dono no lugar.
 */
public final class MostOrdered {

    public static final Duration WINDOW = Duration.ofDays(30);
    public static final int MAX = 8;
    public static final int MIN = 3;

    private MostOrdered() {
    }

    /** `ranked` vem do mais pedido ao menos; `orderable` são os que dá para pedir agora. */
    public static List<ProductId> pick(List<ProductId> ranked, Set<ProductId> orderable) {
        List<ProductId> picked = ranked.stream().filter(orderable::contains).limit(MAX).toList();
        return picked.size() < MIN ? List.of() : picked;
    }
}
