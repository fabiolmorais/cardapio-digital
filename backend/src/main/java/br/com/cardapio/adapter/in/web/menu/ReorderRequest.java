package br.com.cardapio.adapter.in.web.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "ReorderRequest", description = "Todos os itens da lista, na ordem nova")
record ReorderRequest(
        @Schema(requiredMode = REQUIRED) @NotNull @Size(max = 500) List<@NotNull UUID> ids
) {
    <T> List<T> as(Function<UUID, T> idType) {
        return ids.stream().map(idType).toList();
    }
}
