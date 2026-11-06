package br.com.cardapio.application.port.out.account;

import br.com.cardapio.domain.account.model.PasswordHash;
import br.com.cardapio.domain.account.model.PasswordResetToken;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.shared.model.TokenHash;

import java.time.Instant;
import java.util.Optional;

/**
 * Os links de "esqueci a senha". Como a pessoa (app_user), são globais: sem establishmentId.
 * Quem chega aqui só tem o e-mail ou o token, e nunca vê dado de restaurante nenhum.
 */
public interface PasswordResetPort {

    void save(PasswordResetToken token);

    Optional<PasswordResetToken> findByHash(TokenHash hash);

    /** Quantos links a pessoa pediu desde {@code since}. */
    long countSince(UserId userId, Instant since);

    /**
     * Troca a senha, gasta todos os links ainda válidos da pessoa e encerra as sessões dela (os
     * refresh tokens): quem estava logado com a senha antiga precisa entrar de novo.
     */
    void changePassword(UserId userId, PasswordHash newHash, Instant now);
}
