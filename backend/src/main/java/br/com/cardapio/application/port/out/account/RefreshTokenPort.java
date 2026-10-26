package br.com.cardapio.application.port.out.account;

import br.com.cardapio.domain.account.model.RefreshToken;
import br.com.cardapio.domain.account.model.RefreshTokenHash;
import br.com.cardapio.domain.account.model.RefreshTokenId;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenPort {

    /** Grava um token novo ou atualiza um existente (ao revogar). */
    void save(RefreshToken token);

    Optional<RefreshToken> findByHash(RefreshTokenHash hash);

    /** Revoga todos os tokens ainda ativos da família (logout ou suspeita de cópia). */
    void revokeFamily(RefreshTokenId familyId, Instant now);
}
