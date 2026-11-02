package br.com.cardapio.application.port.in.presence;

public interface CheckPresenceUseCase {

    /** Confere a presença do celular (se ainda não estiver confirmada) e devolve como está. */
    PresenceStatus check(CheckPresenceCommand command);
}
