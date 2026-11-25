package br.com.cardapio.adapter.in.web.unit;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.unit.CreateUnitCommand;
import br.com.cardapio.application.port.in.unit.ManageUnitsUseCase;
import br.com.cardapio.application.port.in.unit.UnitAllowance;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.UnitSummary;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * As unidades da empresa (RF31, Enterprise). Só o dono; a empresa sai do restaurante do token, e
 * nunca do pedido.
 */
@RestController
@Validated
@RequestMapping("/api/v1/units")
@PreAuthorize("hasRole('OWNER')")
@Tag(name = "Unidades", description = "As unidades da empresa (redes e franquias)")
@SecurityRequirement(name = "bearer")
@ApiResponse(responseCode = "403", description = "Só o dono cuida das unidades",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
class UnitController {

    private final ManageUnitsUseCase units;

    UnitController(ManageUnitsUseCase units) {
        this.units = units;
    }

    @GetMapping
    @Operation(operationId = "listUnits", summary = "As unidades ativas da empresa, por nome")
    @ApiResponse(responseCode = "200", description = "Página das unidades")
    PageResponse<UnitResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Itens por página (1 a 100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        return PageResponse.from(units.list(user.establishmentId(), new PageQuery(page, size)),
                u -> UnitResponse.from(u, user));
    }

    @GetMapping("/allowance")
    @Operation(operationId = "getUnitAllowance", summary = "Quantas unidades a empresa tem e quantas o plano permite")
    @ApiResponse(responseCode = "200", description = "O uso e o limite")
    UnitAllowanceResponse allowance(@AuthenticationPrincipal Jwt jwt) {
        return UnitAllowanceResponse.from(units.allowance(AuthenticatedUser.from(jwt).establishmentId()));
    }

    @PostMapping
    @Operation(operationId = "createUnit", summary = "Cria uma unidade na empresa, vazia, com quem pediu como dono")
    @ApiResponse(responseCode = "201", description = "Criada")
    @ApiResponse(responseCode = "400", description = "Nome vazio ou fuso fora da lista",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "O plano não permite mais unidades",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<UnitResponse> create(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody CreateUnitRequest request) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        UnitSummary created = units.create(new CreateUnitCommand(user.establishmentId(), user.userId(), user.role(),
                request.name(), request.timeZone()));
        return ResponseEntity.created(URI.create("/api/v1/units/" + created.id().value()))
                .body(UnitResponse.from(created, user));
    }

    @Schema(name = "CreateUnitRequest", description = "Uma unidade nova da empresa")
    record CreateUnitRequest(
            @Schema(requiredMode = REQUIRED, example = "Sakura Moema") @NotBlank @Size(max = 120) String name,
            @Schema(requiredMode = REQUIRED, example = "America/Sao_Paulo",
                    description = "America/Sao_Paulo, America/Manaus, America/Cuiaba, America/Rio_Branco ou America/Noronha")
            @NotBlank @Size(max = 40) String timeZone) {
    }

    @Schema(name = "Unit", description = "Uma unidade da empresa")
    record UnitResponse(
            @Schema(requiredMode = REQUIRED) UUID id,
            @Schema(requiredMode = REQUIRED) String name,
            @Schema(requiredMode = REQUIRED, description = "O endereço da mesa: /m/… usa o QR de cada mesa; este é o nome curto")
            String slug,
            @Schema(requiredMode = REQUIRED) Instant createdAt,
            @Schema(requiredMode = REQUIRED, description = "Mesas ativas") long activeTables,
            @Schema(requiredMode = REQUIRED, description = "Pessoas ativas na equipe") long activeTeam,
            @Schema(requiredMode = REQUIRED, description = "É a unidade em que quem pediu está agora") boolean current) {

        static UnitResponse from(UnitSummary u, AuthenticatedUser user) {
            return new UnitResponse(u.id().value(), u.name(), u.slug().value(), u.createdAt(), u.activeTables(),
                    u.activeTeam(), u.id().equals(user.establishmentId()));
        }
    }

    @Schema(name = "UnitAllowance")
    record UnitAllowanceResponse(
            @Schema(requiredMode = REQUIRED, example = "Enterprise") String planName,
            @Schema(requiredMode = REQUIRED, description = "Unidades ativas da empresa") long used,
            @Schema(requiredMode = REQUIRED, nullable = true, description = "Nulo = sem limite") Integer max,
            @Schema(requiredMode = REQUIRED, description = "Ainda cabe mais uma") boolean canAddMore) {

        static UnitAllowanceResponse from(UnitAllowance a) {
            return new UnitAllowanceResponse(a.planName(), a.used(), a.max(), a.canAddMore());
        }
    }
}
