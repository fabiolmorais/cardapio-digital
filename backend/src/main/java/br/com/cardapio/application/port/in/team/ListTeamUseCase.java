package br.com.cardapio.application.port.in.team;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.MembershipId;

public interface ListTeamUseCase {

    /** Equipe do restaurante: ativos primeiro, depois por nome. */
    ResultPage<TeamMemberSummary> list(EstablishmentId establishmentId, PageQuery page);

    /** Uma pessoa da equipe; TeamMemberNotFoundException se ela não for deste restaurante. */
    TeamMemberSummary get(EstablishmentId establishmentId, MembershipId memberId);
}
