package br.com.cardapio.application.port.out.floor;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;

import java.util.Collection;
import java.util.Map;

public interface TableActivityPort {

    /** O movimento de cada mesa pedida, numa consulta só. Mesa sem movimento pode faltar no mapa. */
    Map<DiningTableId, TableActivity> findActivity(EstablishmentId establishmentId, Collection<DiningTableId> tableIds);
}
