package br.com.cardapio.application.port.out.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.report.model.ItemSales;
import br.com.cardapio.domain.report.model.PrepTotal;
import br.com.cardapio.domain.report.model.SummaryStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * O resumo diário por produto (ADR 0014): ler os dias já resumidos e refazê-los. Tudo pelo
 * restaurante de quem pede.
 */
public interface ReportSummaryPort {

    /** O dia seguinte ao último resumido; vazio quando o restaurante ainda não tem resumo. */
    Optional<LocalDate> summarizedUntil(EstablishmentId establishmentId);

    /** Os vendidos de cada produto nos dias [de, até), com o nome de hoje; pelo setor, se pedido. */
    List<ItemSales> sales(EstablishmentId establishmentId, LocalDate from, LocalDate until, Optional<SectorId> sector);

    /** Os preparos de cada produto nos dias [de, até); pelo setor, se pedido. */
    List<PrepTotal> preparations(EstablishmentId establishmentId, LocalDate from, LocalDate until,
                                 Optional<SectorId> sector);

    /** Os restaurantes ativos e onde o resumo de cada um parou. */
    List<SummaryStatus> statuses();

    /** Refaz os dias [de, até) e marca o resumo como feito até {@code until}, numa transação só. */
    void rebuild(EstablishmentId establishmentId, ZoneId zone, LocalDate from, LocalDate until, Instant now);

    /** Apaga o resumo do restaurante (a próxima volta da tarefa refaz tudo). */
    void invalidate(EstablishmentId establishmentId);
}
