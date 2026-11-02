package br.com.cardapio.application.service.tablesession;

import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.service.floor.PublicTableLookup;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.shared.model.TokenHash;
import br.com.cardapio.domain.tablesession.exception.TableSessionRequiredException;
import br.com.cardapio.domain.tablesession.model.TableSession;

import java.time.Instant;

/**
 * O porteiro das ações da mesa (chamar, pedir): o QR abre uma mesa ativa, e o cookie traz uma
 * sessão válida DESTA mesa? Se sim, o uso empurra o prazo da sessão. Se não, 401 (ou 404 para
 * o QR que não existe). Um cookie de outra mesa não serve: quem fotografou o QR da mesa 4 não
 * chama o garçom pela mesa 5.
 */
public final class TableVisits {

    private TableVisits() {
    }

    /** A mesa e a sessão em uso, já com o prazo renovado. */
    public record Visit(DiningTable table, TableSession session) {
    }

    public static Visit require(DiningTablePort tables, TableSessionPort sessions, String tableToken,
                                String sessionToken, Instant now) {
        DiningTable table = PublicTableLookup.resolve(tables, tableToken).table();
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new TableSessionRequiredException();
        }
        TableSession session = sessions.findByTokenHash(TokenHash.of(sessionToken))
                .filter(s -> s.isActive(now) && s.isFor(table.id()))
                .orElseThrow(TableSessionRequiredException::new)
                .touch(now);
        sessions.save(session);
        return new Visit(table, session);
    }
}
