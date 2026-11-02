package br.com.cardapio.application.port.in.realtime;

import java.util.function.Consumer;

public interface WatchTableEventsUseCase {

    /**
     * Os avisos da mesa para o celular, com a sessão dele (como os botões). Devolve o que para
     * de receber.
     */
    Runnable watch(String tableToken, String sessionToken, Consumer<LiveEvent> onEvent, Runnable onEnd);
}
