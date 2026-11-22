package br.com.cardapio.application.service.device;

import br.com.cardapio.application.port.in.device.CloseExpiredShiftsUseCase;
import br.com.cardapio.application.port.out.device.StaffShiftPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Fecha os turnos cuja sessão venceu ou foi revogada sem a pessoa sair do app (REL-53). */
public class CloseExpiredShiftsService implements CloseExpiredShiftsUseCase {

    private final StaffShiftPort shifts;
    private final Clock clock;

    public CloseExpiredShiftsService(StaffShiftPort shifts, Clock clock) {
        this.shifts = shifts;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int closeExpired() {
        return shifts.endExpired(clock.instant());
    }
}
