package br.com.cardapio.application.port.out.account;

import br.com.cardapio.domain.account.model.Slug;

public interface LoadEstablishmentPort {

    /** O slug é único no sistema inteiro, por isso a consulta é global. */
    boolean existsBySlug(Slug slug);
}
