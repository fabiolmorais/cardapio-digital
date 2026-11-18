package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.order.GetStaffOrderSheetUseCase;
import br.com.cardapio.application.port.in.order.PlaceStaffOrderCommand;
import br.com.cardapio.application.port.in.order.PlaceStaffOrderUseCase;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.order.exception.DuplicateOrderException;
import br.com.cardapio.domain.order.model.OrderLine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** O garçom lança pedidos na comanda da mesa, se o restaurante permitir (ADR 0011). */
@RestController
@RequestMapping("/api/v1/tables/{id}")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
@Tag(name = "Pedidos", description = "O pedido pelo celular")
@SecurityRequirement(name = "bearer")
class StaffOrderController {

    private final GetStaffOrderSheetUseCase getSheet;
    private final PlaceStaffOrderUseCase placeOrder;

    StaffOrderController(GetStaffOrderSheetUseCase getSheet, PlaceStaffOrderUseCase placeOrder) {
        this.getSheet = getSheet;
        this.placeOrder = placeOrder;
    }

    @GetMapping("/staff-order")
    @Operation(operationId = "getStaffOrderSheet",
            summary = "A situação da mesa para lançar um pedido: se está liberado, o que já foi pedido e o rodízio")
    @ApiResponse(responseCode = "200", description = "A situação da mesa")
    @ApiResponse(responseCode = "404", description = "Não é uma mesa deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    StaffOrderResponses.Sheet sheet(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return StaffOrderResponses.Sheet.from(
                getSheet.get(AuthenticatedUser.from(jwt).establishmentId(), new DiningTableId(id)));
    }

    @PostMapping("/orders")
    @Operation(operationId = "placeStaffOrder",
            summary = "Lança um pedido na comanda da mesa: vai direto para a cozinha")
    @ApiResponse(responseCode = "201", description = "Pedido lançado")
    @ApiResponse(responseCode = "404", description = "Não é uma mesa deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A mesa já pediu estes itens (a lista vem em duplicates), o "
            + "lançamento está desligado, ou um item esgotou",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = StaffOrderResponses.DuplicateProblem.class)))
    ResponseEntity<StaffOrderResponses.Placed> place(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                     @Valid @RequestBody PlaceStaffOrderRequest body) {
        var user = AuthenticatedUser.from(jwt);
        List<OrderLine> lines = body.items().stream().map(PlaceOrderRequest.Line::toLine).toList();
        Set<ProductId> confirmed = body.confirmedDuplicates() == null ? Set.of()
                : body.confirmedDuplicates().stream().map(ProductId::new).collect(Collectors.toSet());
        var order = placeOrder.place(new PlaceStaffOrderCommand(user.establishmentId(), user.userId(),
                new DiningTableId(id), body.clientRequestId(), lines, body.round(), confirmed));
        return ResponseEntity.status(HttpStatus.CREATED).body(StaffOrderResponses.Placed.from(order));
    }

    @ExceptionHandler(DuplicateOrderException.class)
    ProblemDetail duplicate(DuplicateOrderException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("duplicates", ex.duplicates().stream().map(StaffOrderResponses.Item::from).toList());
        return problem;
    }
}
