package br.com.cardapio.domain.menu.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class MostOrderedTest {

    private final List<ProductId> ranked = IntStream.range(0, 10).mapToObj(i -> ProductId.newId()).toList();

    @Test
    void keepsTheOrderAndOnlyWhatCanBeOrderedUpToEight() {
        Set<ProductId> orderable = Set.copyOf(ranked.subList(1, 10)); // o primeiro esgotou
        List<ProductId> picked = MostOrdered.pick(ranked, orderable);

        assertThat(picked).hasSize(MostOrdered.MAX).doesNotContain(ranked.get(0));
        assertThat(picked.get(0)).isEqualTo(ranked.get(1));
    }

    @Test
    void withLessThanThreeTheListSaysNothingYet() {
        assertThat(MostOrdered.pick(ranked.subList(0, 2), Set.copyOf(ranked))).isEmpty();
        assertThat(MostOrdered.pick(ranked.subList(0, 3), Set.copyOf(ranked))).hasSize(3);
    }
}
