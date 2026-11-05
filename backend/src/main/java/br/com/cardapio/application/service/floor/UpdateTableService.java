package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.TableSummary;
import br.com.cardapio.application.port.in.floor.UpdateTableCommand;
import br.com.cardapio.application.port.in.floor.UpdateTableUseCase;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.service.plan.PlanAllowances;
import br.com.cardapio.domain.floor.exception.SectorNotFoundException;
import br.com.cardapio.domain.floor.exception.TableLabelTakenException;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.Sector;
import br.com.cardapio.domain.plan.model.PlanResource;
import org.springframework.transaction.annotation.Transactional;

public class UpdateTableService implements UpdateTableUseCase {

    private final SectorPort sectors;
    private final DiningTablePort tables;
    private final SubscriptionPort subscriptions;

    public UpdateTableService(SectorPort sectors, DiningTablePort tables, SubscriptionPort subscriptions) {
        this.sectors = sectors;
        this.tables = tables;
        this.subscriptions = subscriptions;
    }

    @Override
    @Transactional
    public TableSummary update(UpdateTableCommand command) {
        DiningTable table = tables.find(command.establishmentId(), command.tableId())
                .orElseThrow(TableNotFoundException::new);

        if (command.label() != null) {
            table = table.relabel(command.label());
            if (tables.labelExists(command.establishmentId(), table.label(), table.id())) {
                throw new TableLabelTakenException(table.label());
            }
        }
        if (command.sectorId() != null) {
            table = table.moveTo(command.sectorId());
        }
        if (command.active() != null) {
            // Reativar ocupa uma vaga do plano, como cadastrar uma mesa nova.
            if (command.active() && !table.active()) {
                PlanAllowances.requireRoomForOneMore(subscriptions, command.establishmentId(), PlanResource.TABLES);
            }
            table = command.active() ? table.activate() : table.deactivate();
        }
        // Mesa ativa precisa estar num setor ativo deste restaurante (vale para mover e para reativar).
        if (table.active()) {
            sectors.find(command.establishmentId(), table.sectorId())
                    .filter(Sector::active)
                    .orElseThrow(SectorNotFoundException::new);
        }

        tables.save(table);
        return TableSummary.from(table);
    }
}
