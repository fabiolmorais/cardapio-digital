package br.com.cardapio.adapter.in.web.live;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.live.GetLiveFloorUseCase;
import br.com.cardapio.domain.account.model.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** O salão ao vivo (desenhos 7.1 e 7.2): o mapa das mesas e os números do dia. */
@RestController
@RequestMapping("/api/v1/live")
@Tag(name = "Ao vivo", description = "O movimento do restaurante agora")
@SecurityRequirement(name = "bearer")
class LiveFloorController {

    private final GetLiveFloorUseCase floor;

    LiveFloorController(GetLiveFloorUseCase floor) {
        this.floor = floor;
    }

    @GetMapping("/floor")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF', 'KITCHEN')")
    @Operation(operationId = "getLiveFloor",
            summary = "As mesas agora (livre, ocupada, chamando, conta pedida) e o resumo do dia")
    @ApiResponse(responseCode = "200", description = "O salão; o faturado só para o dono e o gerente")
    LiveFloorResponse get(@AuthenticationPrincipal Jwt jwt) {
        var user = AuthenticatedUser.from(jwt);
        boolean manager = user.role() == Role.OWNER || user.role() == Role.MANAGER;
        return LiveFloorResponse.from(floor.get(user.establishmentId(), manager));
    }
}
