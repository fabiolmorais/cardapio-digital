package br.com.cardapio.adapter.in.web.brand;

import br.com.cardapio.application.port.in.brand.BrandSummary;
import br.com.cardapio.domain.brand.model.BrandColor;
import br.com.cardapio.domain.brand.model.LogoVariant;
import br.com.cardapio.domain.brand.model.Theme;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * A marca, e a paleta inteira para a tela montar as opções. As cores ficam no back (BrandColor):
 * o front não declara nenhum código de cor à mão, e a página da mesa (Etapa 5) usa os mesmos tons.
 */
@Schema(name = "Brand")
record BrandResponse(
        @Schema(requiredMode = REQUIRED) String name,
        @Schema(requiredMode = REQUIRED) BrandColor color,
        @Schema(requiredMode = REQUIRED) Theme theme,
        @Schema(description = "null quando não há logo (a tela mostra as iniciais)") Logo logo,
        @Schema(description = "O @, sem o \"@\"; null quando não informado", example = "sakurasushi.sp") String instagram,
        @Schema(description = "null quando não informado") String googleReviewUrl,
        @Schema(requiredMode = REQUIRED) List<PaletteColor> palette
) {
    @Schema(name = "BrandLogo", description = "A logo em PNG quadrado")
    record Logo(
            @Schema(requiredMode = REQUIRED, description = "128 px") String small,
            @Schema(requiredMode = REQUIRED, description = "512 px") String large
    ) {
    }

    @Schema(name = "PaletteColor")
    record PaletteColor(
            @Schema(requiredMode = REQUIRED) BrandColor color,
            @Schema(requiredMode = REQUIRED, example = "Tomate") String label,
            @Schema(requiredMode = REQUIRED, description = "Fundo dos botões (texto branco por cima)", example = "#CC3F1C") String primary,
            @Schema(requiredMode = REQUIRED, description = "Texto de destaque no tema escuro", example = "#FF8466") String textOnDark,
            @Schema(requiredMode = REQUIRED, description = "Texto de destaque no tema claro", example = "#B8361A") String textOnLight
    ) {
    }

    private static final List<PaletteColor> PALETTE = Arrays.stream(BrandColor.values())
            .map(c -> new PaletteColor(c, c.label(), c.primary(), c.textOnDark(), c.textOnLight()))
            .toList();

    static BrandResponse from(BrandSummary b) {
        Logo logo = b.logoUrls().isEmpty() ? null
                : new Logo(b.logoUrls().get(LogoVariant.SMALL), b.logoUrls().get(LogoVariant.LARGE));
        return new BrandResponse(b.name(), b.color(), b.theme(), logo, b.instagram(), b.reviewLink(), PALETTE);
    }
}
