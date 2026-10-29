package br.com.cardapio.adapter.in.web.floor;

import br.com.cardapio.application.port.in.floor.ViewPublicTableUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A página da mesa, aberta pelo QR Code. Pública: o cliente não tem login. Quem identifica a
 * mesa (e o restaurante) é o token aleatório da URL.
 */
@RestController
@RequestMapping("/api/v1/public/tables")
@Tag(name = "Mesa", description = "O que o cliente vê ao escanear o QR Code")
class PublicTableController {

    private final ViewPublicTableUseCase viewTable;

    PublicTableController(ViewPublicTableUseCase viewTable) {
        this.viewTable = viewTable;
    }

    @GetMapping("/{token}")
    @Operation(operationId = "getPublicTable", summary = "A mesa do QR Code: restaurante e número")
    @ApiResponse(responseCode = "200", description = "Mesa encontrada")
    @ApiResponse(responseCode = "404", description = "QR inválido, trocado ou mesa desativada",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    PublicTableResponse view(@PathVariable String token) {
        return PublicTableResponse.from(viewTable.view(token));
    }
}
