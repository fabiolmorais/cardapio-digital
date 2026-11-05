package br.com.cardapio.adapter.in.web.push;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.push.ManagePushUseCase;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * As notificações no celular da equipe (o app do garçom). Dono, gerente e garçom se inscrevem;
 * a cozinha acompanha pela tela dela.
 */
@RestController
@Validated
@RequestMapping("/api/v1/push")
@Tag(name = "Notificações", description = "As notificações no celular da equipe (web push)")
@SecurityRequirement(name = "bearer")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
class PushController {

    private final ManagePushUseCase push;

    PushController(ManagePushUseCase push) {
        this.push = push;
    }

    @GetMapping("/settings")
    @Operation(operationId = "pushSettings", summary = "Se o push está ligado, e a chave pública VAPID")
    @ApiResponse(responseCode = "200", description = "As configurações")
    PushSettingsResponse settings() {
        return PushSettingsResponse.from(push.settings());
    }

    @PostMapping("/subscriptions")
    @Operation(operationId = "subscribePush", summary = "Inscreve este navegador nas notificações")
    @ApiResponse(responseCode = "201", description = "Inscrito")
    @ApiResponse(responseCode = "400", description = "Inscrição inválida, ou serviço de push desconhecido",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "O push está desligado neste servidor",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> subscribe(@AuthenticationPrincipal Jwt jwt,
                                   @Valid @RequestBody PushSubscriptionRequest request) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        push.subscribe(user.establishmentId(), user.userId(), endpoint(request.endpoint()), request.keys().p256dh(),
                request.keys().auth());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/subscriptions")
    @Operation(operationId = "unsubscribePush", summary = "Tira este navegador das notificações (ao sair, por exemplo)")
    @ApiResponse(responseCode = "204", description = "Removido")
    @ApiResponse(responseCode = "404", description = "Este navegador não está inscrito por esta pessoa",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> unsubscribe(@AuthenticationPrincipal Jwt jwt,
                                     @Parameter(description = "O endpoint da inscrição")
                                     @RequestParam @NotBlank @Size(max = 1000) String endpoint) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        push.unsubscribe(user.establishmentId(), user.userId(), endpoint(endpoint));
        return ResponseEntity.noContent().build();
    }

    /** Um endpoint que nem é URL vira 400 com a mensagem de sempre (o domínio confere o resto). */
    private static URI endpoint(String value) {
        try {
            return new URI(value);
        } catch (URISyntaxException e) {
            throw new InvalidValueException("O endereço de notificação do navegador não é válido.");
        }
    }
}
