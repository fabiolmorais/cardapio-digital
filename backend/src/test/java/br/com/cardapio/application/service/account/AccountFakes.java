package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.out.account.ActiveMembership;
import br.com.cardapio.application.port.out.account.LoadMembershipPort;
import br.com.cardapio.application.port.out.account.LoadUserPort;
import br.com.cardapio.application.port.out.account.RefreshTokenPort;
import br.com.cardapio.application.port.out.security.*;
import br.com.cardapio.domain.account.model.*;
import br.com.cardapio.domain.shared.model.Email;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Todas as portas de saída das contas em memória, para testar os casos de uso sem banco e sem Spring.
 * O relógio é fixo e só anda quando o teste manda (advance).
 */
final class AccountFakes implements LoadUserPort, LoadMembershipPort, PasswordHasher, AccessTokenIssuer,
        RefreshTokenPort, RandomTokenGenerator {

    static final Duration REFRESH_TTL = Duration.ofDays(14);

    record Row(UserId userId, ActiveMembership membership) {
    }

    final List<User> users = new ArrayList<>();
    final List<Row> memberships = new ArrayList<>();
    final Map<RefreshTokenId, RefreshToken> refreshTokens = new LinkedHashMap<>();
    TokenSubject lastSubject;
    /** Simula outra renovação revogando o token entre a leitura e o UPDATE. */
    boolean anotherRenewalWinsTheRace;
    int matchesCalls;
    private int generated;
    private Instant now = Instant.parse("2026-10-26T12:00:00Z");

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

    SessionIssuer sessionIssuer() {
        return new SessionIssuer(this, this, this, clock(), REFRESH_TTL);
    }

    User addUser(String name, boolean active) {
        User user = new User(UserId.newId(), name, new Email(name.toLowerCase() + "@sakura.com"),
                new PasswordHash("hash:sushi2026bom"), active);
        users.add(user);
        return user;
    }

    /** Vínculo com papel e restaurante escolhidos (a pessoa da equipe que entra por PIN). */
    ActiveMembership addMembership(User user, EstablishmentId establishmentId, Role role) {
        ActiveMembership m = new ActiveMembership(MembershipId.newId(), establishmentId, "Sakura Sushi",
                "sakura-sushi", role);
        memberships.add(new Row(user.id(), m));
        return m;
    }

    ActiveMembership addMembership(User user, String establishmentName) {
        ActiveMembership m = new ActiveMembership(MembershipId.newId(), EstablishmentId.newId(), establishmentName,
                "sakura-sushi", Role.OWNER);
        memberships.add(new Row(user.id(), m));
        return m;
    }

    // LoadUserPort
    @Override
    public boolean existsByEmail(Email email) { return findByEmail(email).isPresent(); }

    @Override
    public Optional<User> findByEmail(Email email) {
        return users.stream().filter(u -> u.email().equals(email)).findFirst();
    }
    @Override
    public Optional<User> findById(UserId id) {
        return users.stream().filter(u -> u.id().equals(id)).findFirst();
    }

    // LoadMembershipPort
    @Override
    public List<ActiveMembership> findActiveByUser(UserId userId) {
        return memberships.stream().filter(r -> r.userId().equals(userId)).map(Row::membership).toList();
    }

    @Override
    public Optional<ActiveMembership> findActive(UserId userId, EstablishmentId establishmentId) {
        return findActiveByUser(userId).stream()
                .filter(m -> m.establishmentId().equals(establishmentId)).findFirst();
    }

    // PasswordHasher
    @Override
    public PasswordHash hash(String rawPassword) { return new PasswordHash("hash:" + rawPassword); }

    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
        matchesCalls++;
        return hash.value().equals("hash:" + rawPassword);
    }

    // AccessTokenIssuer
    @Override
    public AccessToken issue(TokenSubject subject) {
        lastSubject = subject;
        return new AccessToken("token-falso", now.plus(Duration.ofMinutes(15)));
    }

    // RefreshTokenPort
    @Override
    public void save(RefreshToken token) { refreshTokens.put(token.id(), token); }

    @Override
    public Optional<RefreshToken> findByHash(RefreshTokenHash hash) {
        return refreshTokens.values().stream().filter(t -> t.hash().equals(hash)).findFirst();
    }

    @Override
    public boolean revokeIfActive(RefreshTokenId id, Instant at) {
        RefreshToken token = refreshTokens.get(id);
        if (anotherRenewalWinsTheRace || token == null || token.isRevoked()) {
            return false;
        }
        refreshTokens.put(id, token.revoke(at));
        return true;
    }

    @Override
    public void revokeFamily(RefreshTokenId familyId, Instant at) {
        refreshTokens.replaceAll((id, t) -> t.familyId().equals(familyId) ? t.revoke(at) : t);
    }

    // RandomTokenGenerator: previsível no teste ("refresh-1", "refresh-2"…)
    @Override
    public String generate() { return "refresh-" + (++generated); }
}
