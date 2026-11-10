package br.com.cardapio.application.port.out.rodizio;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioId;

import java.util.Optional;

public interface RodizioPort {

    /** Grava o rodízio inteiro: as faixas, os preços especiais e os produtos incluídos vão junto. */
    void save(Rodizio rodizio);

    Optional<Rodizio> find(EstablishmentId establishmentId, RodizioId id);

    /** Os rodízios ativos, por nome. */
    ResultPage<Rodizio> findActive(EstablishmentId establishmentId, PageQuery page);

    /** O cadastro inteiro: os ativos primeiro, depois os pausados, por nome. */
    ResultPage<Rodizio> findAll(EstablishmentId establishmentId, PageQuery page);

    long countActive(EstablishmentId establishmentId);

    /** Já existe um rodízio ativo com este nome (sem contar o exceptId, que é o que está sendo editado)? */
    boolean activeNameExists(EstablishmentId establishmentId, String name, RodizioId exceptId);
}
