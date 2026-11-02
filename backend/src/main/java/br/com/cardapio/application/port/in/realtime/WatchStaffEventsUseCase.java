package br.com.cardapio.application.port.in.realtime;

import br.com.cardapio.domain.account.model.EstablishmentId;

import java.util.function.Consumer;

public interface WatchStaffEventsUseCase {

    /** Os avisos do restaurante para as telas da equipe. Devolve o que para de receber. */
    Runnable watch(EstablishmentId establishmentId, Consumer<LiveEvent> onEvent, Runnable onEnd);
}
