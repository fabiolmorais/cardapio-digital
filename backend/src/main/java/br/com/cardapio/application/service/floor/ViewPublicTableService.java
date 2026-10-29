package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.PublicTable;
import br.com.cardapio.application.port.in.floor.ViewPublicTableUseCase;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.TableToken;
import org.springframework.transaction.annotation.Transactional;

public class ViewPublicTableService implements ViewPublicTableUseCase {

    private final DiningTablePort tables;

    public ViewPublicTableService(DiningTablePort tables) {
        this.tables = tables;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicTable view(String token) {
        TableToken tableToken;
        try {
            tableToken = new TableToken(token);
        } catch (IllegalArgumentException | NullPointerException e) {
            // Token fora do formato: nem vale ir ao banco. Mesma resposta de um token que não existe.
            throw new TableNotFoundException();
        }
        return tables.findActiveByToken(tableToken)
                .map(found -> new PublicTable(found.establishmentName(), found.table().label()))
                .orElseThrow(TableNotFoundException::new);
    }
}
