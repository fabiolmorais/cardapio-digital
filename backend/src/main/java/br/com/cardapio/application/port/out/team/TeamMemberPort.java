package br.com.cardapio.application.port.out.team;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Membership;
import br.com.cardapio.domain.account.model.MembershipId;
import br.com.cardapio.domain.account.model.User;

import java.util.Optional;

/** Equipe de um restaurante. Toda operação recebe o restaurante: nunca se lê a equipe "de todos". */
public interface TeamMemberPort {

    ResultPage<TeamMember> findTeam(EstablishmentId establishmentId, PageQuery page);

    /** Vazio se a pessoa não existe ou é de outro restaurante. */
    Optional<TeamMember> findMember(EstablishmentId establishmentId, MembershipId memberId);

    /** Quem pode entrar por PIN: ativos e que não são o dono, por nome. */
    ResultPage<TeamMember> findPinMembers(EstablishmentId establishmentId, PageQuery page);

    /** Grava a pessoa nova e o vínculo dela com o restaurante. */
    void addMember(User user, Membership membership);

    /** Grava as mudanças de um vínculo que já existe (papel, PIN, ativo). */
    void updateMember(Membership membership);
}
