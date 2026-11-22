package br.com.cardapio.adapter.in.scheduler;

import br.com.cardapio.application.port.in.device.CloseExpiredShiftsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * De 15 em 15 minutos, fecha os turnos de quem entrou pelo PIN e não saiu do app: a sessão venceu
 * ou foi revogada. Um UPDATE só: rodar em duas instâncias dá o mesmo resultado.
 */
@Component
class StaffShiftCleanup {

    private static final Logger log = LoggerFactory.getLogger(StaffShiftCleanup.class);

    private final CloseExpiredShiftsUseCase shifts;

    StaffShiftCleanup(CloseExpiredShiftsUseCase shifts) {
        this.shifts = shifts;
    }

    @Scheduled(fixedDelayString = "${app.staff-shifts.cleanup-interval:PT15M}",
            initialDelayString = "${app.staff-shifts.cleanup-interval:PT15M}")
    void run() {
        int closed = shifts.closeExpired();
        if (closed > 0) {
            log.info("Turnos encerrados pela sessão vencida: {}", closed);
        }
    }
}
