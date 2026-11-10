package br.com.cardapio.adapter.in.web.rodizio;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.rodizio.ManageRodiziosUseCase;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.rodizio.model.RodizioId;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.stream.Collectors;

/** A chave "Incluído no rodízio" da tela do produto: um produto, em quais rodízios. */
@RestController
@Tag(name = "Rodízio", description = "O cadastro dos rodízios: faixas, preços, produtos e regras")
@SecurityRequirement(name = "bearer")
class ProductRodiziosController {

    private final ManageRodiziosUseCase manageRodizios;

    ProductRodiziosController(ManageRodiziosUseCase manageRodizios) {
        this.manageRodizios = manageRodizios;
    }

    @PutMapping("/api/v1/products/{id}/rodizios")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(operationId = "setProductRodizios",
            summary = "Em quais rodízios o produto fica (entra nos escolhidos e sai dos outros)")
    @ApiResponse(responseCode = "200", description = "Os rodízios em que ele ficou")
    @ApiResponse(responseCode = "404", description = "O produto ou algum rodízio não é deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ProductRodiziosResponse set(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @Valid @RequestBody ProductRodiziosRequest request) {
        var included = manageRodizios.setProductRodizios(AuthenticatedUser.from(jwt).establishmentId(),
                new ProductId(id), request.rodizioIds().stream().map(RodizioId::new).collect(Collectors.toSet()));
        return new ProductRodiziosResponse(included.stream().map(RodizioId::value).sorted().toList());
    }
}
