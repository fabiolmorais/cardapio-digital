package br.com.cardapio.application.service.tablesession;

import br.com.cardapio.application.port.in.tablesession.EndExpiredSessionsUseCase;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class EndExpiredSessionsService implements EndExpiredSessionsUseCase {

    private final TableSessionPort sessions;
    private final Clock clock;

    public EndExpiredSessionsService(TableSessionPort sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int endExpired() {
        return sessions.endExpired(clock.instant());
    }
}
