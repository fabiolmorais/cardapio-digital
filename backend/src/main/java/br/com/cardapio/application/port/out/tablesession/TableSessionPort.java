package br.com.cardapio.application.port.out.tablesession;

import br.com.cardapio.domain.shared.model.TokenHash;
import br.com.cardapio.domain.tablesession.model.TableSession;

import java.util.Optional;

public interface TableSessionPort {

    void save(TableSession session);

    /**
     * Busca pelo hash do cookie, sem restaurante: quem chama é a página pública, e é o token
     * (aleatório e secreto) que diz de qual sessão se trata.
     */
    Optional<TableSession> findByTokenHash(TokenHash tokenHash);
}
