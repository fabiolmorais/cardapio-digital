package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.order.AdvanceOrderItemUseCase;
import br.com.cardapio.application.port.in.order.CancelOrderItemByStaffUseCase;
import br.com.cardapio.application.port.in.order.ListKitchenQueueUseCase;
import br.com.cardapio.application.port.in.order.OrderItemStep;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.order.model.OrderItemId;
import br.com.cardapio.domain.order.model.OrderItemStatus;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * A fila da cozinha e os passos de cada item. A cozinha marca em preparo e pronto; quem atende o
 * salão marca entregue; todos cancelam com motivo (acabou o ingrediente).
 */
@RestController
@Validated
@RequestMapping("/api/v1/order-items")
@Tag(name = "Cozinha", description = "A fila de itens e os passos de cada um")
@SecurityRequirement(name = "bearer")
class KitchenController {

    private final ListKitchenQueueUseCase listQueue;
    private final AdvanceOrderItemUseCase advance;
    private final CancelOrderItemByStaffUseCase cancel;

    KitchenController(ListKitchenQueueUseCase listQueue, AdvanceOrderItemUseCase advance,
                      CancelOrderItemByStaffUseCase cancel) {
        this.listQueue = listQueue;
        this.advance = advance;
        this.cancel = cancel;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF', 'KITCHEN')")
    @Operation(operationId = "listKitchenQueue",
            summary = "Os itens a caminho dos pedidos que foram para a cozinha, do mais antigo ao mais novo")
    @ApiResponse(responseCode = "200", description = "Página da fila")
    @ApiResponse(responseCode = "400", description = "Status que não está na fila (entregue, cancelado)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    PageResponse<KitchenItemResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Só estes status (PENDING, PREPARING, READY); sem nada, os três")
            @RequestParam(name = "status", required = false) List<OrderItemStatus> status,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Itens por página (1 a 100)")
            @RequestParam(defaultValue = "50") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        var statuses = status == null || status.isEmpty()
                ? EnumSet.noneOf(OrderItemStatus.class) : EnumSet.copyOf(status);
        return PageResponse.from(listQueue.list(AuthenticatedUser.from(jwt).establishmentId(), statuses,
                new PageQuery(page, size)), KitchenItemResponse::from);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF', 'KITCHEN')")
    @Operation(operationId = "startOrderItem", summary = "Em preparo: a cozinha começou")
    @ApiResponse(responseCode = "204", description = "Em preparo")
    @ApiResponse(responseCode = "404", description = "Não é um item deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Não está na fila, ou o pedido espera o garçom",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> start(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return step(jwt, id, OrderItemStep.START);
    }

    @PostMapping("/{id}/ready")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF', 'KITCHEN')")
    @Operation(operationId = "readyOrderItem", summary = "Pronto: saindo da cozinha (da fila ou do preparo)")
    @ApiResponse(responseCode = "204", description = "Pronto")
    @ApiResponse(responseCode = "404", description = "Não é um item deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Já está pronto, entregue ou cancelado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> ready(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return step(jwt, id, OrderItemStep.READY);
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    @Operation(operationId = "deliverOrderItem", summary = "Entregue: chegou à mesa")
    @ApiResponse(responseCode = "204", description = "Entregue")
    @ApiResponse(responseCode = "404", description = "Não é um item deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Já foi entregue ou cancelado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> deliver(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return step(jwt, id, OrderItemStep.DELIVER);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF', 'KITCHEN')")
    @Operation(operationId = "cancelOrderItemByStaff", summary = "Cancela o item, com o motivo que o cliente vê")
    @ApiResponse(responseCode = "204", description = "Cancelado")
    @ApiResponse(responseCode = "400", description = "Sem motivo, ou motivo longo demais",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Não é um item deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Já foi entregue ou cancelado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @Valid @RequestBody CancelItemRequest request) {
        var user = AuthenticatedUser.from(jwt);
        cancel.cancel(user.establishmentId(), new OrderItemId(id), user.userId(), request.reason());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Void> step(Jwt jwt, UUID id, OrderItemStep step) {
        var user = AuthenticatedUser.from(jwt);
        advance.advance(user.establishmentId(), new OrderItemId(id), step, user.userId());
        return ResponseEntity.noContent().build();
    }
}
