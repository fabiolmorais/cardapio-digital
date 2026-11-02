package br.com.cardapio.adapter.in.scheduler;

import br.com.cardapio.application.port.in.tablesession.EndExpiredSessionsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * De tempos em tempos (5 minutos, por padrão), encerra as sessões de mesa vencidas.
 *
 * A sessão já deixa de valer sozinha quando vence: toda ação confere o prazo. Esta tarefa só
 * registra o fim (ended_at), para o histórico e para as contas de "sessões abertas" ficarem certas.
 *
 * Sem trava entre instâncias (ShedLock): a tarefa é um UPDATE só, e rodar em duas instâncias ao
 * mesmo tempo dá o mesmo resultado (a segunda não encontra mais nada).
 */
@Component
class TableSessionCleanup {

    private static final Logger log = LoggerFactory.getLogger(TableSessionCleanup.class);

    private final EndExpiredSessionsUseCase endExpired;

    TableSessionCleanup(EndExpiredSessionsUseCase endExpired) {
        this.endExpired = endExpired;
    }

    @Scheduled(fixedDelayString = "${app.table-sessions.cleanup-interval:PT5M}",
            initialDelayString = "${app.table-sessions.cleanup-interval:PT5M}")
    void run() {
        int ended = endExpired.endExpired();
        if (ended > 0) {
            log.info("Sessões de mesa vencidas encerradas: {}", ended);
        }
    }
}
