package br.com.cardapio.application.port.in.team;

public interface UpdateTeamMemberUseCase {

    TeamMemberSummary update(UpdateTeamMemberCommand command);
}
