package br.com.cardapio.application.port.in.rodizio;

/** O garçom confirma as pessoas e ativa o rodízio na mesa. */
public interface ActivateRodizioUseCase {

    TableRodizio activate(ActivateRodizioCommand command);
}
