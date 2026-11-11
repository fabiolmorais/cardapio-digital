package br.com.cardapio.application.service.device;

import br.com.cardapio.application.port.in.device.PairingStarted;
import br.com.cardapio.application.port.in.device.StartPairingUseCase;
import br.com.cardapio.application.port.out.device.DevicePairingPort;
import br.com.cardapio.application.port.out.security.RandomCodeGenerator;
import br.com.cardapio.application.port.out.security.RandomTokenGenerator;
import br.com.cardapio.domain.device.model.DevicePairing;
import br.com.cardapio.domain.device.model.DeviceSecretHash;
import br.com.cardapio.domain.device.model.PairingCode;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class StartPairingService implements StartPairingUseCase {

    private final DevicePairingPort pairings;
    private final RandomCodeGenerator codes;
    private final RandomTokenGenerator secrets;
    private final Clock clock;
    private final Duration ttl;

    public StartPairingService(DevicePairingPort pairings, RandomCodeGenerator codes, RandomTokenGenerator secrets,
                               Clock clock, Duration ttl) {
        this.pairings = pairings;
        this.codes = codes;
        this.secrets = secrets;
        this.clock = clock;
        this.ttl = Objects.requireNonNull(ttl);
    }

    @Override
    @Transactional
    public PairingStarted start() {
        Instant now = clock.instant();
        PairingCode code = PairingCodes.free(pairings, codes, now);
        String secret = secrets.generate();

        DevicePairing pairing = DevicePairing.start(code, DeviceSecretHash.of(secret), now, ttl);
        pairings.save(pairing);
        return new PairingStarted(pairing.id(), code, secret, pairing.expiresAt());
    }
}
