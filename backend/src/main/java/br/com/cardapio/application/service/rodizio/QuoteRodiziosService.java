package br.com.cardapio.application.service.rodizio;

import br.com.cardapio.application.port.in.rodizio.QuoteRodiziosUseCase;
import br.com.cardapio.application.port.in.rodizio.RodizioQuote;
import br.com.cardapio.application.port.out.account.EstablishmentTimeZonePort;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.rodizio.RodizioPort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.application.service.tablesession.TableVisits;
import br.com.cardapio.domain.account.model.EstablishmentId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

public class QuoteRodiziosService implements QuoteRodiziosUseCase {

    private final RodizioPort rodizios;
    private final EstablishmentTimeZonePort timeZones;
    private final DiningTablePort tables;
    private final TableSessionPort sessions;
    private final Clock clock;

    public QuoteRodiziosService(RodizioPort rodizios, EstablishmentTimeZonePort timeZones, DiningTablePort tables,
                                TableSessionPort sessions, Clock clock) {
        this.rodizios = rodizios;
        this.timeZones = timeZones;
        this.tables = tables;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<RodizioQuote> forStaff(EstablishmentId establishmentId, PageQuery page) {
        return quote(establishmentId, page);
    }

    /** Não é readOnly: como nos pedidos, olhar conta como uso e renova a sessão do celular. */
    @Override
    @Transactional
    public ResultPage<RodizioQuote> forTable(String tableToken, String sessionToken, PageQuery page) {
        var visit = TableVisits.require(tables, sessions, tableToken, sessionToken, clock.instant());
        return quote(visit.table().establishmentId(), page);
    }

    private ResultPage<RodizioQuote> quote(EstablishmentId establishmentId, PageQuery page) {
        LocalDateTime localTime = LocalDateTime.ofInstant(clock.instant(), timeZones.timeZoneOf(establishmentId));
        return rodizios.findActive(establishmentId, page).map(r -> RodizioQuote.of(r, localTime));
    }
}
