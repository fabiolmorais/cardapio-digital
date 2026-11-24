package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.menu.model.MenuEvent;

import java.util.List;

/** O celular da mesa conta o que fez no cardápio (REL-27, 28, 29). */
public interface RecordMenuEventsUseCase {

    /** Grava os eventos da sessão; devolve quantos foram gravados. */
    int record(String tableToken, String sessionToken, List<MenuEvent> events);
}
