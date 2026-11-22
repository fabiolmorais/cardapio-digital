package br.com.cardapio.application.port.out.device;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.RefreshTokenId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.device.model.DeviceId;
import br.com.cardapio.domain.device.model.ShiftEnd;

import java.time.Instant;

/** Os turnos de quem entra pelo PIN (REL-53): um por sessão aberta no aparelho. */
public interface StaffShiftPort {

    /** Fecha o turno aberto no aparelho (outra pessoa entrou: REPLACED) e abre o da sessão nova. */
    void start(EstablishmentId establishmentId, UserId userId, DeviceId deviceId, RefreshTokenId familyId,
               Instant now);

    /** Fecha o turno da sessão, se ainda estava aberto. */
    void endSession(RefreshTokenId familyId, ShiftEnd reason, Instant now);

    /** Fecha os turnos abertos no aparelho. */
    void endDevice(DeviceId deviceId, ShiftEnd reason, Instant now);

    /**
     * Fecha (EXPIRED) os turnos cuja sessão não vale mais: todos os refresh tokens vencidos ou
     * revogados. O fim é a última renovação (o último momento em que o app estava em uso) ou a
     * revogação. Devolve quantos fechou.
     */
    int endExpired(Instant now);
}
