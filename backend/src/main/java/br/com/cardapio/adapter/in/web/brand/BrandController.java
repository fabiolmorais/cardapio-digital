package br.com.cardapio.adapter.in.web.brand;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.brand.GetBrandUseCase;
import br.com.cardapio.application.port.in.brand.RemoveLogoUseCase;
import br.com.cardapio.application.port.in.brand.SetLogoUseCase;
import br.com.cardapio.application.port.in.brand.StartLogoUploadUseCase;
import br.com.cardapio.application.port.in.brand.UpdateBrandUseCase;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.brand.model.LogoFile;
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
import org.springframework.web.bind.annotation.*;

/**
 * A marca do restaurante de quem está logado. Não há id na URL: "o restaurante" é sempre o do
 * token (como /api/v1/me). Por isso é um recurso no singular.
 */
@RestController
@RequestMapping("/api/v1/establishment")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
@Tag(name = "Marca", description = "Logo, cor e aparência da página da mesa")
@SecurityRequirement(name = "bearer")
class BrandController {

    private final GetBrandUseCase getBrand;
    private final UpdateBrandUseCase updateBrand;
    private final StartLogoUploadUseCase startLogoUpload;
    private final SetLogoUseCase setLogo;
    private final RemoveLogoUseCase removeLogo;

    BrandController(GetBrandUseCase getBrand, UpdateBrandUseCase updateBrand, StartLogoUploadUseCase startLogoUpload,
                    SetLogoUseCase setLogo, RemoveLogoUseCase removeLogo) {
        this.getBrand = getBrand;
        this.updateBrand = updateBrand;
        this.startLogoUpload = startLogoUpload;
        this.setLogo = setLogo;
        this.removeLogo = removeLogo;
    }

    private static EstablishmentId establishment(Jwt jwt) {
        return AuthenticatedUser.from(jwt).establishmentId();
    }

    @GetMapping("/brand")
    @Operation(operationId = "getBrand", summary = "A marca do restaurante e a paleta de cores")
    @ApiResponse(responseCode = "200", description = "A marca")
    BrandResponse get(@AuthenticationPrincipal Jwt jwt) {
        return BrandResponse.from(getBrand.get(establishment(jwt)));
    }

    @PutMapping("/brand")
    @Operation(operationId = "updateBrand", summary = "Salva nome, cor, tema, Instagram e link de avaliação")
    @ApiResponse(responseCode = "200", description = "Salva")
    @ApiResponse(responseCode = "400", description = "Instagram ou link de avaliação inválido",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    BrandResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BrandRequest request) {
        return BrandResponse.from(updateBrand.update(request.toCommand(establishment(jwt))));
    }

    @PostMapping("/logo/uploads")
    @Operation(operationId = "startLogoUpload", summary = "Passo 1: pede a URL assinada para enviar a logo")
    @ApiResponse(responseCode = "200", description = "Como enviar o arquivo")
    @ApiResponse(responseCode = "400", description = "Tipo ou tamanho não aceito",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    LogoUploadResponse startUpload(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LogoUploadRequest request) {
        LogoFile file = new LogoFile(request.contentType(), request.sizeBytes());
        return LogoUploadResponse.from(startLogoUpload.start(establishment(jwt), file));
    }

    @PostMapping("/logo")
    @Operation(operationId = "setLogo", summary = "Passo 2: o arquivo já foi enviado; confere e troca a logo")
    @ApiResponse(responseCode = "200", description = "Logo trocada")
    @ApiResponse(responseCode = "400", description = "O arquivo não é uma imagem PNG ou JPG que dá para usar",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "O arquivo não chegou",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    BrandResponse set(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SetLogoRequest request) {
        return BrandResponse.from(setLogo.set(establishment(jwt), request.uploadId()));
    }

    @DeleteMapping("/logo")
    @Operation(operationId = "removeLogo", summary = "Tira a logo (a página volta a mostrar as iniciais)")
    @ApiResponse(responseCode = "200", description = "Logo removida")
    BrandResponse remove(@AuthenticationPrincipal Jwt jwt) {
        return BrandResponse.from(removeLogo.remove(establishment(jwt)));
    }
}
