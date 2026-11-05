package br.com.cardapio.application.port.in.plan;

public interface SendTrialNoticesUseCase {

    /** Envia os avisos do fim do teste que estão na vez. Devolve quantos saíram. */
    int sendDue();
}
