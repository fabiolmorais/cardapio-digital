package br.com.cardapio.application.port.out.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuEvent;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;
import java.util.List;

/** Os eventos do celular no cardápio (REL-27, 28, 29). */
public interface MenuEventPort {

    /** Quantos eventos a sessão já gravou. */
    int countForSession(TableSessionId sessionId);

    /** Grava os eventos dos pratos deste restaurante (os de outro restaurante ou apagados são ignorados). */
    int save(EstablishmentId establishmentId, TableSessionId sessionId, List<MenuEvent> events, Instant at);

    /** Apaga os eventos de antes de {@code before}; devolve quantos. */
    int deleteBefore(Instant before);
}
