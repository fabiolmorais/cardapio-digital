package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.ListSectorsUseCase;
import br.com.cardapio.application.port.in.floor.SectorSummary;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import org.springframework.transaction.annotation.Transactional;

public class ListSectorsService implements ListSectorsUseCase {

    private final SectorPort sectors;

    public ListSectorsService(SectorPort sectors) {
        this.sectors = sectors;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<SectorSummary> list(EstablishmentId establishmentId, PageQuery page) {
        return sectors.findActive(establishmentId, page).map(SectorSummary::from);
    }
}
