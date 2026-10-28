package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.CreateTableCommand;
import br.com.cardapio.application.port.in.floor.CreateTableUseCase;
import br.com.cardapio.application.port.in.floor.TableSummary;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.security.RandomTokenGenerator;
import br.com.cardapio.domain.floor.exception.SectorNotFoundException;
import br.com.cardapio.domain.floor.exception.TableLabelTakenException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.Sector;
import br.com.cardapio.domain.floor.model.TableToken;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class CreateTableService implements CreateTableUseCase {

    private final SectorPort sectors;
    private final DiningTablePort tables;
    private final RandomTokenGenerator tokens;
    private final Clock clock;

    public CreateTableService(SectorPort sectors, DiningTablePort tables, RandomTokenGenerator tokens, Clock clock) {
        this.sectors = sectors;
        this.tables = tables;
        this.tokens = tokens;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TableSummary create(CreateTableCommand command) {
        // O setor precisa ser deste restaurante e estar ativo: um id de outro restaurante dá 404.
        sectors.find(command.establishmentId(), command.sectorId())
                .filter(Sector::active)
                .orElseThrow(SectorNotFoundException::new);

        DiningTable table = DiningTable.create(command.establishmentId(), command.sectorId(), command.label(),
                new TableToken(tokens.generate()), clock.instant());
        if (tables.labelExists(command.establishmentId(), table.label(), null)) {
            throw new TableLabelTakenException(table.label());
        }
        tables.save(table);
        return TableSummary.from(table);
    }
}
