package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.menu.ChangeVariantAvailabilityUseCase;
import br.com.cardapio.application.port.in.menu.GetProductOptionsUseCase;
import br.com.cardapio.application.port.in.menu.UpdateProductOptionsUseCase;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductVariantId;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Os tamanhos e os grupos de cada produto (ADR 0010). */
@RestController
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
@Tag(name = "Cardápio", description = "Categorias e produtos")
@SecurityRequirement(name = "bearer")
class ProductOptionsController {

    private final GetProductOptionsUseCase getOptions;
    private final UpdateProductOptionsUseCase updateOptions;
    private final ChangeVariantAvailabilityUseCase changeVariantAvailability;

    ProductOptionsController(GetProductOptionsUseCase getOptions, UpdateProductOptionsUseCase updateOptions,
                             ChangeVariantAvailabilityUseCase changeVariantAvailability) {
        this.getOptions = getOptions;
        this.updateOptions = updateOptions;
        this.changeVariantAvailability = changeVariantAvailability;
    }

    private static EstablishmentId establishment(Jwt jwt) {
        return AuthenticatedUser.from(jwt).establishmentId();
    }

    @GetMapping("/api/v1/products/{id}/options")
    @Operation(operationId = "getProductOptions", summary = "Os tamanhos e os grupos de opções do produto")
    @ApiResponse(responseCode = "200", description = "A configuração (vazia no produto de sempre)")
    @ApiResponse(responseCode = "404", description = "Não é um produto ativo deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ProductOptionsResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ProductOptionsResponse.from(getOptions.get(establishment(jwt), new ProductId(id)));
    }

    @PutMapping("/api/v1/products/{id}/options")
    @Operation(operationId = "updateProductOptions",
            summary = "Salva os tamanhos e os grupos do produto (tudo de uma vez; o que sai é desativado)")
    @ApiResponse(responseCode = "200", description = "Salvo")
    @ApiResponse(responseCode = "400", description = "Tamanhos, limites ou preços inválidos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Produto, tamanho, grupo ou opção não encontrado neste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ProductOptionsResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                  @Valid @RequestBody ProductOptionsRequest request) {
        return ProductOptionsResponse.from(updateOptions.update(establishment(jwt), new ProductId(id),
                request.toForm()));
    }

    @PatchMapping("/api/v1/product-variants/{id}/availability")
    @Operation(operationId = "changeVariantAvailability", summary = "Tamanho disponível ou esgotado")
    @ApiResponse(responseCode = "200", description = "Alterado")
    @ApiResponse(responseCode = "404", description = "Não é um tamanho ativo deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    OptionDtos.Variant changeVariantAvailability(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                 @Valid @RequestBody AvailabilityRequest request) {
        return OptionDtos.Variant.from(changeVariantAvailability.change(establishment(jwt),
                new ProductVariantId(id), request.available()));
    }
}
