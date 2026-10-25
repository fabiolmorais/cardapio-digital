package br.com.cardapio.domain.shared.model;

import java.util.List;
import java.util.function.Function;

/**
 * Uma página de resultados com o total geral.
 */
public record ResultPage<T>(List<T> content, int page, int size, long totalElements) {

    public ResultPage {
        content = List.copyOf(content);
    }

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public <R> ResultPage<R> map(Function<T, R> mapper) {
        return new ResultPage<>(content.stream().map(mapper).toList(), page, size, totalElements);
    }
}
