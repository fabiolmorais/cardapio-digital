package br.com.cardapio.adapter.in.web.rodizio;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.rodizio.ListRodiziosUseCase;
import br.com.cardapio.application.port.in.rodizio.ManageRodiziosUseCase;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

/**
 * O cadastro dos rodízios. Dono e gerente criam e editam; o garçom só lista (é ele quem ativa o
 * rodízio na mesa, no Guia 25).
 */
@RestController
@Validated
@RequestMapping("/api/v1/rodizios")
@Tag(name = "Rodízio", description = "O cadastro dos rodízios: faixas, preços, produtos e regras")
@SecurityRequirement(name = "bearer")
class RodizioController {

    private final ListRodiziosUseCase listRodizios;
    private final ManageRodiziosUseCase manageRodizios;

    RodizioController(ListRodiziosUseCase listRodizios, ManageRodiziosUseCase manageRodizios) {
        this.listRodizios = listRodizios;
        this.manageRodizios = manageRodizios;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    @Operation(operationId = "listRodizios", summary = "Os rodízios ativos, por nome")
    @ApiResponse(responseCode = "200", description = "Página dos rodízios")
    PageResponse<RodizioResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Rodízios por página (1 a 100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(listRodizios.list(AuthenticatedUser.from(jwt).establishmentId(),
                new PageQuery(page, size)), RodizioResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    @Operation(operationId = "getRodizio", summary = "Um rodízio, com as faixas, os preços e os produtos")
    @ApiResponse(responseCode = "200", description = "O rodízio")
    @ApiResponse(responseCode = "404", description = "Não é um rodízio ativo deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    RodizioResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return RodizioResponse.from(listRodizios.get(AuthenticatedUser.from(jwt).establishmentId(),
                new RodizioId(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(operationId = "createRodizio", summary = "Cria um rodízio")
    @ApiResponse(responseCode = "201", description = "Criado")
    @ApiResponse(responseCode = "400", description = "Campo inválido, preços especiais que se sobrepõem, "
            + "ou produto que não é do cardápio",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Já existe um rodízio com este nome",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "O restaurante já tem 10 rodízios ativos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<RodizioResponse> create(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody RodizioRequest request) {
        Rodizio created = manageRodizios.create(AuthenticatedUser.from(jwt).establishmentId(), request.toForm());
        return ResponseEntity.created(URI.create("/api/v1/rodizios/" + created.id().value()))
                .body(RodizioResponse.from(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(operationId = "updateRodizio", summary = "Troca o rodízio inteiro (faixas, preços, produtos, regras)")
    @ApiResponse(responseCode = "200", description = "Salvo")
    @ApiResponse(responseCode = "400", description = "Campo inválido",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Não é um rodízio ativo deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Já existe outro rodízio com este nome",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    RodizioResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                           @Valid @RequestBody RodizioRequest request) {
        return RodizioResponse.from(manageRodizios.update(AuthenticatedUser.from(jwt).establishmentId(),
                new RodizioId(id), request.toForm()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(operationId = "deactivateRodizio", summary = "Pausa o rodízio (o cadastro fica, para reativar)")
    @ApiResponse(responseCode = "204", description = "Pausado")
    @ApiResponse(responseCode = "404", description = "Não é um rodízio deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        manageRodizios.deactivate(AuthenticatedUser.from(jwt).establishmentId(), new RodizioId(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(operationId = "reactivateRodizio", summary = "Volta a oferecer o rodízio pausado")
    @ApiResponse(responseCode = "200", description = "Ativo de novo")
    @ApiResponse(responseCode = "404", description = "Não é um rodízio deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Já existe um rodízio ativo com este nome, ou o limite de ativos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    RodizioResponse reactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return RodizioResponse.from(manageRodizios.reactivate(AuthenticatedUser.from(jwt).establishmentId(),
                new RodizioId(id)));
    }
}
