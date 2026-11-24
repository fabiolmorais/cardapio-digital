package br.com.cardapio.application.service.rating;

import br.com.cardapio.application.port.in.rating.RateServiceUseCase;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.rating.RatingPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.service.floor.PublicTableLookup;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.rating.event.LowRatingReceived;
import br.com.cardapio.domain.rating.exception.RatingStateException;
import br.com.cardapio.domain.rating.model.ServiceRating;
import br.com.cardapio.domain.rating.service.RatingPolicy;
import br.com.cardapio.domain.shared.model.TokenHash;
import br.com.cardapio.domain.tablesession.exception.TableSessionRequiredException;
import br.com.cardapio.domain.tablesession.model.TableSession;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * A avaliação pelo celular (RF40). Não usa o porteiro de sempre ({@code TableVisits}): o fechamento da
 * comanda encerra a sessão, e o cliente ainda pode avaliar por um tempo depois disso
 * ({@link RatingPolicy}). O cookie continua tendo de ser desta mesa.
 */
public class RateServiceService implements RateServiceUseCase {

    private final DiningTablePort tables;
    private final TableSessionPort sessions;
    private final RatingPort ratings;
    private final TabPort tabs;
    private final OrderPort orders;
    private final EventPublisherPort events;
    private final Clock clock;

    public RateServiceService(DiningTablePort tables, TableSessionPort sessions, RatingPort ratings, TabPort tabs,
                              OrderPort orders, EventPublisherPort events, Clock clock) {
        this.tables = tables;
        this.sessions = sessions;
        this.ratings = ratings;
        this.tabs = tabs;
        this.orders = orders;
        this.events = events;
        this.clock = clock;
    }

    private record Context(DiningTable table, TableSession session, RatingPort.Visit visit,
                           Optional<ServiceRating> rated, boolean canRate) {
    }

    private Context context(String tableToken, String sessionToken, Instant now) {
        DiningTable table = PublicTableLookup.resolve(tables, tableToken).table();
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new TableSessionRequiredException();
        }
        TableSession session = sessions.findByTokenHash(TokenHash.of(sessionToken))
                .filter(s -> s.isFor(table.id()))
                .orElseThrow(TableSessionRequiredException::new);
        RatingPort.Visit visit = ratings.visit(table.establishmentId(), table.id(), session.createdAt());
        Optional<ServiceRating> rated = ratings.findBySession(table.establishmentId(), session.id());
        return new Context(table, session, visit, rated,
                RatingPolicy.canRate(session, visit.billRequested(), visit.tabClosed(), now));
    }

    @Override
    @Transactional(readOnly = true)
    public RatingOffer offer(String tableToken, String sessionToken) {
        Context c = context(tableToken, sessionToken, clock.instant());
        if (c.rated().isPresent()) {
            ServiceRating r = c.rated().get();
            return new RatingOffer(false, r.stars(), r.invitesToGoogle(), null);
        }
        if (!c.canRate()) {
            return RatingOffer.NONE;
        }
        return new RatingOffer(true, null, false, waiter(c).map(id -> firstName(c, id)).orElse(null));
    }

    @Override
    @Transactional
    public RatingOffer rate(RateServiceCommand command) {
        Instant now = clock.instant();
        Context c = context(command.tableToken(), command.sessionToken(), now);
        if (c.rated().isPresent()) {
            throw new RatingStateException("Este celular já avaliou o atendimento. Obrigado!");
        }
        if (!c.canRate()) {
            throw new RatingStateException("A avaliação abre quando a mesa pede a conta.");
        }
        ServiceRating rating = ServiceRating.rate(c.table().establishmentId(), c.session().id(), c.table().id(),
                c.visit().tab().orElse(null), waiter(c).orElse(null), command.stars(), command.tags(),
                command.comment(), now);
        if (!ratings.insert(rating)) {
            throw new RatingStateException("Este celular já avaliou o atendimento. Obrigado!");
        }
        if (rating.isLow()) {
            events.publish(new LowRatingReceived(rating.establishmentId(), rating.tableId(), rating.id(),
                    rating.stars(), now));
        }
        return new RatingOffer(false, rating.stars(), rating.invitesToGoogle(), null);
    }

    @Override
    @Transactional
    public void clickGoogle(String tableToken, String sessionToken) {
        Context c = context(tableToken, sessionToken, clock.instant());
        ServiceRating rating = c.rated()
                .orElseThrow(() -> new RatingStateException("Este celular ainda não avaliou o atendimento."));
        ServiceRating clicked = rating.clickGoogle(clock.instant());
        if (!clicked.equals(rating)) {
            ratings.update(clicked);
        }
    }

    /** Quem mais atendeu a comanda da visita (chamados aceitos e itens entregues). */
    private Optional<UserId> waiter(Context c) {
        Optional<TabId> tab = c.visit().tab();
        return tab.flatMap(id -> tabs.findView(c.table().establishmentId(), id))
                .flatMap(view -> tabs.findTopAttendant(view.tab()));
    }

    /** Só o primeiro nome: a mesa não precisa do sobrenome de ninguém da equipe. */
    private String firstName(Context c, UserId id) {
        String name = orders.findStaffNames(c.table().establishmentId(), Set.of(id)).get(id);
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.strip().split("\\s+")[0];
    }
}
