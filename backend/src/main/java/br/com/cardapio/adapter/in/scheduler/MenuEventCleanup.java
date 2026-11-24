package br.com.cardapio.adapter.in.scheduler;

import br.com.cardapio.application.port.in.menu.PurgeMenuEventsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * De 6 em 6 horas, apaga os eventos do cardápio com mais de 13 meses (LGPD: o prazo de retenção). Um
 * DELETE só: rodar em duas instâncias dá o mesmo resultado.
 */
@Component
class MenuEventCleanup {

    private static final Logger log = LoggerFactory.getLogger(MenuEventCleanup.class);

    private final PurgeMenuEventsUseCase purge;

    MenuEventCleanup(PurgeMenuEventsUseCase purge) {
        this.purge = purge;
    }

    @Scheduled(fixedDelayString = "${app.menu-events.cleanup-interval:PT6H}",
            initialDelayString = "${app.menu-events.cleanup-interval:PT6H}")
    void run() {
        int deleted = purge.purge();
        if (deleted > 0) {
            log.info("Eventos do cardápio vencidos apagados: {}", deleted);
        }
    }
}
