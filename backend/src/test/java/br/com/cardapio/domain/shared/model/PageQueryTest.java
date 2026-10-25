package br.com.cardapio.domain.shared.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageQueryTest {

    @Test
    void rejectsInvalidValues() {
        assertThatThrownBy(() -> new PageQuery(-1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageQuery(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageQuery(0, PageQuery.MAX_SIZE + 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resultPageCalculatesTotalPages() {
        assertThat(new ResultPage<>(List.of(1, 2), 0, 2, 5).totalPages()).isEqualTo(3);
        assertThat(new ResultPage<>(List.of(), 0, 10, 0).totalPages()).isZero();
    }

    @Test
    void resultPageMapsContent() {
        ResultPage<String> mapped = new ResultPage<>(List.of(1, 2), 1, 2, 4).map(String::valueOf);
        assertThat(mapped.content()).containsExactly("1", "2");
        assertThat(mapped.page()).isEqualTo(1);
        assertThat(mapped.totalElements()).isEqualTo(4);
    }
}
