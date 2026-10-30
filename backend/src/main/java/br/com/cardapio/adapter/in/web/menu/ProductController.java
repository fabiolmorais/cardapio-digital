package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.menu.ChangeProductAvailabilityUseCase;
import br.com.cardapio.application.port.in.menu.CreateProductUseCase;
import br.com.cardapio.application.port.in.menu.DeactivateProductUseCase;
import br.com.cardapio.application.port.in.menu.GetProductUseCase;
import br.com.cardapio.application.port.in.menu.ListProductsUseCase;
import br.com.cardapio.application.port.in.menu.ProductSummary;
import br.com.cardapio.application.port.in.menu.UpdateProductUseCase;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.ProductId;
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
import jakarta.validation.constraints.Size;
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
@RequestMapping("/api/v1/products")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
@Tag(name = "Cardápio", description = "Categorias e produtos")
@SecurityRequirement(name = "bearer")
class ProductController {

    private final ListProductsUseCase listProducts;
    private final GetProductUseCase getProduct;
    private final CreateProductUseCase createProduct;
    private final UpdateProductUseCase updateProduct;
    private final ChangeProductAvailabilityUseCase changeAvailability;
    private final DeactivateProductUseCase deactivateProduct;

    ProductController(ListProductsUseCase listProducts, GetProductUseCase getProduct,
                      CreateProductUseCase createProduct, UpdateProductUseCase updateProduct,
                      ChangeProductAvailabilityUseCase changeAvailability,
                      DeactivateProductUseCase deactivateProduct) {
        this.listProducts = listProducts;
        this.getProduct = getProduct;
        this.createProduct = createProduct;
        this.updateProduct = updateProduct;
        this.changeAvailability = changeAvailability;
        this.deactivateProduct = deactivateProduct;
    }

    private static EstablishmentId establishment(Jwt jwt) {
        return AuthenticatedUser.from(jwt).establishmentId();
    }

    @GetMapping
    @Operation(operationId = "listProducts",
            summary = "Produtos ativos na ordem do cardápio, de uma categoria ou de todas, com busca pelo nome")
    @ApiResponse(responseCode = "200", description = "Página de produtos")
    PageResponse<ProductResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Só os produtos desta categoria") @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Parte do nome, sem diferenciar maiúsculas")
            @RequestParam(required = false) @Size(max = 80) String q,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Itens por página (1 a 100)")
            @RequestParam(defaultValue = "100") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        MenuCategoryId category = categoryId == null ? null : new MenuCategoryId(categoryId);
        return PageResponse.from(listProducts.list(establishment(jwt), category, q, new PageQuery(page, size)),
                ProductResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getProduct", summary = "Um produto, para a tela de edição")
    @ApiResponse(responseCode = "200", description = "O produto")
    @ApiResponse(responseCode = "404", description = "Não é um produto ativo deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ProductResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ProductResponse.from(getProduct.get(establishment(jwt), new ProductId(id)));
    }

    @PostMapping
    @Operation(operationId = "createProduct", summary = "Cria um produto (entra no fim da categoria)")
    @ApiResponse(responseCode = "201", description = "Criado")
    @ApiResponse(responseCode = "404", description = "A categoria não é uma categoria ativa deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<ProductResponse> create(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody ProductRequest request) {
        ProductSummary product = createProduct.create(establishment(jwt), request.toForm());
        return ResponseEntity.created(URI.create("/api/v1/products/" + product.id().value()))
                .body(ProductResponse.from(product));
    }

    @PutMapping("/{id}")
    @Operation(operationId = "updateProduct",
            summary = "Salva o formulário do produto (todos os campos; promoção vazia apaga a promoção)")
    @ApiResponse(responseCode = "200", description = "Salvo")
    @ApiResponse(responseCode = "404", description = "Produto ou categoria não encontrado neste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ProductResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                           @Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(updateProduct.update(establishment(jwt), new ProductId(id), request.toForm()));
    }

    @PatchMapping("/{id}/availability")
    @Operation(operationId = "changeProductAvailability", summary = "Disponível ou \"Esgotado hoje\"")
    @ApiResponse(responseCode = "200", description = "Alterado")
    @ApiResponse(responseCode = "404", description = "Não é um produto ativo deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ProductResponse changeAvailability(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                       @Valid @RequestBody AvailabilityRequest request) {
        return ProductResponse.from(changeAvailability.change(establishment(jwt), new ProductId(id),
                request.available()));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "deactivateProduct", summary = "Tira o produto do cardápio (o registro fica)")
    @ApiResponse(responseCode = "204", description = "Desativado")
    @ApiResponse(responseCode = "404", description = "Não é um produto deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        deactivateProduct.deactivate(establishment(jwt), new ProductId(id));
        return ResponseEntity.noContent().build();
    }
}
