package br.com.cardapio.application.port.in.rating;

import br.com.cardapio.domain.rating.model.RatingSummary;
import br.com.cardapio.domain.rating.model.RatingView;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.rating.model.RatingId;

import java.util.List;

/** As avaliações no painel (RF40): o dia, os alertas de nota baixa e a média do garçom. */
public interface ManageRatingsUseCase {

    /** O resumo de hoje (no fuso do restaurante). */
    RatingSummary today(EstablishmentId establishmentId);

    /** As avaliações de hoje, paginadas; só as notas baixas, se pedido. */
    ResultPage<RatingView> todayList(EstablishmentId establishmentId, boolean onlyLow, PageQuery page);

    /** As notas baixas de hoje que ninguém resolveu. */
    List<RatingView> openAlerts(EstablishmentId establishmentId);

    void resolve(EstablishmentId establishmentId, RatingId id, UserId by);

    /**
     * A média de quem está logado nos últimos {@code MY_DAYS} dias; null se o restaurante não mostra
     * a média ao garçom.
     */
    RatingSummary mine(EstablishmentId establishmentId, UserId userId);

    boolean waiterSeesAverage(EstablishmentId establishmentId);

    void setWaiterSeesAverage(EstablishmentId establishmentId, boolean visible);
}
