package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.CreateTableCommand;
import br.com.cardapio.application.port.in.floor.CreateTableUseCase;
import br.com.cardapio.application.port.in.floor.TableSummary;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.security.RandomTokenGenerator;
import br.com.cardapio.application.service.plan.PlanAllowances;
import br.com.cardapio.domain.floor.exception.SectorNotFoundException;
import br.com.cardapio.domain.floor.exception.TableLabelTakenException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.Sector;
import br.com.cardapio.domain.floor.model.TableToken;
import br.com.cardapio.domain.plan.model.PlanResource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class CreateTableService implements CreateTableUseCase {

    private final SectorPort sectors;
    private final DiningTablePort tables;
    private final RandomTokenGenerator tokens;
    private final SubscriptionPort subscriptions;
    private final Clock clock;

    public CreateTableService(SectorPort sectors, DiningTablePort tables, RandomTokenGenerator tokens,
                              SubscriptionPort subscriptions, Clock clock) {
        this.sectors = sectors;
        this.tables = tables;
        this.tokens = tokens;
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TableSummary create(CreateTableCommand command) {
        // O setor precisa ser deste restaurante e estar ativo: um id de outro restaurante dá 404.
        sectors.find(command.establishmentId(), command.sectorId())
                .filter(Sector::active)
                .orElseThrow(SectorNotFoundException::new);

        // O balcão (ADR 0015) também tem um token (a coluna é obrigatória), mas ele nunca vira QR Code.
        TableToken token = new TableToken(tokens.generate());
        DiningTable table = command.kind().isCounter()
                ? DiningTable.createCounter(command.establishmentId(), command.sectorId(), command.label(), token,
                        clock.instant())
                : DiningTable.create(command.establishmentId(), command.sectorId(), command.label(), token,
                        clock.instant());
        if (tables.labelExists(command.establishmentId(), table.label(), null)) {
            throw new TableLabelTakenException(table.label());
        }
        // O balcão não conta no limite de mesas do plano.
        if (!table.isCounter()) {
            PlanAllowances.requireRoomForOneMore(subscriptions, command.establishmentId(), PlanResource.TABLES);
        }
        tables.save(table);
        return TableSummary.from(table);
    }
}
