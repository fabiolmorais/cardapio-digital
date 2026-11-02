package br.com.cardapio.adapter.in.web.tablesession;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** O token NÃO vem aqui: ele só existe no cookie HttpOnly, longe do JavaScript da página. */
@Schema(name = "TableSession")
record TableSessionResponse(
        @Schema(requiredMode = REQUIRED, description = "Vence se ficar este tempo sem uso") Instant expiresAt
) {
}
