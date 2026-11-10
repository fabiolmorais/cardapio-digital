package br.com.cardapio.adapter.out.persistence.floor;

import br.com.cardapio.application.port.out.floor.TableActivity;
import br.com.cardapio.application.port.out.floor.TableActivityPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * O movimento das mesas, da comanda, do chamado de conta e do rodízio, numa consulta nativa só.
 * O filtro do Hibernate não vale em SQL nativo: o restaurante vai na própria consulta.
 */
@Component
class TableActivityPersistenceAdapter implements TableActivityPort {

    private final DiningTableJpaRepository tables;

    TableActivityPersistenceAdapter(DiningTableJpaRepository tables) {
        this.tables = tables;
    }

    @Override
    public Map<DiningTableId, TableActivity> findActivity(EstablishmentId establishmentId,
                                                          Collection<DiningTableId> tableIds) {
        if (tableIds.isEmpty()) {
            return Map.of();
        }
        return tables.findActivity(establishmentId.value(), tableIds.stream().map(DiningTableId::value).toList())
                .stream().collect(Collectors.toMap(row -> new DiningTableId(row.getTableId()),
                        row -> new TableActivity(row.getTabOpen(), row.getBillRequested(), row.getRodizio())));
    }
}
