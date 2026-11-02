package br.com.cardapio.adapter.in.web.presence;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.ClientIp;
import br.com.cardapio.application.port.in.presence.GetPresenceRuleUseCase;
import br.com.cardapio.application.port.in.presence.UpdatePresenceRuleCommand;
import br.com.cardapio.application.port.in.presence.UpdatePresenceRuleUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A tela "Presença" da gestão: as redes do salão, o nome do Wi-Fi e o local com o raio. */
@RestController
@RequestMapping("/api/v1/presence-rule")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
@Tag(name = "Presença", description = "A trava de presença: só quem está no salão chama e pede")
@SecurityRequirement(name = "bearer")
class PresenceRuleController {

    private final GetPresenceRuleUseCase getRule;
    private final UpdatePresenceRuleUseCase updateRule;
    private final ClientIp clientIp;

    PresenceRuleController(GetPresenceRuleUseCase getRule, UpdatePresenceRuleUseCase updateRule, ClientIp clientIp) {
        this.getRule = getRule;
        this.updateRule = updateRule;
        this.clientIp = clientIp;
    }

    @GetMapping
    @Operation(operationId = "getPresenceRule", summary = "A configuração de presença (vazia se nunca configurada)")
    @ApiResponse(responseCode = "200", description = "A configuração e a rede de onde você está")
    PresenceRuleResponse get(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        return PresenceRuleResponse.from(getRule.get(AuthenticatedUser.from(jwt).establishmentId(),
                clientIp.of(request)));
    }

    @PutMapping
    @Operation(operationId = "updatePresenceRule", summary = "Troca a configuração de presença inteira")
    @ApiResponse(responseCode = "200", description = "Salva")
    @ApiResponse(responseCode = "400", description = "IP, local ou raio inválido",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    PresenceRuleResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PresenceRuleRequest body,
                                HttpServletRequest request) {
        var command = new UpdatePresenceRuleCommand(AuthenticatedUser.from(jwt).establishmentId(), body.networks(),
                body.wifiName(), body.location() == null ? null : body.location().toGeoFence(), clientIp.of(request),
                body.sessionIdleMinutes());
        return PresenceRuleResponse.from(updateRule.update(command));
    }
}
