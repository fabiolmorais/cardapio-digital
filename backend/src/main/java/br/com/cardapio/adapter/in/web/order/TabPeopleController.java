package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.order.TabPeopleUseCase;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabPeople;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Quantas pessoas estão na mesa fora do rodízio (REL-04): o garçom informa, se quiser. */
@RestController
@RequestMapping("/api/v1/tabs/{id}/people")
@Tag(name = "Comandas", description = "As comandas das mesas e o fechamento da conta")
@SecurityRequirement(name = "bearer")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
class TabPeopleController {

    private final TabPeopleUseCase people;

    TabPeopleController(TabPeopleUseCase people) {
        this.people = people;
    }

    @GetMapping
    @Operation(operationId = "getTabPeople", summary = "Quantas pessoas estão na mesa")
    @ApiResponse(responseCode = "200", description = "O número, ou null se ninguém informou")
    @ApiResponse(responseCode = "404", description = "Não é uma comanda deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    TabPeopleResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return TabPeopleResponse.of(people.get(AuthenticatedUser.from(jwt).establishmentId(), new TabId(id)));
    }

    @PutMapping
    @Operation(operationId = "setTabPeople", summary = "Informa (ou apaga) quantas pessoas estão na mesa")
    @ApiResponse(responseCode = "200", description = "Salvo")
    @ApiResponse(responseCode = "400", description = "Fora de 1 a 50",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Não é uma comanda deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A comanda já foi fechada",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    TabPeopleResponse set(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                          @Valid @RequestBody TabPeopleRequest request) {
        return TabPeopleResponse.of(people.set(AuthenticatedUser.from(jwt).establishmentId(), new TabId(id),
                request.people() == null ? null : new TabPeople(request.people())));
    }
}
