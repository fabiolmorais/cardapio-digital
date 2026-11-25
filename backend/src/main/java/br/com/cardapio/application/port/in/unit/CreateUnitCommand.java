package br.com.cardapio.application.port.in.unit;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;

import java.util.Objects;

/**
 * @param current   restaurante de quem pede (do token): a unidade nova fica na mesma empresa
 * @param userId    quem pede (do token): vira dono da unidade nova
 * @param actorRole papel de quem pede (do token): só o dono cria unidades
 */
public record CreateUnitCommand(EstablishmentId current, UserId userId, Role actorRole, String name,
                                String timeZone) {
    public CreateUnitCommand {
        Objects.requireNonNull(current);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(actorRole);
    }
}
