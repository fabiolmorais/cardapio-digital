package br.com.cardapio.adapter.in.web.tablesession;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.tablesession.ListBlockedPhonesUseCase;
import br.com.cardapio.application.port.in.tablesession.UnblockPhoneUseCase;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Os celulares que a equipe bloqueou (pela fila de chamados), e o "Desbloquear". Quem bloqueia, desbloqueia. */
@RestController
@Validated
@RequestMapping("/api/v1/blocked-phones")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
@Tag(name = "Chamados", description = "A fila de chamados da equipe")
@SecurityRequirement(name = "bearer")
class BlockedPhoneController {

    private final ListBlockedPhonesUseCase listBlocked;
    private final UnblockPhoneUseCase unblockPhone;

    BlockedPhoneController(ListBlockedPhonesUseCase listBlocked, UnblockPhoneUseCase unblockPhone) {
        this.listBlocked = listBlocked;
        this.unblockPhone = unblockPhone;
    }

    @GetMapping
    @Operation(operationId = "listBlockedPhones",
            summary = "Celulares bloqueados com a sessão ainda aberta, do bloqueio mais recente")
    @ApiResponse(responseCode = "200", description = "Página de celulares bloqueados")
    PageResponse<BlockedPhoneResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Itens por página (1 a 100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(listBlocked.list(AuthenticatedUser.from(jwt).establishmentId(),
                new PageQuery(page, size)), BlockedPhoneResponse::from);
    }

    @PostMapping("/{id}/unblock")
    @Operation(operationId = "unblockPhone", summary = "Desbloqueia o celular: ele volta a poder chamar e pedir")
    @ApiResponse(responseCode = "204", description = "Desbloqueado (ou já não estava bloqueado)")
    @ApiResponse(responseCode = "404", description = "Não é um celular deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> unblock(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        unblockPhone.unblock(AuthenticatedUser.from(jwt).establishmentId(), new TableSessionId(id));
        return ResponseEntity.noContent().build();
    }
}
