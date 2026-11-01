package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.menu.AddProductMediaUseCase;
import br.com.cardapio.application.port.in.menu.ListProductMediaUseCase;
import br.com.cardapio.application.port.in.menu.ProductMediaSummary;
import br.com.cardapio.application.port.in.menu.RemoveProductMediaUseCase;
import br.com.cardapio.application.port.in.menu.ReorderProductMediaUseCase;
import br.com.cardapio.application.port.in.menu.StartMediaUploadUseCase;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.PhotoFile;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductMediaId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

/**
 * Fotos do produto. O arquivo não passa pelo back: o navegador pede uma URL assinada (uploads),
 * envia direto para o armazenamento e depois avisa (POST media), quando o back confere e gera os tamanhos.
 */
@RestController
@RequestMapping("/api/v1/products/{productId}")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
@Tag(name = "Cardápio", description = "Categorias e produtos")
@SecurityRequirement(name = "bearer")
class ProductMediaController {

    private final StartMediaUploadUseCase startUpload;
    private final AddProductMediaUseCase addMedia;
    private final ListProductMediaUseCase listMedia;
    private final RemoveProductMediaUseCase removeMedia;
    private final ReorderProductMediaUseCase reorderMedia;

    ProductMediaController(StartMediaUploadUseCase startUpload, AddProductMediaUseCase addMedia,
                           ListProductMediaUseCase listMedia, RemoveProductMediaUseCase removeMedia,
                           ReorderProductMediaUseCase reorderMedia) {
        this.startUpload = startUpload;
        this.addMedia = addMedia;
        this.listMedia = listMedia;
        this.removeMedia = removeMedia;
        this.reorderMedia = reorderMedia;
    }

    private static EstablishmentId establishment(Jwt jwt) {
        return AuthenticatedUser.from(jwt).establishmentId();
    }

    @PostMapping("/media/uploads")
    @Operation(operationId = "startMediaUpload", summary = "Passo 1: pede a URL assinada para enviar uma foto")
    @ApiResponse(responseCode = "200", description = "Como enviar o arquivo")
    @ApiResponse(responseCode = "400", description = "Tipo ou tamanho não aceito",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Não é um produto ativo deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "O produto já tem 8 fotos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    MediaUploadResponse startUpload(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId,
                                    @Valid @RequestBody MediaUploadRequest request) {
        PhotoFile file = new PhotoFile(request.contentType(), request.sizeBytes());
        return MediaUploadResponse.from(startUpload.start(establishment(jwt), new ProductId(productId), file));
    }

    @PostMapping("/media")
    @Operation(operationId = "addProductMedia",
            summary = "Passo 2: o arquivo já foi enviado; confere a foto, gera os 3 tamanhos e a põe na galeria")
    @ApiResponse(responseCode = "201", description = "Foto adicionada (no fim da galeria)")
    @ApiResponse(responseCode = "400", description = "O arquivo não é uma foto JPG ou PNG que dá para usar",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "O arquivo não chegou, ou o produto já tem 8 fotos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<ProductMediaResponse> add(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId,
                                             @Valid @RequestBody AddMediaRequest request) {
        ProductMediaSummary media = addMedia.add(establishment(jwt), new ProductId(productId), request.uploadId());
        return ResponseEntity.created(URI.create("/api/v1/products/" + productId + "/media/" + media.id().value()))
                .body(ProductMediaResponse.from(media));
    }

    @GetMapping("/media")
    @Operation(operationId = "listProductMedia", summary = "As fotos do produto, na ordem (a primeira é a capa)")
    @ApiResponse(responseCode = "200", description = "Todas as fotos, numa página só (são no máximo 8)")
    @ApiResponse(responseCode = "404", description = "Não é um produto ativo deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    PageResponse<ProductMediaResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId) {
        return PageResponse.from(listMedia.list(establishment(jwt), new ProductId(productId)),
                ProductMediaResponse::from);
    }

    @DeleteMapping("/media/{mediaId}")
    @Operation(operationId = "removeProductMedia", summary = "Tira a foto da galeria")
    @ApiResponse(responseCode = "204", description = "Removida")
    @ApiResponse(responseCode = "404", description = "Não é uma foto deste produto",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId,
                                @PathVariable UUID mediaId) {
        removeMedia.remove(establishment(jwt), new ProductId(productId), new ProductMediaId(mediaId));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/media-order")
    @Operation(operationId = "reorderProductMedia", summary = "Nova ordem das fotos (todas, a primeira vira a capa)")
    @ApiResponse(responseCode = "204", description = "Reordenadas")
    @ApiResponse(responseCode = "404", description = "Não é um produto ativo deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A galeria mudou desde que a tela carregou",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> reorder(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId,
                                 @Valid @RequestBody ReorderRequest request) {
        reorderMedia.reorder(establishment(jwt), new ProductId(productId), request.as(ProductMediaId::new));
        return ResponseEntity.noContent().build();
    }
}
