package br.com.cardapio.application.port.in.menu;

/** Apaga os eventos do cardápio mais velhos que o prazo de retenção (LGPD: guardar só o necessário). */
public interface PurgeMenuEventsUseCase {

    int purge();
}
