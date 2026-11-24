package br.com.cardapio.application.port.out.rating;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.rating.model.RatingId;
import br.com.cardapio.domain.rating.model.RatingSummary;
import br.com.cardapio.domain.rating.model.RatingView;
import br.com.cardapio.domain.rating.model.ServiceRating;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** As avaliações do atendimento (RF40), sempre de um restaurante. */
public interface RatingPort {

    /**
     * A visita de um celular, desde {@code since} (quando a sessão abriu): a comanda (a aberta da mesa,
     * ou a fechada mais recente depois disso), se a mesa pediu a conta e se a comanda fechou.
     */
    Visit visit(EstablishmentId establishmentId, DiningTableId tableId, Instant since);

    record Visit(Optional<TabId> tab, boolean billRequested, boolean tabClosed) {
    }

    Optional<ServiceRating> findBySession(EstablishmentId establishmentId, TableSessionId sessionId);

    Optional<ServiceRating> find(EstablishmentId establishmentId, RatingId id);

    /** Grava a avaliação nova; false se este celular já tinha avaliado (uma por sessão). */
    boolean insert(ServiceRating rating);

    /** Grava o toque no Google e a resolução do alerta. */
    void update(ServiceRating rating);

    /** As avaliações de [início, fim), da mais nova para a mais velha; só as notas baixas, se pedido. */
    ResultPage<RatingView> findBetween(EstablishmentId establishmentId, Instant start, Instant end, boolean onlyLow,
                                       PageQuery page);

    /** O resumo de [início, fim). */
    RatingSummary summary(EstablishmentId establishmentId, Instant start, Instant end);

    /** As notas baixas de [início, fim) que ninguém marcou como resolvidas, da mais nova para a mais velha. */
    List<RatingView> openAlerts(EstablishmentId establishmentId, Instant start, Instant end);

    /** A média de um garçom nas avaliações de [início, fim). */
    RatingSummary waiterSummary(EstablishmentId establishmentId, UserId waiterId, Instant start, Instant end);

    /** O garçom vê a própria média no app? (Começa desligado.) */
    boolean waiterSeesAverage(EstablishmentId establishmentId);

    void saveWaiterSeesAverage(EstablishmentId establishmentId, boolean visible, Instant now);
}
