package br.com.cardapio.application.port.out.tablesession;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.model.TokenHash;
import br.com.cardapio.domain.tablesession.model.TableSession;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;
import java.util.Optional;

public interface TableSessionPort {

    void save(TableSession session);

    /**
     * Busca pelo hash do cookie, sem restaurante: quem chama é a página pública, e é o token
     * (aleatório e secreto) que diz de qual sessão se trata.
     */
    Optional<TableSession> findByTokenHash(TokenHash tokenHash);

    /**
     * "Liberar esta mesa": confirma a presença (pelo garçom) de todas as sessões ativas da mesa que
     * ainda não tinham. Quem está sentado junto e abriu o QR também é liberado. Devolve quantas.
     */
    int unlockTable(EstablishmentId establishmentId, DiningTableId tableId, UserId by, Instant now);

    /**
     * "Bloquear este celular". false se a sessão não é do restaurante ou já estava bloqueada.
     */
    boolean block(EstablishmentId establishmentId, TableSessionId sessionId, UserId by, Instant now);

    /**
     * Encerra (ended_at) as sessões que venceram por falta de uso. É uma tarefa do sistema, de
     * todos os restaurantes de uma vez. Devolve quantas encerrou.
     */
    int endExpired(Instant now);
}
