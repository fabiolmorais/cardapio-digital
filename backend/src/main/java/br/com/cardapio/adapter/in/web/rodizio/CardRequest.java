package br.com.cardapio.adapter.in.web.rodizio;

import br.com.cardapio.domain.rodizio.model.TabRodizioStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "RodizioCardRequest")
record CardRequest(
        @Schema(requiredMode = REQUIRED, description = "SERVING: cartão verde (pode servir). PAUSED: vermelho (pausa)")
        @NotNull TabRodizioStatus status
) {
}
