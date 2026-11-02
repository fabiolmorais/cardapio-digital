package br.com.cardapio.adapter.in.web.presence;

import br.com.cardapio.adapter.in.web.shared.ClientIp;
import br.com.cardapio.adapter.in.web.tablesession.TableSessionCookie;
import br.com.cardapio.application.port.in.presence.CheckPresenceCommand;
import br.com.cardapio.application.port.in.presence.CheckPresenceUseCase;
import br.com.cardapio.domain.presence.model.GeoPosition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** "Estou no salão?": a página da mesa pergunta ao abrir e quando o cliente manda a localização. */
@RestController
@Tag(name = "Mesa", description = "O que o cliente vê ao escanear o QR Code")
class PresenceController {

    private final CheckPresenceUseCase checkPresence;
    private final ClientIp clientIp;

    PresenceController(CheckPresenceUseCase checkPresence, ClientIp clientIp) {
        this.checkPresence = checkPresence;
        this.clientIp = clientIp;
    }

    /** POST, e não GET: confirmar a presença muda a sessão (e a localização não vai na URL). */
    @PostMapping("/api/v1/public/tables/{token}/presence")
    @Operation(operationId = "checkPresence",
            summary = "Confere se o celular está no salão: pela rede e, se vier, pela localização")
    @ApiResponse(responseCode = "200", description = "Como está a presença desta sessão")
    @ApiResponse(responseCode = "401", description = "Sem sessão válida nesta mesa",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "QR inválido, trocado ou mesa desativada",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    PresenceResponse check(@PathVariable String token,
                           @CookieValue(name = TableSessionCookie.NAME, required = false) String session,
                           @RequestBody(required = false) PresenceCheckRequest body,
                           HttpServletRequest request) {
        GeoPosition position = body != null && body.hasPosition()
                ? new GeoPosition(body.latitude(), body.longitude(), body.accuracyMeters())
                : null;
        return PresenceResponse.from(checkPresence.check(
                new CheckPresenceCommand(token, session, clientIp.of(request), position)));
    }
}
