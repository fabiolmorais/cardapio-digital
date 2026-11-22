package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.application.port.in.report.SalesFull;
import br.com.cardapio.domain.report.model.CancelReasonTotal;
import br.com.cardapio.domain.report.model.PerPersonTicket;
import br.com.cardapio.domain.report.model.SectorSales;
import br.com.cardapio.domain.report.model.TableSales;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** Os blocos completos de Vendas (Business). */
final class SalesFullDtos {

    private SalesFullDtos() {
    }

    @Schema(name = "ReportSector", description = "Um setor: o faturamento e as mesas ativas")
    record Sector(@Schema(requiredMode = REQUIRED) UUID sectorId, @Schema(requiredMode = REQUIRED) String name,
                  @Schema(requiredMode = REQUIRED) int tables, @Schema(requiredMode = REQUIRED) long revenueCents,
                  @Schema(requiredMode = REQUIRED, description = "Por mesa, por dia do período")
                  long perTablePerDayCents) {
        static Sector of(SectorSales s, int days) {
            return new Sector(s.sectorId().value(), s.name(), s.tables(), s.revenue().cents(),
                    s.perTablePerDay(days).cents());
        }
    }

    @Schema(name = "ReportTable", description = "Uma mesa: comandas, tíquete e faturamento")
    record Table(@Schema(requiredMode = REQUIRED) UUID tableId, @Schema(requiredMode = REQUIRED) String label,
                 @Schema(requiredMode = REQUIRED) String sector, @Schema(requiredMode = REQUIRED) int tabs,
                 @Schema(requiredMode = REQUIRED) long averageTicketCents,
                 @Schema(requiredMode = REQUIRED) long revenueCents) {
        static Table of(TableSales t) {
            return new Table(t.tableId().value(), t.label(), t.sector(), t.tabs(), t.averageTicket().cents(),
                    t.revenue().cents());
        }
    }

    @Schema(name = "ReportPerPerson", description = "O tíquete por pessoa")
    record PerPerson(@Schema(requiredMode = REQUIRED) long revenueCents, @Schema(requiredMode = REQUIRED) int people,
                     @Schema(description = "null: ninguém contado") Long averageCents) {
        static PerPerson of(PerPersonTicket t) {
            return new PerPerson(t.revenue().cents(), t.people(), t.average().map(m -> m.cents()).orElse(null));
        }
    }

    @Schema(name = "ReportCancelReason", description = "O valor cancelado por motivo")
    record Reason(@Schema(description = "null: os outros motivos e os sem motivo") String reason,
                  @Schema(requiredMode = REQUIRED) long amountCents) {
        static Reason of(CancelReasonTotal r) {
            return new Reason(r.reason(), r.amount().cents());
        }
    }

    @Schema(name = "SalesFull", description = "Os blocos do Business")
    record Full(@Schema(requiredMode = REQUIRED) List<Sector> bySector,
                @Schema(requiredMode = REQUIRED) List<Table> topTables,
                @Schema(requiredMode = REQUIRED, description = "Comandas com rodízio") PerPerson rodizio,
                @Schema(requiredMode = REQUIRED) PerPerson comparedRodizio,
                @Schema(requiredMode = REQUIRED, description = "Fora do rodízio, com as pessoas informadas")
                PerPerson people,
                @Schema(requiredMode = REQUIRED) PerPerson comparedPeople,
                @Schema(requiredMode = REQUIRED, description = "Comandas que pagaram antes de fechar") int advanceTabs,
                @Schema(requiredMode = REQUIRED) long advanceCents,
                @Schema(requiredMode = REQUIRED, description = "Itens cancelados nas comandas fechadas")
                long canceledCents,
                @Schema(requiredMode = REQUIRED) long comparedCanceledCents,
                @Schema(requiredMode = REQUIRED, description = "Os motivos que mais cancelaram e, por último, os outros")
                List<Reason> reasons) {
        static Full of(SalesFull f) {
            return new Full(f.bySector().stream().map(s -> Sector.of(s, f.days())).toList(),
                    f.topTables().stream().map(Table::of).toList(), PerPerson.of(f.rodizio()),
                    PerPerson.of(f.comparedRodizio()), PerPerson.of(f.people()), PerPerson.of(f.comparedPeople()),
                    f.advance().tabs(), f.advance().amount().cents(), f.canceled().cents(),
                    f.comparedCanceled().cents(), f.reasons().stream().map(Reason::of).toList());
        }
    }
}
