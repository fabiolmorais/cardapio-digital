package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.out.account.PinLockoutPort;
import br.com.cardapio.application.port.out.device.DevicePort;
import br.com.cardapio.application.port.out.device.DeviceSessionPort;
import br.com.cardapio.application.port.out.device.LinkedDevice;
import br.com.cardapio.application.port.out.team.TeamMember;
import br.com.cardapio.application.port.out.team.TeamMemberPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.*;
import br.com.cardapio.domain.device.model.Device;
import br.com.cardapio.domain.device.model.DeviceId;
import br.com.cardapio.domain.device.model.DeviceSecretHash;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * As portas do login por PIN que o AccountFakes não tem: aparelhos, equipe, tentativas de PIN
 * e sessões por aparelho. Só o que o caso de uso usa; o resto não é chamado.
 */
final class PinLoginFakes implements DevicePort, TeamMemberPort, PinLockoutPort, DeviceSessionPort {

    final Map<DeviceId, Device> devices = new LinkedHashMap<>();
    final Map<MembershipId, TeamMember> members = new LinkedHashMap<>();
    final Map<MembershipId, PinLockout> lockouts = new LinkedHashMap<>();
    final Map<RefreshTokenId, DeviceId> sessions = new LinkedHashMap<>();
    int locks;

    // DevicePort
    @Override
    public void save(Device device) { devices.put(device.id(), device); }

    @Override
    public Optional<LinkedDevice> findActiveByTokenHash(DeviceSecretHash tokenHash) {
        return devices.values().stream().filter(d -> d.isActive() && tokenHash.equals(d.tokenHash())).findFirst()
                .map(d -> new LinkedDevice(d, "Sakura Sushi"));
    }

    @Override
    public ResultPage<Device> findActive(EstablishmentId establishmentId, PageQuery page) { throw unused(); }

    @Override
    public Optional<Device> find(EstablishmentId establishmentId, DeviceId id) { throw unused(); }

    @Override
    public Optional<LinkedDevice> findForClaim(DeviceId id) { throw unused(); }

    // TeamMemberPort
    @Override
    public Optional<TeamMember> findMember(EstablishmentId establishmentId, MembershipId memberId) {
        return Optional.ofNullable(members.get(memberId))
                .filter(m -> m.membership().establishmentId().equals(establishmentId));
    }

    @Override
    public ResultPage<TeamMember> findTeam(EstablishmentId establishmentId, PageQuery page) { throw unused(); }

    @Override
    public ResultPage<TeamMember> findPinMembers(EstablishmentId establishmentId, PageQuery page) { throw unused(); }

    @Override
    public void addMember(User user, Membership membership) { throw unused(); }

    @Override
    public void updateMember(Membership membership) { throw unused(); }

    // PinLockoutPort
    @Override
    public Optional<PinLockout> lockAndFind(EstablishmentId establishmentId, MembershipId membershipId) {
        locks++;
        return Optional.ofNullable(lockouts.get(membershipId));
    }

    @Override
    public void save(PinLockout lockout) { lockouts.put(lockout.membershipId(), lockout); }

    // DeviceSessionPort
    @Override
    public void register(RefreshTokenId familyId, DeviceId deviceId, EstablishmentId establishmentId, Instant now) {
        sessions.put(familyId, deviceId);
    }

    @Override
    public void revokeAll(DeviceId deviceId, Instant now) { throw unused(); }

    private static UnsupportedOperationException unused() {
        return new UnsupportedOperationException("não usado no login por PIN");
    }
}
