package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.order.GetStaffOrderSheetUseCase;
import br.com.cardapio.application.port.in.order.ManageCounterTabsUseCase;
import br.com.cardapio.application.port.in.order.OpenCounterTabCommand;
import br.com.cardapio.application.port.in.order.PlaceStaffOrderCommand;
import br.com.cardapio.application.port.in.order.PlaceStaffOrderUseCase;
import br.com.cardapio.application.port.in.order.RenameCounterTabCommand;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.order.exception.DuplicateOrderException;
import br.com.cardapio.domain.order.model.OrderLine;
import br.com.cardapio.domain.order.model.TabId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * As comandas do balcão (RF45, ADR 0015): dono, gerente e garçom abrem a comanda de cada cliente,
 * corrigem o nome e lançam os pedidos (no balcão, lançar é sempre permitido). A cozinha não.
 */
@RestController
@Validated
@RequestMapping("/api/v1/counter-tabs")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
@Tag(name = "Comandas", description = "Comandas abertas, fechamento e histórico")
@SecurityRequirement(name = "bearer")
class CounterTabController {

    private final ManageCounterTabsUseCase counterTabs;
    private final GetStaffOrderSheetUseCase getSheet;
    private final PlaceStaffOrderUseCase placeOrder;

    CounterTabController(ManageCounterTabsUseCase counterTabs, GetStaffOrderSheetUseCase getSheet,
                         PlaceStaffOrderUseCase placeOrder) {
        this.counterTabs = counterTabs;
        this.getSheet = getSheet;
        this.placeOrder = placeOrder;
    }

    @PostMapping
    @Operation(operationId = "openCounterTab", summary = "Abre a comanda de um cliente do balcão, com o nome")
    @ApiResponse(responseCode = "201", description = "Comanda aberta")
    @ApiResponse(responseCode = "404", description = "Não é um balcão deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "O cartão já está em outra comanda aberta",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<OpenTabResponse> open(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody CounterTabRequests.Open body) {
        var tab = counterTabs.open(new OpenCounterTabCommand(AuthenticatedUser.from(jwt).establishmentId(),
                new DiningTableId(body.counterId()), body.name(), body.card()));
        return ResponseEntity.status(HttpStatus.CREATED).body(OpenTabResponse.from(tab));
    }

    @GetMapping
    @Operation(operationId = "listCounterTabs",
            summary = "As comandas abertas de um balcão, da mais antiga para a mais nova")
    @ApiResponse(responseCode = "200", description = "Página das comandas do balcão")
    @ApiResponse(responseCode = "404", description = "Não é um balcão deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    PageResponse<OpenTabResponse> list(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID counterId,
                                       @RequestParam(defaultValue = "0") @Min(0) int page,
                                       @RequestParam(defaultValue = "50") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(counterTabs.list(AuthenticatedUser.from(jwt).establishmentId(),
                new DiningTableId(counterId), new PageQuery(page, size)), OpenTabResponse::from);
    }

    @PatchMapping("/{id}")
    @Operation(operationId = "renameCounterTab", summary = "Corrige o nome ou o cartão da comanda aberta do balcão")
    @ApiResponse(responseCode = "200", description = "Comanda corrigida")
    @ApiResponse(responseCode = "404", description = "Comanda não encontrada",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "O cartão já está em outra comanda aberta",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    OpenTabResponse rename(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                           @Valid @RequestBody CounterTabRequests.Rename body) {
        return OpenTabResponse.from(counterTabs.rename(new RenameCounterTabCommand(
                AuthenticatedUser.from(jwt).establishmentId(), new TabId(id), body.name(), body.card())));
    }

    @GetMapping("/{id}/staff-order")
    @Operation(operationId = "getCounterOrderSheet",
            summary = "A situação da comanda do balcão para lançar um pedido: o que já foi pedido")
    @ApiResponse(responseCode = "200", description = "A situação da comanda")
    @ApiResponse(responseCode = "404", description = "Não é uma comanda do balcão deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    StaffOrderResponses.Sheet sheet(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return StaffOrderResponses.Sheet.from(
                getSheet.getForCounterTab(AuthenticatedUser.from(jwt).establishmentId(), new TabId(id)));
    }

    @PostMapping("/{id}/orders")
    @Operation(operationId = "placeCounterOrder",
            summary = "Lança um pedido na comanda do balcão: vai direto para a cozinha")
    @ApiResponse(responseCode = "201", description = "Pedido lançado")
    @ApiResponse(responseCode = "404", description = "Não é uma comanda do balcão deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A comanda já pediu estes itens (a lista vem em duplicates), "
            + "ou um item esgotou",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = StaffOrderResponses.DuplicateProblem.class)))
    ResponseEntity<StaffOrderResponses.Placed> place(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                     @Valid @RequestBody PlaceStaffOrderRequest body) {
        var user = AuthenticatedUser.from(jwt);
        List<OrderLine> lines = body.items().stream().map(PlaceOrderRequest.Line::toLine).toList();
        Set<ProductId> confirmed = body.confirmedDuplicates() == null ? Set.of()
                : body.confirmedDuplicates().stream().map(ProductId::new).collect(Collectors.toSet());
        var order = placeOrder.place(PlaceStaffOrderCommand.atCounter(user.establishmentId(), user.userId(),
                new TabId(id), body.clientRequestId(), lines, confirmed));
        return ResponseEntity.status(HttpStatus.CREATED).body(StaffOrderResponses.Placed.from(order));
    }

    @ExceptionHandler(DuplicateOrderException.class)
    ProblemDetail duplicate(DuplicateOrderException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("duplicates", ex.duplicates().stream().map(StaffOrderResponses.Item::from).toList());
        return problem;
    }
}
