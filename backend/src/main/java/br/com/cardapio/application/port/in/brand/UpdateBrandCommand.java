package br.com.cardapio.application.port.in.brand;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.brand.model.BrandColor;
import br.com.cardapio.domain.brand.model.Theme;

import java.util.Objects;

/**
 * O formulário "Marca e aparência" (menos a logo).
 *
 * @param instagram  como o dono digitou: "@sakura", "sakura" ou o link; vazio apaga
 * @param reviewLink vazio apaga
 */
public record UpdateBrandCommand(EstablishmentId establishmentId, String name, BrandColor color, Theme theme,
                                 String instagram, String reviewLink) {

    public UpdateBrandCommand {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(color);
        Objects.requireNonNull(theme);
    }
}
