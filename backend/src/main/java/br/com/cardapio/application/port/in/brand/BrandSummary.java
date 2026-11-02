package br.com.cardapio.application.port.in.brand;

import br.com.cardapio.domain.brand.model.BrandColor;
import br.com.cardapio.domain.brand.model.LogoVariant;
import br.com.cardapio.domain.brand.model.Theme;

import java.util.Map;

/**
 * A marca para a tela.
 *
 * @param logoUrls    vazio quando não há logo (a tela mostra as iniciais)
 * @param instagram   null quando não informado
 * @param reviewLink  null quando não informado
 */
public record BrandSummary(String name, BrandColor color, Theme theme, Map<LogoVariant, String> logoUrls,
                           String instagram, String reviewLink) {

    public BrandSummary {
        logoUrls = Map.copyOf(logoUrls);
    }
}
