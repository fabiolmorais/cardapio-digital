package br.com.cardapio.application.port.in.team;

public interface AddTeamMemberUseCase {
    TeamMemberSummary add(AddTeamMemberCommand command);
}
