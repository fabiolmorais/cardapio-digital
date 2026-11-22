package br.com.cardapio.adapter.in.web.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(name = "TabPeopleRequest")
record TabPeopleRequest(
        @Schema(description = "Pessoas na mesa (1 a 50); vazio apaga", example = "4")
        @Min(1) @Max(50) Integer people) {
}
