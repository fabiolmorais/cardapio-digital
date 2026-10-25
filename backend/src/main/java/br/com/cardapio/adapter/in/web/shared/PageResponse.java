package br.com.cardapio.adapter.in.web.shared;

import br.com.cardapio.application.port.shared.ResultPage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.function.Function;

@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
public record PageResponse<T>(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<T> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalElements,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalPages) {

    public static <D, T> PageResponse<T> from(ResultPage<D> page, Function<D, T> mapper) {
        return new PageResponse<>(
                page.content().stream().map(mapper).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
