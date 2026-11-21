package br.com.cardapio.application.port.out.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.report.model.PrepTime;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/** Os tempos da cozinha (ADR 0013). */
public interface KitchenReportPort {

    /**
     * Os itens pedidos no intervalo [início, fim) que ficaram prontos, sem os cancelados (rodadas do
     * rodízio entram: a cozinha preparou), com o tempo da chegada à cozinha até pronto e o dia da
     * semana e a hora da chegada no fuso dado. Pelo setor da mesa e pela categoria de hoje do
     * produto, se pedidos.
     */
    List<PrepTime> prepTimes(EstablishmentId establishmentId, Instant start, Instant end, ZoneId zone,
                             Optional<SectorId> sector, Optional<MenuCategoryId> category);
}
