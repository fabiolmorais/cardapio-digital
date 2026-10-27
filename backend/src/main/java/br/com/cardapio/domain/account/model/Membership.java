package br.com.cardapio.domain.account.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Objects;

/**
 * Vínculo de uma pessoa com um restaurante: o papel dela lá, o PIN (hash) e se está ativa.
 * Imutável: cada mudança devolve um vínculo novo.
 */
public record Membership(
        MembershipId id,
        UserId userId,
        EstablishmentId establishmentId,
        Role role,
        PasswordHash pinHash,
        boolean active
) {

    public Membership {
        Objects.requireNonNull(id);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(role);
        if (role != Role.OWNER && pinHash == null) {
            throw new IllegalArgumentException("quem não é dono precisa de PIN");
        }
    }

    /** Quem cria a conta vira dono do primeiro estabelecimento. */
    public static Membership owner(UserId userId, EstablishmentId establishmentId) {
        return new Membership(MembershipId.newId(), userId, establishmentId, Role.OWNER, null, true);
    }

    /** Pessoa cadastrada pelo dono ou gerente. Dono só existe um, e vem do cadastro. */
    public static Membership teamMember(UserId userId, EstablishmentId establishmentId, Role role, PasswordHash pinHash) {
        if (role == Role.OWNER) {
            throw new InvalidValueException("O restaurante já tem um dono. Escolha outro papel");
        }
        return new Membership(MembershipId.newId(), userId, establishmentId, role, Objects.requireNonNull(pinHash), true);
    }

    public Membership changeRole(Role newRole) {
        if (newRole == Role.OWNER) {
            throw new InvalidValueException("O restaurante já tem um dono. Escolha outro papel");
        }
        return new Membership(id, userId, establishmentId, newRole, pinHash, active);
    }

    public Membership changePin(PasswordHash newPinHash) {
        return new Membership(id, userId, establishmentId, role, Objects.requireNonNull(newPinHash), active);
    }

    /** Desliga a pessoa do restaurante. O registro fica (histórico de pedidos e chamados). */
    public Membership deactivate() {
        return new Membership(id, userId, establishmentId, role, pinHash, false);
    }

    /** Religa quem foi desligado, com o mesmo papel e o mesmo PIN. */
    public Membership reactivate() {
        return new Membership(id, userId, establishmentId, role, pinHash, true);
    }
}
