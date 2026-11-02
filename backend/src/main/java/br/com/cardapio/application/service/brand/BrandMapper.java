package br.com.cardapio.application.service.brand;

import br.com.cardapio.application.port.in.brand.BrandSummary;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.domain.brand.model.Brand;
import br.com.cardapio.domain.brand.model.LogoVariant;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** A marca do domínio vira o resumo da tela, com o endereço público de cada tamanho da logo. */
final class BrandMapper {

    private BrandMapper() {
    }

    static BrandSummary toSummary(Brand brand, MediaStoragePort storage) {
        Map<LogoVariant, String> logo = brand.hasLogo()
                ? Arrays.stream(LogoVariant.values())
                        .collect(Collectors.toMap(Function.identity(), v -> storage.publicUrl(brand.logoFile(v))))
                : Map.of();
        return new BrandSummary(brand.name(), brand.color(), brand.theme(), logo,
                brand.instagram() == null ? null : brand.instagram().value(),
                brand.reviewLink() == null ? null : brand.reviewLink().value());
    }
}
