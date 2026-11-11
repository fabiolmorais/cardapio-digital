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
     * "Liberar": confirma a presença (pelo garçom) só da sessão de quem pediu, se ela ainda estiver
     * ativa, sem presença e sem bloqueio. Liberar a mesa inteira deixaria passar junto um celular
     * que abriu o QR de longe. Devolve false se não havia o que liberar.
     */
    boolean unlockSession(EstablishmentId establishmentId, TableSessionId sessionId, UserId by, Instant now);

    /**
     * "Bloquear este celular". false se a sessão não é do restaurante ou já estava bloqueada.
     */
    boolean block(EstablishmentId establishmentId, TableSessionId sessionId, UserId by, Instant now);

    /**
     * Encerra (ended_at) as sessões que venceram por falta de uso. É uma tarefa do sistema, de
     * todos os restaurantes de uma vez. Devolve quantas encerrou.
     */
    int endExpired(Instant now);

    /**
     * A comanda da mesa fechou: encerra todas as sessões ainda abertas da mesa. Quem voltar a
     * escanear começa uma visita nova (e confirma a presença de novo). Devolve quantas.
     */
    int endTable(EstablishmentId establishmentId, DiningTableId tableId, Instant now);
}
