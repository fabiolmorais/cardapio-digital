package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.order.GetOrderSettingsUseCase;
import br.com.cardapio.application.port.in.order.UpdateOrderSettingsCommand;
import br.com.cardapio.application.port.in.order.UpdateOrderSettingsUseCase;
import br.com.cardapio.domain.shared.model.Money;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Como o restaurante recebe pedidos (a tela "Presença e pedidos" da gestão). */
@RestController
@RequestMapping("/api/v1/order-settings")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
@Tag(name = "Pedidos", description = "O pedido pelo celular")
@SecurityRequirement(name = "bearer")
class OrderSettingsController {

    private final GetOrderSettingsUseCase getSettings;
    private final UpdateOrderSettingsUseCase updateSettings;

    OrderSettingsController(GetOrderSettingsUseCase getSettings, UpdateOrderSettingsUseCase updateSettings) {
        this.getSettings = getSettings;
        this.updateSettings = updateSettings;
    }

    @GetMapping
    @Operation(operationId = "getOrderSettings",
            summary = "As configurações de pedido (os padrões, se nunca mudadas)")
    @ApiResponse(responseCode = "200", description = "As configurações")
    OrderSettingsResponse get(@AuthenticationPrincipal Jwt jwt) {
        return OrderSettingsResponse.from(getSettings.get(AuthenticatedUser.from(jwt).establishmentId()));
    }

    @PutMapping
    @Operation(operationId = "updateOrderSettings", summary = "Troca as configurações de pedido")
    @ApiResponse(responseCode = "200", description = "Salvas")
    OrderSettingsResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OrderSettingsRequest body) {
        return OrderSettingsResponse.from(updateSettings.update(new UpdateOrderSettingsCommand(
                AuthenticatedUser.from(jwt).establishmentId(), body.orderingEnabled(), body.approvalRequired(),
                body.maxAmountCents() == null ? null : Money.ofCents(body.maxAmountCents()), body.maxItems())));
    }
}
