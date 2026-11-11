package br.com.cardapio.application.port.in.tablesession;

import br.com.cardapio.application.port.out.tablesession.BlockedPhoneView;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;

/** O celular bloqueado como a equipe vê: a mesa, quando e quem bloqueou. */
public record BlockedPhone(TableSessionId sessionId, DiningTableId tableId, String tableLabel, String sectorName,
                           Instant blockedAt, String blockedByName) {

    public static BlockedPhone from(BlockedPhoneView view) {
        return new BlockedPhone(view.sessionId(), view.tableId(), view.tableLabel(), view.sectorName(),
                view.blockedAt(), view.blockedByName());
    }
}
