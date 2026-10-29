package br.com.cardapio.application.port.out.account;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Slug;

import java.util.Optional;

public interface LoadEstablishmentPort {

    /** O slug é único no sistema inteiro, por isso a consulta é global. */
    boolean existsBySlug(Slug slug);

    /** Nome do restaurante (vai no topo do display impresso). */
    Optional<String> findName(EstablishmentId id);
}
