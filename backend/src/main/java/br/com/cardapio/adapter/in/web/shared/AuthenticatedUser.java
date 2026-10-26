package br.com.cardapio.adapter.in.web.shared;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Quem está chamando a API, lido do token já validado pelo Spring Security.
 * É daqui (e só daqui) que sai o establishmentId de cada caso de uso.
 */
public record AuthenticatedUser(UserId userId, EstablishmentId establishmentId, Role role) {

    /** Mesmos nomes usados pelo emissor do token (adapter/out/security). */
    static final String ESTABLISHMENT_CLAIM = "est";
    static final String ROLE_CLAIM = "role";

    public static AuthenticatedUser from(Jwt jwt) {
        return new AuthenticatedUser(
                new UserId(UUID.fromString(jwt.getSubject())),
                new EstablishmentId(UUID.fromString(jwt.getClaimAsString(ESTABLISHMENT_CLAIM))),
                Role.valueOf(jwt.getClaimAsString(ROLE_CLAIM)));
    }
}
