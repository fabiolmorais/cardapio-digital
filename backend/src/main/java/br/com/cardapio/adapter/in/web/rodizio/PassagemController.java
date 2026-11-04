package br.com.cardapio.adapter.in.web.rodizio;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.rodizio.ListPassagemRequestsUseCase;
import br.com.cardapio.application.port.in.rodizio.ListPassagemTablesUseCase;
import br.com.cardapio.application.port.shared.PageQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A tela dos passadores: as mesas no rodízio de passagem, a cor de cada cartão e os cortes
 * pedidos. Toda a equipe vê, inclusive a cozinha (a churrasqueira): quem passa com o espeto é quem
 * está no salão. Entregar e cancelar um corte usam as rotas do item (Guia 19).
 */
@RestController
@Validated
@RequestMapping("/api/v1")
@Tag(name = "Rodízio", description = "O cadastro dos rodízios: faixas, preços, produtos e regras")
@SecurityRequirement(name = "bearer")
class PassagemController {

    private final ListPassagemTablesUseCase listTables;
    private final ListPassagemRequestsUseCase listRequests;

    PassagemController(ListPassagemTablesUseCase listTables, ListPassagemRequestsUseCase listRequests) {
        this.listTables = listTables;
        this.listRequests = listRequests;
    }

    @GetMapping("/passagem-requests")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF', 'KITCHEN')")
    @Operation(operationId = "listPassagemRequests",
            summary = "Os cortes pedidos no rodízio de passagem, ainda a caminho, do mais antigo ao mais novo")
    @ApiResponse(responseCode = "200", description = "Página dos cortes")
    PageResponse<PassagemRequestResponse> requests(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Cortes por página (1 a 100)")
            @RequestParam(defaultValue = "100") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(listRequests.list(AuthenticatedUser.from(jwt).establishmentId(),
                new PageQuery(page, size)), PassagemRequestResponse::from);
    }

    @GetMapping("/passagem-tables")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF', 'KITCHEN')")
    @Operation(operationId = "listPassagemTables",
            summary = "As mesas abertas no rodízio de passagem, as de cartão vermelho primeiro")
    @ApiResponse(responseCode = "200", description = "Página das mesas")
    PageResponse<PassagemTableResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Mesas por página (1 a 100)")
            @RequestParam(defaultValue = "100") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(listTables.list(AuthenticatedUser.from(jwt).establishmentId(),
                new PageQuery(page, size)), PassagemTableResponse::from);
    }
}
