package br.com.cardapio.application.service.rating;

import br.com.cardapio.application.port.in.rating.ManageRatingsUseCase;
import br.com.cardapio.application.port.out.account.EstablishmentTimeZonePort;
import br.com.cardapio.application.port.out.rating.RatingPort;
import br.com.cardapio.domain.rating.model.RatingSummary;
import br.com.cardapio.domain.rating.model.RatingView;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.rating.exception.RatingNotFoundException;
import br.com.cardapio.domain.rating.model.RatingId;
import br.com.cardapio.domain.rating.model.ServiceRating;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** As avaliações no painel (RF40): o dia do restaurante, no fuso dele. */
public class ManageRatingsService implements ManageRatingsUseCase {

    /** A média do garçom olha os últimos 30 dias: um dia só oscila demais. */
    static final Duration MY_PERIOD = Duration.ofDays(30);

    private final RatingPort ratings;
    private final EstablishmentTimeZonePort timeZones;
    private final Clock clock;

    public ManageRatingsService(RatingPort ratings, EstablishmentTimeZonePort timeZones, Clock clock) {
        this.ratings = ratings;
        this.timeZones = timeZones;
        this.clock = clock;
    }

    private record Day(Instant start, Instant end) {
    }

    private Day today(EstablishmentId id, Instant now) {
        ZoneId zone = timeZones.timeZoneOf(id);
        LocalDate day = now.atZone(zone).toLocalDate();
        return new Day(day.atStartOfDay(zone).toInstant(), day.plusDays(1).atStartOfDay(zone).toInstant());
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummary today(EstablishmentId establishmentId) {
        Day day = today(establishmentId, clock.instant());
        return ratings.summary(establishmentId, day.start(), day.end());
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<RatingView> todayList(EstablishmentId establishmentId, boolean onlyLow, PageQuery page) {
        Day day = today(establishmentId, clock.instant());
        return ratings.findBetween(establishmentId, day.start(), day.end(), onlyLow, page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingView> openAlerts(EstablishmentId establishmentId) {
        Day day = today(establishmentId, clock.instant());
        return ratings.openAlerts(establishmentId, day.start(), day.end());
    }

    @Override
    @Transactional
    public void resolve(EstablishmentId establishmentId, RatingId id, UserId by) {
        ServiceRating rating = ratings.find(establishmentId, id).orElseThrow(RatingNotFoundException::new);
        ServiceRating resolved = rating.resolve(by, clock.instant());
        if (!resolved.equals(rating)) {
            ratings.update(resolved);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummary mine(EstablishmentId establishmentId, UserId userId) {
        if (!ratings.waiterSeesAverage(establishmentId)) {
            return null;
        }
        Instant now = clock.instant();
        return ratings.waiterSummary(establishmentId, userId, now.minus(MY_PERIOD), now);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean waiterSeesAverage(EstablishmentId establishmentId) {
        return ratings.waiterSeesAverage(establishmentId);
    }

    @Override
    @Transactional
    public void setWaiterSeesAverage(EstablishmentId establishmentId, boolean visible) {
        ratings.saveWaiterSeesAverage(establishmentId, visible, clock.instant());
    }
}
