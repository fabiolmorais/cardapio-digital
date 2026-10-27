package br.com.cardapio.application.port.in.team;

public interface DeactivateTeamMemberUseCase {

    /** Desliga a pessoa do restaurante. Desligar quem já está desligado não é erro. */
    void deactivate(DeactivateTeamMemberCommand command);
}
