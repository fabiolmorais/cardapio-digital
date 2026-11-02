package br.com.cardapio.adapter.in.web.call;

import br.com.cardapio.adapter.in.web.tablesession.TableSessionCookie;
import br.com.cardapio.application.port.in.call.CancelCallUseCase;
import br.com.cardapio.application.port.in.call.ListTableCallsUseCase;
import br.com.cardapio.application.port.in.call.RequestCallCommand;
import br.com.cardapio.application.port.in.call.RequestCallUseCase;
import br.com.cardapio.application.port.in.call.RequestedCall;
import br.com.cardapio.domain.call.model.CallId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Os botões da mesa: chamar o garçom e pedir a conta. Sem login: quem identifica o celular é o
 * cookie table_session (Guia 12). Sem ele, ou vencido, a resposta é 401 e a página abre outra sessão.
 */
@RestController
@RequestMapping("/api/v1/public/tables/{token}/calls")
@Tag(name = "Mesa", description = "O que o cliente vê ao escanear o QR Code")
class PublicCallController {

    private final RequestCallUseCase requestCall;
    private final ListTableCallsUseCase listCalls;
    private final CancelCallUseCase cancelCall;

    PublicCallController(RequestCallUseCase requestCall, ListTableCallsUseCase listCalls,
                         CancelCallUseCase cancelCall) {
        this.requestCall = requestCall;
        this.listCalls = listCalls;
        this.cancelCall = cancelCall;
    }

    @GetMapping
    @Operation(operationId = "listTableCalls", summary = "Os chamados abertos da mesa (no máximo um de cada tipo)")
    @ApiResponse(responseCode = "200", description = "Lista curta, sem paginação: são no máximo dois")
    @ApiResponse(responseCode = "401", description = "Sem sessão válida nesta mesa",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "QR inválido, trocado ou mesa desativada",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    List<TableCallResponse> list(@PathVariable String token,
                                 @CookieValue(name = TableSessionCookie.NAME, required = false) String session) {
        return listCalls.list(token, session).stream().map(TableCallResponse::from).toList();
    }

    @PostMapping
    @Operation(operationId = "requestCall", summary = "Chama o garçom ou pede a conta")
    @ApiResponse(responseCode = "201", description = "Chamado aberto")
    @ApiResponse(responseCode = "200", description = "Já havia um chamado desse tipo aberto: volta o mesmo")
    @ApiResponse(responseCode = "401", description = "Sem sessão válida nesta mesa",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "QR inválido, trocado ou mesa desativada",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<TableCallResponse> request(
            @PathVariable String token,
            @CookieValue(name = TableSessionCookie.NAME, required = false) String session,
            @Valid @RequestBody CallRequest request) {
        RequestedCall result = requestCall.request(
                new RequestCallCommand(token, session, request.type(), request.paymentMethod()));
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(TableCallResponse.from(result.call()));
    }

    @DeleteMapping("/{callId}")
    @Operation(operationId = "cancelCall", summary = "Cancela um chamado aberto da mesa (\"já resolvi\")")
    @ApiResponse(responseCode = "204", description = "Cancelado")
    @ApiResponse(responseCode = "401", description = "Sem sessão válida nesta mesa",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Não é um chamado desta mesa",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "O chamado já foi encerrado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> cancel(@PathVariable String token, @PathVariable UUID callId,
                                @CookieValue(name = TableSessionCookie.NAME, required = false) String session) {
        cancelCall.cancel(token, session, new CallId(callId));
        return ResponseEntity.noContent().build();
    }
}
