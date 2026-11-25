package br.com.cardapio.application.port.out.report;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.report.model.RatingComment;
import br.com.cardapio.domain.report.model.RatingDay;
import br.com.cardapio.domain.report.model.RatingStats;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * As avaliações do atendimento nos relatórios (ADR 0013; REL-83, 84): as de [início, fim), do
 * restaurante e, se pedido, das mesas do setor.
 */
public interface RatingReportPort {

    /** O resumo do período; o almoço vai até {@code lunchUntil}h, no fuso {@code zone}. */
    RatingStats stats(EstablishmentId establishmentId, Instant start, Instant end, ZoneId zone, int lunchUntil,
                      Optional<SectorId> sector);

    /** Por dia do restaurante (só os dias com avaliação). */
    List<RatingDay> byDay(EstablishmentId establishmentId, Instant start, Instant end, ZoneId zone,
                          Optional<SectorId> sector);

    /** As que têm comentário, da mais nova para a mais velha; só as notas 1 e 2, se pedido. */
    ResultPage<RatingComment> comments(EstablishmentId establishmentId, Instant start, Instant end,
                                       Optional<SectorId> sector, boolean onlyLow, PageQuery page);
}
