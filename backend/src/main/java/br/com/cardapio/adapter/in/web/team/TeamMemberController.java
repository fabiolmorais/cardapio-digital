package br.com.cardapio.adapter.in.web.team;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.team.*;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.MembershipId;
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
 * Equipe do restaurante de quem está logado. Só dono e gerente entram aqui; o que cada um
 * pode alterar é decidido no domínio (TeamPolicy). O restaurante sai sempre do token.
 */
@RestController
@Validated
@RequestMapping("/api/v1/team-members")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
@Tag(name = "Equipe", description = "Cadastro da equipe do restaurante (dono e gerente)")
@SecurityRequirement(name = "bearer")
@ApiResponse(responseCode = "403", description = "Papel sem permissão para esta ação",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
class TeamMemberController {

    private final ListTeamUseCase listTeam;
    private final AddTeamMemberUseCase addTeamMember;
    private final UpdateTeamMemberUseCase updateTeamMember;
    private final DeactivateTeamMemberUseCase deactivateTeamMember;
    private final ReactivateTeamMemberUseCase reactivateTeamMember;

    TeamMemberController(ListTeamUseCase listTeam, AddTeamMemberUseCase addTeamMember,
                         UpdateTeamMemberUseCase updateTeamMember, DeactivateTeamMemberUseCase deactivateTeamMember,
                         ReactivateTeamMemberUseCase reactivateTeamMember) {
        this.listTeam = listTeam;
        this.addTeamMember = addTeamMember;
        this.updateTeamMember = updateTeamMember;
        this.deactivateTeamMember = deactivateTeamMember;
        this.reactivateTeamMember = reactivateTeamMember;
    }

    @GetMapping
    @Operation(operationId = "listTeamMembers", summary = "Lista a equipe (ativos primeiro, depois por nome)")
    @ApiResponse(responseCode = "200", description = "Página da equipe")
    PageResponse<TeamMemberResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Itens por página (1 a 100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        return PageResponse.from(listTeam.list(user.establishmentId(), new PageQuery(page, size)),
                TeamMemberResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getTeamMember", summary = "Uma pessoa da equipe")
    @ApiResponse(responseCode = "200", description = "A pessoa")
    @ApiResponse(responseCode = "404", description = "Não está na equipe deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    TeamMemberResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        return TeamMemberResponse.from(listTeam.get(user.establishmentId(), new MembershipId(id)));
    }

    @PostMapping
    @Operation(operationId = "addTeamMember", summary = "Cadastra uma pessoa na equipe")
    @ApiResponse(responseCode = "201", description = "Cadastrada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou PIN fácil de adivinhar",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<TeamMemberResponse> add(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody AddTeamMemberRequest request) {
        TeamMemberSummary added = addTeamMember.add(request.toCommand(AuthenticatedUser.from(jwt)));
        URI location = URI.create("/api/v1/team-members/" + added.id().value());
        return ResponseEntity.created(location).body(TeamMemberResponse.from(added));
    }

    @PatchMapping("/{id}")
    @Operation(operationId = "updateTeamMember", summary = "Muda o papel e/ou o PIN de uma pessoa da equipe")
    @ApiResponse(responseCode = "200", description = "Alterada")
    @ApiResponse(responseCode = "404", description = "Não está na equipe deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    TeamMemberResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                              @Valid @RequestBody UpdateTeamMemberRequest request) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        return TeamMemberResponse.from(updateTeamMember.update(request.toCommand(user, new MembershipId(id))));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "deactivateTeamMember",
            summary = "Desliga uma pessoa da equipe (o registro fica, para o histórico)")
    @ApiResponse(responseCode = "204", description = "Desligada")
    @ApiResponse(responseCode = "404", description = "Não está na equipe deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        deactivateTeamMember.deactivate(
                new DeactivateTeamMemberCommand(user.establishmentId(), user.role(), new MembershipId(id)));
        return ResponseEntity.noContent().build();
    }

    /** Desfaz o DELETE: a pessoa volta com o mesmo registro, papel e PIN. */
    @PostMapping("/{id}/reactivation")
    @Operation(operationId = "reactivateTeamMember", summary = "Religa uma pessoa desligada da equipe")
    @ApiResponse(responseCode = "200", description = "Religada (ou já estava ativa)")
    @ApiResponse(responseCode = "404", description = "Não está na equipe deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    TeamMemberResponse reactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        return TeamMemberResponse.from(reactivateTeamMember.reactivate(
                new ReactivateTeamMemberCommand(user.establishmentId(), user.role(), new MembershipId(id))));
    }
}
