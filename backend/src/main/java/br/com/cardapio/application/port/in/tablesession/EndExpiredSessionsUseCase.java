package br.com.cardapio.application.port.in.tablesession;

public interface EndExpiredSessionsUseCase {

    /** Encerra as sessões que venceram por falta de uso. Devolve quantas. */
    int endExpired();
}
