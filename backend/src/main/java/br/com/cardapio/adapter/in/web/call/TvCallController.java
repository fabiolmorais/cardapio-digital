package br.com.cardapio.adapter.in.web.call;

import br.com.cardapio.adapter.in.web.device.DeviceTokenCookie;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.call.ListTvCallQueueUseCase;
import br.com.cardapio.application.port.shared.PageQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A fila no Modo TV, sem login. Fica em /api/v1/auth/device porque o cookie device_token só vai
 * para /api/v1/auth (Guia 5): quem identifica a TV é o cookie.
 */
@RestController
@Validated
@Tag(name = "Chamados", description = "A fila de chamados das mesas")
class TvCallController {

    private final ListTvCallQueueUseCase listQueue;

    TvCallController(ListTvCallQueueUseCase listQueue) {
        this.listQueue = listQueue;
    }

    @GetMapping("/api/v1/auth/device/tv/calls")
    @Operation(operationId = "listTvCallQueue", summary = "A fila de chamados na TV vinculada (sem login, pelo cookie)")
    @ApiResponse(responseCode = "200", description = "Página da fila")
    @ApiResponse(responseCode = "401", description = "Aparelho não vinculado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "O aparelho não é uma TV",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    PageResponse<StaffCallResponse> list(
            @CookieValue(name = DeviceTokenCookie.NAME, required = false) String deviceToken,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Itens por página (1 a 100)")
            @RequestParam(defaultValue = "50") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(listQueue.list(deviceToken, new PageQuery(page, size)), StaffCallResponse::from);
    }
}
