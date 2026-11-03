package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.order.ListWaitingOrdersUseCase;
import br.com.cardapio.application.port.in.order.ReviewOrderUseCase;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.order.model.OrderId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** A confirmação do garçom: os pedidos parados nela, e confirmar ou recusar. A cozinha não decide. */
@RestController
@Validated
@RequestMapping("/api/v1/orders")
@Tag(name = "Pedidos", description = "A confirmação dos pedidos pelo garçom")
@SecurityRequirement(name = "bearer")
class OrderReviewController {

    private final ListWaitingOrdersUseCase listWaiting;
    private final ReviewOrderUseCase review;

    OrderReviewController(ListWaitingOrdersUseCase listWaiting, ReviewOrderUseCase review) {
        this.listWaiting = listWaiting;
        this.review = review;
    }

    @GetMapping("/awaiting-approval")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    @Operation(operationId = "listWaitingOrders",
            summary = "Os pedidos esperando a confirmação do garçom, do mais antigo ao mais novo")
    @ApiResponse(responseCode = "200", description = "Página dos pedidos")
    PageResponse<WaitingOrderResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Pedidos por página (1 a 100)")
            @RequestParam(defaultValue = "50") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(listWaiting.list(AuthenticatedUser.from(jwt).establishmentId(),
                new PageQuery(page, size)), WaitingOrderResponse::from);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    @Operation(operationId = "approveOrder", summary = "Confirma: o pedido vai para a cozinha")
    @ApiResponse(responseCode = "204", description = "Confirmado")
    @ApiResponse(responseCode = "404", description = "Não é um pedido deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Não está esperando a confirmação",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var user = AuthenticatedUser.from(jwt);
        review.approve(user.establishmentId(), new OrderId(id), user.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    @Operation(operationId = "rejectOrder", summary = "Recusa: o pedido não vai para a cozinha nem para a conta")
    @ApiResponse(responseCode = "204", description = "Recusado (os itens são cancelados)")
    @ApiResponse(responseCode = "404", description = "Não é um pedido deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Não está esperando a confirmação",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var user = AuthenticatedUser.from(jwt);
        review.reject(user.establishmentId(), new OrderId(id), user.userId());
        return ResponseEntity.noContent().build();
    }
}
