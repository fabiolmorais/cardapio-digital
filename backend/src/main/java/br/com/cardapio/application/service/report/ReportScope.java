package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.ReportQuery;
import br.com.cardapio.application.port.out.account.EstablishmentTimeZonePort;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.domain.floor.exception.SectorNotFoundException;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.report.model.ReportAccess;
import br.com.cardapio.domain.report.model.ReportPeriod;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

/**
 * O que todo relatório resolve antes de somar: o plano, o período no fuso do restaurante, a
 * comparação e o setor.
 *
 * @param comparisonEnd o fim da comparação. Quando o período termina hoje, a comparação também para
 *                      na mesma hora (sexta às 21h compara com a sexta passada até as 21h), para o
 *                      "▼" não acusar o que o dia ainda não vendeu.
 */
record ReportScope(ReportAccess access, ReportPeriod period, ReportPeriod comparison, Instant now,
                   Instant comparisonEnd, Optional<SectorId> sector) {

    static ReportScope resolve(ReportQuery query, SubscriptionPort subscriptions, EstablishmentTimeZonePort timeZones,
                               SectorPort sectors, Clock clock) {
        ReportAccess access = ReportAccesses.of(subscriptions, query.establishmentId());
        access.requireReports();
        ZoneId zone = timeZones.timeZoneOf(query.establishmentId());
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, zone);
        ReportPeriod period = new ReportPeriod(query.from(), query.to(), zone);
        access.checkPeriod(period, today);
        // Setor de outro restaurante: 404, sem revelar que existe.
        query.sectorFilter().ifPresent(id -> sectors.find(query.establishmentId(), id)
                .orElseThrow(SectorNotFoundException::new));
        ReportPeriod comparison = period.comparison();
        Instant comparisonEnd = period.to().equals(today)
                ? comparison.end().minus(Duration.between(now, period.end()))
                : comparison.end();
        return new ReportScope(access, period, comparison, now, comparisonEnd, query.sectorFilter());
    }
}
