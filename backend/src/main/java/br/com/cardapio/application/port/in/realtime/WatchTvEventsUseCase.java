package br.com.cardapio.application.port.in.realtime;

import java.util.function.Consumer;

public interface WatchTvEventsUseCase {

    /** Os avisos do restaurante para a TV vinculada (pelo cookie). Devolve o que para de receber. */
    Runnable watch(String deviceToken, Consumer<LiveEvent> onEvent, Runnable onEnd);
}
