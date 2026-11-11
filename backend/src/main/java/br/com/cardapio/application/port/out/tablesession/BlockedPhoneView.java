package br.com.cardapio.application.port.out.tablesession;

import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;

/**
 * Um celular bloqueado pela equipe que ainda está com a sessão aberta: a mesa, quando e quem
 * bloqueou. O cliente é anônimo; o celular é só a sessão.
 *
 * @param blockedByName quem bloqueou; null se a pessoa não existe mais no cadastro
 */
public record BlockedPhoneView(TableSessionId sessionId, DiningTableId tableId, String tableLabel, String sectorName,
                           Instant blockedAt, String blockedByName) {
}
