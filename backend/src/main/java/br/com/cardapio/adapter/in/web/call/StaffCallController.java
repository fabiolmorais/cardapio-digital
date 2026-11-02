package br.com.cardapio.adapter.in.web.call;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.call.AcceptCallUseCase;
import br.com.cardapio.application.port.in.call.FinishCallUseCase;
import br.com.cardapio.application.port.in.call.ListCallQueueUseCase;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.call.model.CallId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** A fila de chamados da equipe. A cozinha só vê; aceitar e concluir é de quem atende o salão. */
@RestController
@Validated
@RequestMapping("/api/v1/calls")
@Tag(name = "Chamados", description = "A fila de chamados das mesas")
@SecurityRequirement(name = "bearer")
class StaffCallController {

    private final ListCallQueueUseCase listQueue;
    private final AcceptCallUseCase acceptCall;
    private final FinishCallUseCase finishCall;

    StaffCallController(ListCallQueueUseCase listQueue, AcceptCallUseCase acceptCall, FinishCallUseCase finishCall) {
        this.listQueue = listQueue;
        this.acceptCall = acceptCall;
        this.finishCall = finishCall;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF', 'KITCHEN')")
    @Operation(operationId = "listCallQueue", summary = "Chamados abertos, do mais antigo para o mais novo")
    @ApiResponse(responseCode = "200", description = "Página da fila")
    PageResponse<StaffCallResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Itens por página (1 a 100)")
            @RequestParam(defaultValue = "50") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        var user = AuthenticatedUser.from(jwt);
        return PageResponse.from(listQueue.list(user.establishmentId(), new PageQuery(page, size)),
                StaffCallResponse::from);
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    @Operation(operationId = "acceptCall", summary = "\"Estou indo\": o chamado fica com quem aceitou")
    @ApiResponse(responseCode = "200", description = "Aceito")
    @ApiResponse(responseCode = "404", description = "Não é um chamado deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Outra pessoa aceitou antes, ou o chamado já foi encerrado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    StaffCallResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var user = AuthenticatedUser.from(jwt);
        return StaffCallResponse.from(acceptCall.accept(user.establishmentId(), new CallId(id), user.userId()));
    }

    @PostMapping("/{id}/finish")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    @Operation(operationId = "finishCall", summary = "Atendido: o chamado sai da fila")
    @ApiResponse(responseCode = "200", description = "Concluído")
    @ApiResponse(responseCode = "404", description = "Não é um chamado deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ainda não foi aceito, ou já foi encerrado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    StaffCallResponse finish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return StaffCallResponse.from(finishCall.finish(AuthenticatedUser.from(jwt).establishmentId(), new CallId(id)));
    }
}
