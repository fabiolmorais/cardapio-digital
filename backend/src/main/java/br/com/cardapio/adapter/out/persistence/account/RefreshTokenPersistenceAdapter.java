package br.com.cardapio.adapter.out.persistence.account;

import br.com.cardapio.application.port.out.account.RefreshTokenPort;
import br.com.cardapio.domain.account.model.RefreshToken;
import br.com.cardapio.domain.account.model.RefreshTokenHash;
import br.com.cardapio.domain.account.model.RefreshTokenId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
class RefreshTokenPersistenceAdapter implements RefreshTokenPort {

    private final RefreshTokenJpaRepository tokens;

    RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    public void save(RefreshToken token) {
        tokens.save(AccountPersistenceMapper.toEntity(token));
    }

    @Override
    public Optional<RefreshToken> findByHash(RefreshTokenHash hash) {
        return tokens.findByTokenHash(hash.value()).map(AccountPersistenceMapper::toDomain);
    }

    @Override
    public void revokeFamily(RefreshTokenId familyId, Instant now) {
        tokens.revokeFamily(familyId.value(), now);
    }
}
