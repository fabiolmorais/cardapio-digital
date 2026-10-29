package br.com.cardapio.adapter.in.web.floor;

import br.com.cardapio.application.port.in.floor.PublicTable;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "PublicTable", description = "A mesa do QR Code, para a página pública")
record PublicTableResponse(
        @Schema(requiredMode = REQUIRED, example = "Sakura Sushi") String establishmentName,
        @Schema(requiredMode = REQUIRED, example = "04") String tableLabel
) {
    static PublicTableResponse from(PublicTable table) {
        return new PublicTableResponse(table.establishmentName(), table.tableLabel());
    }
}
