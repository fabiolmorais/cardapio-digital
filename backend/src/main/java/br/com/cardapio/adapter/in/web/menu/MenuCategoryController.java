package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.menu.CreateMenuCategoryUseCase;
import br.com.cardapio.application.port.in.menu.DeactivateMenuCategoryUseCase;
import br.com.cardapio.application.port.in.menu.ListMenuCategoriesUseCase;
import br.com.cardapio.application.port.in.menu.MenuCategorySummary;
import br.com.cardapio.application.port.in.menu.RenameMenuCategoryUseCase;
import br.com.cardapio.application.port.in.menu.ReorderMenuCategoriesUseCase;
import br.com.cardapio.application.port.in.menu.ReorderProductsUseCase;
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
@RequestMapping("/api/v1/menu-categories")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
@Tag(name = "Cardápio", description = "Categorias e produtos")
@SecurityRequirement(name = "bearer")
class MenuCategoryController {

    private final ListMenuCategoriesUseCase listCategories;
    private final CreateMenuCategoryUseCase createCategory;
    private final RenameMenuCategoryUseCase renameCategory;
    private final DeactivateMenuCategoryUseCase deactivateCategory;
    private final ReorderMenuCategoriesUseCase reorderCategories;
    private final ReorderProductsUseCase reorderProducts;

    MenuCategoryController(ListMenuCategoriesUseCase listCategories, CreateMenuCategoryUseCase createCategory,
                           RenameMenuCategoryUseCase renameCategory, DeactivateMenuCategoryUseCase deactivateCategory,
                           ReorderMenuCategoriesUseCase reorderCategories, ReorderProductsUseCase reorderProducts) {
        this.listCategories = listCategories;
        this.createCategory = createCategory;
        this.renameCategory = renameCategory;
        this.deactivateCategory = deactivateCategory;
        this.reorderCategories = reorderCategories;
        this.reorderProducts = reorderProducts;
    }

    private static EstablishmentId establishment(Jwt jwt) {
        return AuthenticatedUser.from(jwt).establishmentId();
    }

    @GetMapping
    @Operation(operationId = "listMenuCategories",
            summary = "Categorias ativas, na ordem do cardápio, com a contagem de produtos")
    @ApiResponse(responseCode = "200", description = "Página de categorias")
    PageResponse<MenuCategoryResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Itens por página (1 a 100)")
            @RequestParam(defaultValue = "100") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(listCategories.list(establishment(jwt), new PageQuery(page, size)),
                MenuCategoryResponse::from);
    }

    @PostMapping
    @Operation(operationId = "createMenuCategory", summary = "Cria uma categoria (entra no fim do cardápio)")
    @ApiResponse(responseCode = "201", description = "Criada")
    @ApiResponse(responseCode = "409", description = "Já existe uma categoria com esse nome",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<MenuCategoryResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody MenuCategoryRequest request) {
        MenuCategorySummary category = createCategory.create(establishment(jwt), request.name());
        return ResponseEntity.created(URI.create("/api/v1/menu-categories/" + category.id().value()))
                .body(MenuCategoryResponse.from(category));
    }

    @PatchMapping("/{id}")
    @Operation(operationId = "renameMenuCategory", summary = "Renomeia a categoria")
    @ApiResponse(responseCode = "200", description = "Renomeada")
    @ApiResponse(responseCode = "404", description = "Não é uma categoria ativa deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Já existe uma categoria com esse nome",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    MenuCategoryResponse rename(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @Valid @RequestBody MenuCategoryRequest request) {
        return MenuCategoryResponse.from(
                renameCategory.rename(establishment(jwt), new MenuCategoryId(id), request.name()));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "deactivateMenuCategory", summary = "Desativa a categoria (precisa estar sem produtos)")
    @ApiResponse(responseCode = "204", description = "Desativada")
    @ApiResponse(responseCode = "404", description = "Não é uma categoria deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "A categoria ainda tem produtos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        deactivateCategory.deactivate(establishment(jwt), new MenuCategoryId(id));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/order")
    @Operation(operationId = "reorderMenuCategories",
            summary = "Nova ordem das categorias (todas as ativas, da primeira à última)")
    @ApiResponse(responseCode = "204", description = "Reordenadas")
    @ApiResponse(responseCode = "409", description = "A lista mudou desde que a tela carregou",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> reorder(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReorderRequest request) {
        reorderCategories.reorder(establishment(jwt), request.as(MenuCategoryId::new));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/product-order")
    @Operation(operationId = "reorderProducts",
            summary = "Nova ordem dos produtos da categoria (todos os ativos, do primeiro ao último)")
    @ApiResponse(responseCode = "204", description = "Reordenados")
    @ApiResponse(responseCode = "404", description = "Não é uma categoria ativa deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A lista mudou desde que a tela carregou",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> reorderProducts(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                         @Valid @RequestBody ReorderRequest request) {
        reorderProducts.reorder(establishment(jwt), new MenuCategoryId(id), request.as(ProductId::new));
        return ResponseEntity.noContent().build();
    }
}
