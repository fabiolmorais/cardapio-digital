package br.com.cardapio.adapter.in.web.floor;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.floor.CreateSectorUseCase;
import br.com.cardapio.application.port.in.floor.DeactivateSectorUseCase;
import br.com.cardapio.application.port.in.floor.ListSectorsUseCase;
import br.com.cardapio.application.port.in.floor.RenameSectorUseCase;
import br.com.cardapio.application.port.in.floor.RenderSectorDisplaysUseCase;
import br.com.cardapio.application.port.in.floor.SectorSummary;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
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

@RestController
@Validated
@RequestMapping("/api/v1/sectors")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
@Tag(name = "Salão", description = "Setores e mesas do restaurante")
@SecurityRequirement(name = "bearer")
class SectorController {

    private final ListSectorsUseCase listSectors;
    private final CreateSectorUseCase createSector;
    private final RenameSectorUseCase renameSector;
    private final DeactivateSectorUseCase deactivateSector;
    private final RenderSectorDisplaysUseCase renderDisplays;

    SectorController(ListSectorsUseCase listSectors, CreateSectorUseCase createSector,
                     RenameSectorUseCase renameSector, DeactivateSectorUseCase deactivateSector,
                     RenderSectorDisplaysUseCase renderDisplays) {
        this.listSectors = listSectors;
        this.createSector = createSector;
        this.renameSector = renameSector;
        this.deactivateSector = deactivateSector;
        this.renderDisplays = renderDisplays;
    }

    private static EstablishmentId establishment(Jwt jwt) {
        return AuthenticatedUser.from(jwt).establishmentId();
    }

    @GetMapping
    @Operation(operationId = "listSectors", summary = "Setores ativos, na ordem de exibição, com a contagem de mesas")
    @ApiResponse(responseCode = "200", description = "Página de setores")
    PageResponse<SectorResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Itens por página (1 a 100)")
            @RequestParam(defaultValue = "50") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(listSectors.list(establishment(jwt), new PageQuery(page, size)), SectorResponse::from);
    }

    @PostMapping
    @Operation(operationId = "createSector", summary = "Cria um setor (entra no fim da lista)")
    @ApiResponse(responseCode = "201", description = "Criado")
    @ApiResponse(responseCode = "409", description = "Já existe um setor com esse nome",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<SectorResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SectorRequest request) {
        SectorSummary sector = createSector.create(establishment(jwt), request.name());
        return ResponseEntity.created(URI.create("/api/v1/sectors/" + sector.id().value()))
                .body(SectorResponse.from(sector));
    }

    @PatchMapping("/{id}")
    @Operation(operationId = "renameSector", summary = "Renomeia o setor")
    @ApiResponse(responseCode = "200", description = "Renomeado")
    @ApiResponse(responseCode = "404", description = "Não é um setor ativo deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    SectorResponse rename(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                          @Valid @RequestBody SectorRequest request) {
        return SectorResponse.from(renameSector.rename(establishment(jwt), new SectorId(id), request.name()));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "deactivateSector", summary = "Desativa o setor (precisa estar sem mesas ativas)")
    @ApiResponse(responseCode = "204", description = "Desativado")
    @ApiResponse(responseCode = "422", description = "O setor ainda tem mesas ativas",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        deactivateSector.deactivate(establishment(jwt), new SectorId(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/displays")
    @Operation(operationId = "downloadSectorDisplays", summary = "PDF com o display de cada mesa ativa do setor")
    @ApiResponse(responseCode = "200", description = "O PDF, uma mesa por página",
            content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "404", description = "Não é um setor ativo deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "O setor não tem mesas ativas",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<byte[]> displays(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return Downloads.of(renderDisplays.render(establishment(jwt), new SectorId(id)));
    }
}
