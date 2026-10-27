package br.com.cardapio.application.service.device;

import br.com.cardapio.application.port.in.device.ClaimPairingUseCase;
import br.com.cardapio.application.port.in.device.ClaimResult;
import br.com.cardapio.application.port.in.device.DeviceSummary;
import br.com.cardapio.application.port.out.device.DevicePairingPort;
import br.com.cardapio.application.port.out.device.DevicePort;
import br.com.cardapio.application.port.out.device.LinkedDevice;
import br.com.cardapio.application.port.out.security.RandomTokenGenerator;
import br.com.cardapio.domain.device.exception.PairingNotFoundException;
import br.com.cardapio.domain.device.model.Device;
import br.com.cardapio.domain.device.model.DevicePairing;
import br.com.cardapio.domain.device.model.DeviceSecretHash;
import br.com.cardapio.domain.device.model.PairingId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class ClaimPairingService implements ClaimPairingUseCase {

    private final DevicePairingPort pairings;
    private final DevicePort devices;
    private final RandomTokenGenerator tokens;
    private final Clock clock;

    public ClaimPairingService(DevicePairingPort pairings, DevicePort devices, RandomTokenGenerator tokens, Clock clock) {
        this.pairings = pairings;
        this.devices = devices;
        this.tokens = tokens;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ClaimResult claim(PairingId id, String secret) {
        Instant now = clock.instant();
        DevicePairing pairing = pairings.findById(id)
                .filter(p -> p.belongsTo(secret))
                .filter(p -> !p.isClaimed())
                .orElseThrow(PairingNotFoundException::new);

        if (!pairing.isLinked()) {
            if (pairing.isExpired(now)) {
                throw new PairingNotFoundException();
            }
            return new ClaimResult.Pending();
        }

        LinkedDevice linked = devices.findForClaim(pairing.deviceId())
                .filter(d -> d.device().isActive())
                .orElseThrow(PairingNotFoundException::new);
        if (!pairings.claimIfLinked(pairing.id(), now)) {
            // Outra busca levou o token agora há pouco: só uma pode levar.
            throw new PairingNotFoundException();
        }

        String rawToken = tokens.generate();
        Device device = linked.device().issueToken(DeviceSecretHash.of(rawToken), now);
        devices.save(device);
        return new ClaimResult.Linked(rawToken, DeviceSummary.from(device), linked.establishmentName());
    }
}
