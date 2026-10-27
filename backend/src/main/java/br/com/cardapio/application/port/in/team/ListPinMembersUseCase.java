package br.com.cardapio.application.port.in.team;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;

public interface ListPinMembersUseCase {

    /** Quem pode entrar por PIN no aparelho do token (a lista de nomes da tela do PIN). */
    ResultPage<TeamMemberSummary> list(String deviceToken, PageQuery page);
}
