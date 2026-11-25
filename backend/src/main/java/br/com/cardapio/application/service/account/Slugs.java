package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.out.account.LoadEstablishmentPort;
import br.com.cardapio.domain.account.model.Slug;

/**
 * O endereço livre para um restaurante novo: o do nome e, se já existir, com -2, -3…
 * Pública porque o cadastro de unidades (outro pacote) também usa.
 */
public final class Slugs {

    private Slugs() {
    }

    public static Slug available(LoadEstablishmentPort establishments, String establishmentName) {
        Slug base = Slug.from(establishmentName);
        Slug candidate = base;
        int suffix = 2;
        while (establishments.existsBySlug(candidate)) {
            candidate = base.withSuffix(suffix++);
        }
        return candidate;
    }
}
