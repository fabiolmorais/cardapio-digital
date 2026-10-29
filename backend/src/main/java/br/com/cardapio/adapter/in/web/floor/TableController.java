package br.com.cardapio.adapter.in.web.floor;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.floor.CreateTableUseCase;
import br.com.cardapio.application.port.in.floor.DisplayFormat;
import br.com.cardapio.application.port.in.floor.ListTablesUseCase;
import br.com.cardapio.application.port.in.floor.RenderTableDisplayUseCase;
import br.com.cardapio.application.port.in.floor.RotateTableTokenUseCase;
import br.com.cardapio.application.port.in.floor.TableSummary;
import br.com.cardapio.application.port.in.floor.UpdateTableUseCase;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
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
@RequestMapping("/api/v1/tables")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
@Tag(name = "Salão", description = "Setores e mesas do restaurante")
@SecurityRequirement(name = "bearer")
@ApiResponse(responseCode = "404", description = "Mesa ou setor não encontrado neste restaurante",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
class TableController {

    private final ListTablesUseCase listTables;
    private final CreateTableUseCase createTable;
    private final UpdateTableUseCase updateTable;
    private final RotateTableTokenUseCase rotateToken;
    private final RenderTableDisplayUseCase renderDisplay;

    TableController(ListTablesUseCase listTables, CreateTableUseCase createTable, UpdateTableUseCase updateTable,
                    RotateTableTokenUseCase rotateToken, RenderTableDisplayUseCase renderDisplay) {
        this.listTables = listTables;
        this.createTable = createTable;
        this.updateTable = updateTable;
        this.rotateToken = rotateToken;
        this.renderDisplay = renderDisplay;
    }

    private static EstablishmentId establishment(Jwt jwt) {
        return AuthenticatedUser.from(jwt).establishmentId();
    }

    @GetMapping
    @Operation(operationId = "listTables", summary = "Mesas do restaurante (ou de um setor), ativas primeiro")
    @ApiResponse(responseCode = "200", description = "Página de mesas")
    PageResponse<TableResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Só as mesas deste setor") @RequestParam(required = false) UUID sectorId,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Itens por página (1 a 100)")
            @RequestParam(defaultValue = "100") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        SectorId sector = sectorId == null ? null : new SectorId(sectorId);
        return PageResponse.from(listTables.list(establishment(jwt), sector, new PageQuery(page, size)),
                TableResponse::from);
    }

    @PostMapping
    @Operation(operationId = "createTable", summary = "Cria uma mesa num setor, já com o token do QR")
    @ApiResponse(responseCode = "201", description = "Criada")
    @ApiResponse(responseCode = "409", description = "Já existe uma mesa com esse nome",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<TableResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateTableRequest request) {
        TableSummary table = createTable.create(request.toCommand(establishment(jwt)));
        return ResponseEntity.created(URI.create("/api/v1/tables/" + table.id().value())).body(TableResponse.from(table));
    }

    @PatchMapping("/{id}")
    @Operation(operationId = "updateTable", summary = "Renomeia, muda de setor, ativa ou desativa a mesa")
    @ApiResponse(responseCode = "200", description = "Alterada")
    @ApiResponse(responseCode = "409", description = "Já existe uma mesa com esse nome",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    TableResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                         @Valid @RequestBody UpdateTableRequest request) {
        return TableResponse.from(updateTable.update(request.toCommand(establishment(jwt), new DiningTableId(id))));
    }

    @PostMapping("/{id}/token-rotation")
    @Operation(operationId = "rotateTableToken",
            summary = "Troca o QR da mesa: token novo, e o display impresso com o antigo para de funcionar")
    @ApiResponse(responseCode = "200", description = "QR trocado")
    TableResponse rotate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return TableResponse.from(rotateToken.rotate(establishment(jwt), new DiningTableId(id)));
    }

    @GetMapping("/{id}/display")
    @Operation(operationId = "downloadTableDisplay",
            summary = "Baixa o QR Code (PNG ou SVG) ou o display pronto para imprimir (PDF) da mesa")
    @ApiResponse(responseCode = "200", description = "O arquivo", content = {
            @Content(mediaType = "image/png", schema = @Schema(type = "string", format = "binary")),
            @Content(mediaType = "image/svg+xml", schema = @Schema(type = "string", format = "binary")),
            @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))})
    ResponseEntity<byte[]> display(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                   @Parameter(description = "PNG ou SVG (só o QR) ou PDF (display completo)")
                                   @RequestParam(defaultValue = "PDF") DisplayFormat format) {
        return Downloads.of(renderDisplay.render(establishment(jwt), new DiningTableId(id), format));
    }
}
