package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.order.CloseTabUseCase;
import br.com.cardapio.application.port.in.order.GetTabUseCase;
import br.com.cardapio.application.port.in.order.ListOpenTabsUseCase;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.order.model.TabId;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** As comandas para quem atende o salão: as abertas, o detalhe de uma e o fechamento. */
@RestController
@Validated
@RequestMapping("/api/v1/tabs")
@Tag(name = "Comandas", description = "As comandas das mesas e o fechamento da conta")
@SecurityRequirement(name = "bearer")
class TabController {

    private final ListOpenTabsUseCase listOpen;
    private final GetTabUseCase getTab;
    private final CloseTabUseCase closeTab;

    TabController(ListOpenTabsUseCase listOpen, GetTabUseCase getTab, CloseTabUseCase closeTab) {
        this.listOpen = listOpen;
        this.getTab = getTab;
        this.closeTab = closeTab;
    }

    @GetMapping("/open")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    @Operation(operationId = "listOpenTabs",
            summary = "As comandas abertas, com o total e os itens a caminho, da mais antiga à mais nova")
    @ApiResponse(responseCode = "200", description = "Página das comandas")
    PageResponse<OpenTabResponse> listOpen(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Comandas por página (1 a 100)")
            @RequestParam(defaultValue = "50") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(listOpen.list(AuthenticatedUser.from(jwt).establishmentId(),
                new PageQuery(page, size)), OpenTabResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    @Operation(operationId = "getTab", summary = "Uma comanda com todos os pedidos (aberta ou fechada)")
    @ApiResponse(responseCode = "200", description = "A comanda")
    @ApiResponse(responseCode = "404", description = "Não é uma comanda deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    StaffTabResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return StaffTabResponse.from(getTab.get(AuthenticatedUser.from(jwt).establishmentId(), new TabId(id)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    @Operation(operationId = "closeTab",
            summary = "Fecha a comanda depois do pagamento: a mesa fica livre e os celulares saem")
    @ApiResponse(responseCode = "200", description = "Fechada, com o total cobrado")
    @ApiResponse(responseCode = "404", description = "Não é uma comanda deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Já fechada, ou com itens a caminho (a mensagem diz quais)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    StaffTabResponse close(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                           @Valid @RequestBody CloseTabRequest request) {
        var user = AuthenticatedUser.from(jwt);
        return StaffTabResponse.from(closeTab.close(user.establishmentId(), new TabId(id), user.userId(),
                request.paymentMethod()));
    }
}
