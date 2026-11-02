package br.com.cardapio.adapter.in.web.tablesession;

import br.com.cardapio.application.port.in.tablesession.StartTableSessionUseCase;
import br.com.cardapio.application.port.in.tablesession.TableSessionStarted;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A sessão do celular na mesa. Pública: o cliente não tem login; o cookie é a "identidade" da visita. */
@RestController
@RequestMapping("/api/v1/public/tables/{token}")
@Tag(name = "Mesa", description = "O que o cliente vê ao escanear o QR Code")
class TableSessionController {

    private final StartTableSessionUseCase startSession;
    private final TableSessionCookie cookie;

    TableSessionController(StartTableSessionUseCase startSession, TableSessionCookie cookie) {
        this.startSession = startSession;
        this.cookie = cookie;
    }

    /**
     * POST, e não GET: abre ou renova uma sessão (muda o estado). A página chama ao abrir;
     * chamar de novo é seguro (com o cookie válido, só renova o prazo).
     */
    @PostMapping("/session")
    @Operation(operationId = "startTableSession",
            summary = "Abre a sessão deste celular na mesa (ou renova a que ele já tem), no cookie table_session")
    @ApiResponse(responseCode = "200", description = "Sessão em uso; o cookie vai no Set-Cookie")
    @ApiResponse(responseCode = "404", description = "QR inválido, trocado ou mesa desativada",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<TableSessionResponse> start(
            @PathVariable String token,
            @CookieValue(name = TableSessionCookie.NAME, required = false) String sessionToken) {
        TableSessionStarted session = startSession.start(token, sessionToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.create(session.token(), session.expiresAt()).toString())
                .body(new TableSessionResponse(session.expiresAt()));
    }
}
