package br.com.cardapio.application.port.in.tablesession;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

/** "Desbloquear": o celular volta a poder chamar e pedir (se tiver a presença confirmada). */
public interface UnblockPhoneUseCase {

    void unblock(EstablishmentId establishmentId, TableSessionId sessionId);
}
