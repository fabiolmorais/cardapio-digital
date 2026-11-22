package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.LogoutUseCase;
import br.com.cardapio.application.port.out.account.RefreshTokenPort;
import br.com.cardapio.application.port.out.device.StaffShiftPort;
import br.com.cardapio.domain.account.model.RefreshTokenHash;
import br.com.cardapio.domain.device.model.ShiftEnd;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class LogoutService implements LogoutUseCase {

    private final RefreshTokenPort refreshTokens;
    private final StaffShiftPort shifts;
    private final Clock clock;

    public LogoutService(RefreshTokenPort refreshTokens, StaffShiftPort shifts, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.shifts = shifts;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokens.findByHash(RefreshTokenHash.of(rawToken)).ifPresent(token -> {
            refreshTokens.revokeFamily(token.familyId(), clock.instant());
            // Se a sessão veio do PIN, o turno acaba aqui (o login por e-mail não abre turno).
            shifts.endSession(token.familyId(), ShiftEnd.LOGOUT, clock.instant());
        });
    }
}
