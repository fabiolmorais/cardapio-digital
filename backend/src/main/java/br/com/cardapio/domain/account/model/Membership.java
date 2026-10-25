package br.com.cardapio.domain.account.model;

import java.util.Objects;

public record Membership(
        MembershipId id,
        UserId userId,
        EstablishmentId establishmentId,
        Role role,
        boolean active
) {

    public Membership {
        Objects.requireNonNull(id);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(role);
    }

    /** Quem cria a conta vira dono do primeiro estabelecimento. */
    public static Membership owner(UserId userId, EstablishmentId establishmentId) {
        return new Membership(MembershipId.newId(), userId, establishmentId, Role.OWNER, true);
    }
}
