package br.com.cardapio.adapter.in.web.floor;

import br.com.cardapio.application.port.in.floor.PublicTable;
import br.com.cardapio.domain.brand.model.BrandColor;
import br.com.cardapio.domain.brand.model.LogoVariant;
import br.com.cardapio.domain.brand.model.Theme;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * A mesa do QR Code e a marca do restaurante, para o topo da página da mesa. Os dois primeiros
 * campos são os do Guia 8: a página antiga continua funcionando.
 */
@Schema(name = "PublicTable", description = "A mesa do QR Code, para a página pública")
record PublicTableResponse(
        @Schema(requiredMode = REQUIRED, example = "Sakura Sushi") String establishmentName,
        @Schema(requiredMode = REQUIRED, example = "04") String tableLabel,
        @Schema(requiredMode = REQUIRED, example = "Salão principal") String sectorName,
        @Schema(requiredMode = REQUIRED) PublicBrand brand,
        @Schema(requiredMode = REQUIRED, description = """
                false: o atendimento pelo celular está pausado (assinatura suspensa); a página mostra o aviso \
                no lugar dos botões de chamar e pedir""")
        boolean serving
) {
    @Schema(name = "PublicBrand", description = "A aparência da página: só os tons da cor escolhida")
    record PublicBrand(
            @Schema(requiredMode = REQUIRED) Theme theme,
            @Schema(requiredMode = REQUIRED, description = "Fundo dos botões, com texto branco", example = "#CC3F1C") String primary,
            @Schema(requiredMode = REQUIRED, description = "Texto de destaque no tema em uso", example = "#FF8466") String accent,
            @Schema(description = "null quando o restaurante usa as iniciais") PublicLogo logo,
            @Schema(description = "O @, sem o \"@\"; null quando não informado") String instagram,
            @Schema(description = "null quando não informado") String googleReviewUrl
    ) {
    }

    @Schema(name = "PublicLogo")
    record PublicLogo(
            @Schema(requiredMode = REQUIRED, description = "128 px") String small,
            @Schema(requiredMode = REQUIRED, description = "512 px") String large
    ) {
    }

    static PublicTableResponse from(PublicTable t) {
        BrandColor color = t.color();
        // O tom de texto já vem escolhido pelo tema: a página não precisa conhecer a paleta.
        String accent = t.theme() == Theme.DARK ? color.textOnDark() : color.textOnLight();
        PublicLogo logo = t.logoUrls().isEmpty() ? null
                : new PublicLogo(t.logoUrls().get(LogoVariant.SMALL), t.logoUrls().get(LogoVariant.LARGE));
        return new PublicTableResponse(t.establishmentName(), t.tableLabel(), t.sectorName(),
                new PublicBrand(t.theme(), color.primary(), accent, logo, t.instagram(), t.reviewLink()), t.serving());
    }
}
