package br.com.cardapio.application.port.in.floor;

import br.com.cardapio.domain.brand.model.BrandColor;
import br.com.cardapio.domain.brand.model.LogoVariant;
import br.com.cardapio.domain.brand.model.Theme;

import java.util.Map;

/**
 * O que a página da mesa mostra no topo: o restaurante, com a marca dele, e a mesa.
 *
 * @param logoUrls    vazio quando o restaurante usa as iniciais
 * @param instagram   null quando não informado
 * @param reviewLink  null quando não informado
 */
/**
 * @param serving false quando a assinatura passou da tolerância: a página mostra o aviso no lugar
 *                dos botões de chamar e pedir
 */
public record PublicTable(String establishmentName, String tableLabel, String sectorName, BrandColor color,
                          Theme theme, Map<LogoVariant, String> logoUrls, String instagram, String reviewLink,
                          boolean serving) {

    public PublicTable {
        logoUrls = Map.copyOf(logoUrls);
    }
}
