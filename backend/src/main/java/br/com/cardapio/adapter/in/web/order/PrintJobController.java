package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.order.ClaimPrintJobUseCase;
import br.com.cardapio.application.port.in.order.ListPrintJobsUseCase;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.TicketKind;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A comanda impressa, para quem não tem tablet na cozinha: a estação de impressão (um computador
 * no balcão com a impressora) busca o que falta imprimir, pega cada comanda e imprime.
 */
@RestController
@Validated
@RequestMapping("/api/v1/print-jobs")
@Tag(name = "Impressão", description = "As comandas para a impressora da cozinha")
@SecurityRequirement(name = "bearer")
class PrintJobController {

    private final ListPrintJobsUseCase list;
    private final ClaimPrintJobUseCase claim;

    PrintJobController(ListPrintJobsUseCase list, ClaimPrintJobUseCase claim) {
        this.list = list;
        this.claim = claim;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF', 'KITCHEN')")
    @Operation(operationId = "listPendingPrintJobs",
            summary = "As comandas que ainda não saíram na impressora, da mais antiga à mais nova")
    @ApiResponse(responseCode = "200", description = "Página de comandas")
    PageResponse<PrintJobResponse> pending(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ORDER: os pedidos novos; CANCELLATION: os itens cancelados depois de impressos")
            @RequestParam TicketKind kind,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Comandas por página (1 a 100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(list.listPending(AuthenticatedUser.from(jwt).establishmentId(), kind,
                new PageQuery(page, size)), PrintJobResponse::from);
    }

    @GetMapping("/printed")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF', 'KITCHEN')")
    @Operation(operationId = "listPrintedJobs",
            summary = "As comandas impressas nas últimas 12 horas, da mais recente para a mais antiga (para reimprimir)")
    @ApiResponse(responseCode = "200", description = "Página de comandas")
    PageResponse<PrintJobResponse> printed(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Comandas por página (1 a 100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(list.listRecentlyPrinted(AuthenticatedUser.from(jwt).establishmentId(),
                new PageQuery(page, size)), PrintJobResponse::from);
    }

    @PostMapping("/claim")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF', 'KITCHEN')")
    @Operation(operationId = "claimPrintJob",
            summary = "Pega a comanda para imprimir: os itens saem marcados como impressos e só esta estação os recebe")
    @ApiResponse(responseCode = "200", description = "Os itens para imprimir (vazio: outra estação já imprimiu)")
    @ApiResponse(responseCode = "404", description = "Não é um pedido deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    PrintJobResponse claim(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ClaimPrintJobRequest request) {
        return PrintJobResponse.from(claim.claim(AuthenticatedUser.from(jwt).establishmentId(),
                new OrderId(request.orderId()), request.kind()));
    }
}
