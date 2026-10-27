package br.com.cardapio.application.port.in.team;

public interface ReactivateTeamMemberUseCase {

    /** Religa quem foi desligado, com o mesmo papel e PIN. Religar quem já está ativo não é erro. */
    TeamMemberSummary reactivate(ReactivateTeamMemberCommand command);
}
