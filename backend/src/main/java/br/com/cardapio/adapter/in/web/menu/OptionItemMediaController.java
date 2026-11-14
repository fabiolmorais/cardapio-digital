package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.menu.AddProductMediaUseCase;
import br.com.cardapio.application.port.in.menu.ListProductMediaUseCase;
import br.com.cardapio.application.port.in.menu.ProductMediaSummary;
import br.com.cardapio.application.port.in.menu.RemoveProductMediaUseCase;
import br.com.cardapio.application.port.in.menu.ReorderProductMediaUseCase;
import br.com.cardapio.application.port.in.menu.StartMediaUploadUseCase;
import br.com.cardapio.application.port.in.menu.StartVideoUploadUseCase;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.PhotoFile;
import br.com.cardapio.domain.menu.model.OptionItemId;
import br.com.cardapio.domain.menu.model.ProductMediaId;
import br.com.cardapio.domain.menu.model.VideoFile;
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
 * Fotos e vídeos de uma opção (o sabor da pizza; complemento da ADR 0010), com os mesmos passos e
 * limites das do produto (ver {@link ProductMediaController}). O arquivo não passa pelo back. Foto: o navegador pede uma URL
 * assinada (uploads), envia direto para o armazenamento e depois avisa (POST media), quando o back
 * confere e gera os tamanhos. Vídeo: o back cria a mídia e devolve a autorização do envio TUS
 * (videos); o navegador envia direto ao serviço de vídeo, que converte e avisa (webhook).
 */
@RestController
@RequestMapping("/api/v1/option-items/{optionId}")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
@Tag(name = "Cardápio", description = "Categorias e produtos")
@SecurityRequirement(name = "bearer")
class OptionItemMediaController {

    private final StartMediaUploadUseCase startUpload;
    private final AddProductMediaUseCase addMedia;
    private final ListProductMediaUseCase listMedia;
    private final RemoveProductMediaUseCase removeMedia;
    private final ReorderProductMediaUseCase reorderMedia;
    private final StartVideoUploadUseCase startVideo;

    OptionItemMediaController(StartMediaUploadUseCase startUpload, AddProductMediaUseCase addMedia,
                           ListProductMediaUseCase listMedia, RemoveProductMediaUseCase removeMedia,
                           ReorderProductMediaUseCase reorderMedia, StartVideoUploadUseCase startVideo) {
        this.startUpload = startUpload;
        this.addMedia = addMedia;
        this.listMedia = listMedia;
        this.removeMedia = removeMedia;
        this.reorderMedia = reorderMedia;
        this.startVideo = startVideo;
    }

    private static EstablishmentId establishment(Jwt jwt) {
        return AuthenticatedUser.from(jwt).establishmentId();
    }

    @PostMapping("/media/uploads")
    @Operation(operationId = "startOptionMediaUpload", summary = "Passo 1: pede a URL assinada para enviar uma foto")
    @ApiResponse(responseCode = "200", description = "Como enviar o arquivo")
    @ApiResponse(responseCode = "400", description = "Tipo ou tamanho não aceito",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Não é uma opção ativa deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "A opção já tem 8 fotos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    MediaUploadResponse startUpload(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID optionId,
                                    @Valid @RequestBody MediaUploadRequest request) {
        PhotoFile file = new PhotoFile(request.contentType(), request.sizeBytes());
        return MediaUploadResponse.from(startUpload.start(establishment(jwt), new OptionItemId(optionId), file));
    }

    @PostMapping("/media")
    @Operation(operationId = "addOptionMedia",
            summary = "Passo 2: o arquivo já foi enviado; confere a foto, gera os 3 tamanhos e a põe na galeria")
    @ApiResponse(responseCode = "201", description = "Foto adicionada (no fim da galeria)")
    @ApiResponse(responseCode = "400", description = "O arquivo não é uma foto JPG ou PNG que dá para usar",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "O arquivo não chegou, ou o produto já tem 8 fotos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<ProductMediaResponse> add(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID optionId,
                                             @Valid @RequestBody AddMediaRequest request) {
        ProductMediaSummary media = addMedia.add(establishment(jwt), new OptionItemId(optionId), request.uploadId());
        return ResponseEntity.created(URI.create("/api/v1/option-items/" + optionId + "/media/" + media.id().value()))
                .body(ProductMediaResponse.from(media));
    }

    @PostMapping("/videos")
    @Operation(operationId = "startOptionVideoUpload",
            summary = "Cria o vídeo (em conversão, no fim da galeria) e devolve como enviar o arquivo por TUS")
    @ApiResponse(responseCode = "201", description = "A mídia criada e a autorização do envio")
    @ApiResponse(responseCode = "400", description = "Tipo ou tamanho não aceito (MP4, MOV ou WebM, até 200 MB)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Não é uma opção ativa deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A opção já tem 3 vídeos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "A opção já tem 8 fotos e vídeos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<VideoUploadResponse> startVideo(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID optionId,
                                                   @Valid @RequestBody VideoUploadRequest request) {
        VideoFile file = new VideoFile(request.contentType(), request.sizeBytes());
        var upload = startVideo.start(establishment(jwt), new OptionItemId(optionId), file);
        return ResponseEntity.created(URI.create("/api/v1/option-items/" + optionId + "/media/"
                + upload.mediaId().value())).body(VideoUploadResponse.from(upload));
    }

    @GetMapping("/media")
    @Operation(operationId = "listOptionMedia",
            summary = "As fotos e os vídeos da opção, na ordem (a primeira foto é a capa)")
    @ApiResponse(responseCode = "200", description = "Toda a galeria, numa página só (são no máximo 8)")
    @ApiResponse(responseCode = "404", description = "Não é uma opção ativa deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    PageResponse<ProductMediaResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID optionId) {
        return PageResponse.from(listMedia.list(establishment(jwt), new OptionItemId(optionId)),
                ProductMediaResponse::from);
    }

    @DeleteMapping("/media/{mediaId}")
    @Operation(operationId = "removeOptionMedia", summary = "Tira a foto da galeria")
    @ApiResponse(responseCode = "204", description = "Removida")
    @ApiResponse(responseCode = "404", description = "Não é uma foto desta opção",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID optionId,
                                @PathVariable UUID mediaId) {
        removeMedia.remove(establishment(jwt), new OptionItemId(optionId), new ProductMediaId(mediaId));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/media-order")
    @Operation(operationId = "reorderOptionMedia", summary = "Nova ordem das fotos (todas, a primeira vira a capa)")
    @ApiResponse(responseCode = "204", description = "Reordenadas")
    @ApiResponse(responseCode = "404", description = "Não é uma opção ativa deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A galeria mudou desde que a tela carregou",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> reorder(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID optionId,
                                 @Valid @RequestBody ReorderRequest request) {
        reorderMedia.reorder(establishment(jwt), new OptionItemId(optionId), request.as(ProductMediaId::new));
        return ResponseEntity.noContent().build();
    }
}
