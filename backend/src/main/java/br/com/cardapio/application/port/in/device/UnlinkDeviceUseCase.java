package br.com.cardapio.application.port.in.device;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.device.model.DeviceId;

public interface UnlinkDeviceUseCase {

    /** Desvincula o aparelho: o token dele deixa de valer. Desvincular de novo não é erro. */
    void unlink(EstablishmentId establishmentId, DeviceId deviceId);
}
