package br.com.cardapio.adapter.in.web.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** Criar e renomear categoria pedem a mesma coisa: o nome. */
@Schema(name = "MenuCategoryRequest")
record MenuCategoryRequest(
        @Schema(requiredMode = REQUIRED, example = "Entradas") @NotBlank @Size(max = 40) String name
) {
}
