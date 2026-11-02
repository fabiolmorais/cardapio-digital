package br.com.cardapio.domain.call.model;

/** O ciclo do chamado: pendente → em atendimento → atendido, ou cancelado. */
public enum CallStatus {
    PENDING,
    IN_PROGRESS,
    DONE,
    CANCELED;

    /** Ainda precisa de alguém: aparece na fila da equipe e no botão da mesa. */
    public boolean isOpen() {
        return this == PENDING || this == IN_PROGRESS;
    }
}
