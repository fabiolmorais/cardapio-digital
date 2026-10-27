package br.com.cardapio.application.service.device;

import br.com.cardapio.application.port.out.device.DevicePairingPort;
import br.com.cardapio.application.port.out.device.DevicePort;
import br.com.cardapio.application.port.out.device.LinkedDevice;
import br.com.cardapio.application.port.out.security.RandomCodeGenerator;
import br.com.cardapio.application.port.out.security.RandomTokenGenerator;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.device.model.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Portas dos aparelhos em memória, com relógio que só anda quando o teste manda e códigos e
 * tokens previsíveis ("000001", "token-1"…).
 */
final class DeviceFakes implements DevicePairingPort, DevicePort, RandomCodeGenerator, RandomTokenGenerator {

    static final Duration TTL = Duration.ofMinutes(10);
    static final String ESTABLISHMENT_NAME = "Sakura Sushi";

    final Map<PairingId, DevicePairing> pairings = new LinkedHashMap<>();
    final Map<DeviceId, Device> devices = new LinkedHashMap<>();
    /** Próximos códigos a sortear; vazio = sequência "000001", "000002"… */
    final Deque<String> nextCodes = new ArrayDeque<>();
    private int codeCounter;
    private int tokenCounter;
    private Instant now = Instant.parse("2026-10-27T12:00:00Z");

    Clock clock() {
        return new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return now; }
        };
    }

    void advance(Duration duration) {
        now = now.plus(duration);
    }

    // DevicePairingPort
    @Override
    public void save(DevicePairing pairing) { pairings.put(pairing.id(), pairing); }

    @Override
    public Optional<DevicePairing> findById(PairingId id) { return Optional.ofNullable(pairings.get(id)); }

    @Override
    public Optional<DevicePairing> findOpenByCode(PairingCode code, Instant at) {
        return pairings.values().stream().filter(p -> p.code().equals(code) && p.isOpen(at)).findFirst();
    }

    @Override
    public boolean linkIfOpen(PairingId id, DeviceId deviceId, Instant at) {
        DevicePairing p = pairings.get(id);
        if (p == null || !p.isOpen(at)) {
            return false;
        }
        pairings.put(id, new DevicePairing(p.id(), p.code(), p.secretHash(), p.createdAt(), p.expiresAt(), deviceId, null));
        return true;
    }

    @Override
    public boolean claimIfLinked(PairingId id, Instant at) {
        DevicePairing p = pairings.get(id);
        if (p == null || !p.isLinked() || p.isClaimed()) {
            return false;
        }
        pairings.put(id, new DevicePairing(p.id(), p.code(), p.secretHash(), p.createdAt(), p.expiresAt(), p.deviceId(), at));
        return true;
    }

    // DevicePort
    @Override
    public void save(Device device) { devices.put(device.id(), device); }

    @Override
    public ResultPage<Device> findActive(EstablishmentId establishmentId, PageQuery page) {
        List<Device> all = devices.values().stream()
                .filter(d -> d.establishmentId().equals(establishmentId) && d.isActive())
                .sorted(Comparator.comparing(Device::name)).toList();
        return new ResultPage<>(all, page.page(), page.size(), all.size());
    }

    @Override
    public Optional<Device> find(EstablishmentId establishmentId, DeviceId id) {
        return Optional.ofNullable(devices.get(id)).filter(d -> d.establishmentId().equals(establishmentId));
    }

    @Override
    public Optional<LinkedDevice> findForClaim(DeviceId id) {
        return Optional.ofNullable(devices.get(id)).map(d -> new LinkedDevice(d, ESTABLISHMENT_NAME));
    }

    // RandomCodeGenerator
    @Override
    public String digits(int length) {
        return nextCodes.isEmpty() ? String.format("%0" + length + "d", ++codeCounter) : nextCodes.pop();
    }

    // RandomTokenGenerator
    @Override
    public String generate() { return "token-" + (++tokenCounter); }
}
