package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.menu.ChangeOptionItemAvailabilityUseCase;
import br.com.cardapio.application.port.in.menu.CreateOptionGroupUseCase;
import br.com.cardapio.application.port.in.menu.DeactivateOptionGroupUseCase;
import br.com.cardapio.application.port.in.menu.GetOptionGroupUseCase;
import br.com.cardapio.application.port.in.menu.ListOptionGroupsUseCase;
import br.com.cardapio.application.port.in.menu.UpdateOptionGroupUseCase;
import br.com.cardapio.application.port.in.menu.OptionGroupSummary;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.OptionGroupId;
import br.com.cardapio.domain.menu.model.OptionItemId;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/** A biblioteca de grupos de opções do restaurante (ADR 0010): sabores, ponto, adicionais. */
@RestController
@Validated
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
@Tag(name = "Cardápio", description = "Categorias e produtos")
@SecurityRequirement(name = "bearer")
class OptionGroupController {

    private static final String GROUPS = "/api/v1/option-groups";

    private final ListOptionGroupsUseCase listGroups;
    private final GetOptionGroupUseCase getGroup;
    private final CreateOptionGroupUseCase createGroup;
    private final UpdateOptionGroupUseCase updateGroup;
    private final DeactivateOptionGroupUseCase deactivateGroup;
    private final ChangeOptionItemAvailabilityUseCase changeItemAvailability;

    OptionGroupController(ListOptionGroupsUseCase listGroups, GetOptionGroupUseCase getGroup,
                          CreateOptionGroupUseCase createGroup, UpdateOptionGroupUseCase updateGroup,
                          DeactivateOptionGroupUseCase deactivateGroup,
                          ChangeOptionItemAvailabilityUseCase changeItemAvailability) {
        this.listGroups = listGroups;
        this.getGroup = getGroup;
        this.createGroup = createGroup;
        this.updateGroup = updateGroup;
        this.deactivateGroup = deactivateGroup;
        this.changeItemAvailability = changeItemAvailability;
    }

    private static EstablishmentId establishment(Jwt jwt) {
        return AuthenticatedUser.from(jwt).establishmentId();
    }

    @GetMapping(GROUPS)
    @Operation(operationId = "listOptionGroups", summary = "Os grupos de opções ativos, por nome, com as opções")
    @ApiResponse(responseCode = "200", description = "Página de grupos")
    PageResponse<OptionDtos.Group> list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número da página, começando em 0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Itens por página (1 a 100)")
            @RequestParam(defaultValue = "100") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(listGroups.list(establishment(jwt), new PageQuery(page, size)),
                OptionDtos.Group::from);
    }

    @GetMapping(GROUPS + "/{id}")
    @Operation(operationId = "getOptionGroup", summary = "Um grupo de opções, para a tela de edição")
    @ApiResponse(responseCode = "200", description = "O grupo")
    @ApiResponse(responseCode = "404", description = "Não é um grupo ativo deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    OptionDtos.Group get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return OptionDtos.Group.from(getGroup.get(establishment(jwt), new OptionGroupId(id)));
    }

    @PostMapping(GROUPS)
    @Operation(operationId = "createOptionGroup", summary = "Cria um grupo de opções na biblioteca")
    @ApiResponse(responseCode = "201", description = "Criado")
    @ApiResponse(responseCode = "400", description = "Regras ou opções inválidas",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<OptionDtos.Group> create(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody OptionGroupRequest request) {
        OptionGroupSummary group = createGroup.create(establishment(jwt), request.toForm());
        return ResponseEntity.created(URI.create(GROUPS + "/" + group.group().id().value()))
                .body(OptionDtos.Group.from(group));
    }

    @PutMapping(GROUPS + "/{id}")
    @Operation(operationId = "updateOptionGroup",
            summary = "Salva o grupo e as opções (vale para todos os produtos que usam o grupo)")
    @ApiResponse(responseCode = "200", description = "Salvo")
    @ApiResponse(responseCode = "404", description = "Grupo ou opção não encontrado neste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    OptionDtos.Group update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                            @Valid @RequestBody OptionGroupRequest request) {
        return OptionDtos.Group.from(updateGroup.update(establishment(jwt), new OptionGroupId(id), request.toForm()));
    }

    @DeleteMapping(GROUPS + "/{id}")
    @Operation(operationId = "deactivateOptionGroup",
            summary = "Tira o grupo da biblioteca e dos produtos (o registro fica, para os pedidos antigos)")
    @ApiResponse(responseCode = "204", description = "Desativado")
    @ApiResponse(responseCode = "404", description = "Não é um grupo ativo deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        deactivateGroup.deactivate(establishment(jwt), new OptionGroupId(id));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/option-items/{id}/availability")
    @Operation(operationId = "changeOptionItemAvailability",
            summary = "Opção disponível ou esgotada (em todos os produtos que usam o grupo)")
    @ApiResponse(responseCode = "200", description = "Alterada")
    @ApiResponse(responseCode = "404", description = "Não é uma opção ativa deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    OptionDtos.Item changeItemAvailability(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                           @Valid @RequestBody AvailabilityRequest request) {
        return OptionDtos.Item.from(changeItemAvailability.change(establishment(jwt), new OptionItemId(id),
                request.available()));
    }
}
