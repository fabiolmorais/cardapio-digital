package br.com.cardapio.application.port.out.device;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.RefreshTokenId;
import br.com.cardapio.domain.device.model.DeviceId;

import java.time.Instant;

/** Liga as sessões abertas por PIN ao aparelho, para encerrá-las quando ele for desvinculado. */
public interface DeviceSessionPort {

    /** A sessão (família de refresh tokens) foi aberta neste aparelho. */
    void register(RefreshTokenId familyId, DeviceId deviceId, EstablishmentId establishmentId, Instant now);

    /** Revoga os refresh tokens ativos de todas as sessões abertas no aparelho. */
    void revokeAll(DeviceId deviceId, Instant now);
}
