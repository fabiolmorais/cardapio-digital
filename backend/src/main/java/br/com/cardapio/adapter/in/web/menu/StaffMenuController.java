package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.menu.GetStaffMenuUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** O cardápio para a equipe lançar pedidos: como documento único, igual ao da mesa (ADR 0006). */
@RestController
@Tag(name = "Pedidos", description = "O pedido pelo celular")
@SecurityRequirement(name = "bearer")
class StaffMenuController {

    private final GetStaffMenuUseCase getMenu;

    StaffMenuController(GetStaffMenuUseCase getMenu) {
        this.getMenu = getMenu;
    }

    @GetMapping("/api/v1/staff-menu")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    @Operation(operationId = "getStaffMenu",
            summary = "O cardápio do restaurante para lançar pedidos: preços e opções, sem fotos e sem vídeos")
    @ApiResponse(responseCode = "200", description = "O cardápio")
    PublicMenuResponse menu(@AuthenticationPrincipal Jwt jwt) {
        return PublicMenuResponse.from(getMenu.get(AuthenticatedUser.from(jwt).establishmentId()));
    }
}
