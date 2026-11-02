package br.com.cardapio.adapter.in.web.brand;

import br.com.cardapio.application.port.in.brand.UpdateBrandCommand;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.brand.model.BrandColor;
import br.com.cardapio.domain.brand.model.Theme;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** O formulário "Marca e aparência" (a logo tem o próprio envio). Campo vazio apaga. */
@Schema(name = "BrandRequest")
record BrandRequest(
        @Schema(requiredMode = REQUIRED, example = "Sakura Sushi") @NotBlank @Size(max = 120) String name,
        @Schema(requiredMode = REQUIRED) @NotNull BrandColor color,
        @Schema(requiredMode = REQUIRED) @NotNull Theme theme,
        @Schema(description = "O @, o nome ou o link do perfil", example = "@sakurasushi.sp") @Size(max = 100) String instagram,
        @Schema(description = "Link de avaliação do Google", example = "https://g.page/r/CbX0exemplo/review")
        @Size(max = 300) String googleReviewUrl
) {
    UpdateBrandCommand toCommand(EstablishmentId establishmentId) {
        return new UpdateBrandCommand(establishmentId, name, color, theme, instagram, googleReviewUrl);
    }
}
