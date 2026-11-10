package br.com.cardapio.application.service.rodizio;

import br.com.cardapio.application.port.in.rodizio.ListRodiziosUseCase;
import br.com.cardapio.application.port.out.rodizio.RodizioPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.rodizio.exception.RodizioNotFoundException;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioId;
import org.springframework.transaction.annotation.Transactional;

public class ListRodiziosService implements ListRodiziosUseCase {

    private final RodizioPort rodizios;

    public ListRodiziosService(RodizioPort rodizios) {
        this.rodizios = rodizios;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<Rodizio> list(EstablishmentId establishmentId, PageQuery page) {
        return rodizios.findAll(establishmentId, page);
    }

    /** O pausado também abre: o dono pode editar e reativar. */
    @Override
    @Transactional(readOnly = true)
    public Rodizio get(EstablishmentId establishmentId, RodizioId id) {
        return rodizios.find(establishmentId, id).orElseThrow(RodizioNotFoundException::new);
    }
}
