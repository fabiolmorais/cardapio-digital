package br.com.cardapio.adapter.in.web.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** Os pedidos das comandas do balcão (ADR 0015). */
final class CounterTabRequests {

    private CounterTabRequests() {
    }

    @Schema(name = "OpenCounterTabRequest")
    record Open(
            @Schema(requiredMode = REQUIRED, description = "O balcão") @NotNull UUID counterId,
            @Schema(requiredMode = REQUIRED, example = "João", description = "Como a equipe chama o cliente")
            @NotBlank @Size(max = 40) String name,
            @Schema(example = "12", description = "O número do cartão, se a casa usa") @Size(max = 10) String card
    ) {
    }

    @Schema(name = "RenameCounterTabRequest")
    record Rename(
            @Schema(requiredMode = REQUIRED, example = "João") @NotBlank @Size(max = 40) String name,
            @Schema(example = "12") @Size(max = 10) String card
    ) {
    }
}
