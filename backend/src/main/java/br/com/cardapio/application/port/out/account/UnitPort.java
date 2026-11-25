package br.com.cardapio.application.port.out.account;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.Establishment;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Membership;
import br.com.cardapio.domain.account.model.OrganizationId;
import br.com.cardapio.domain.account.model.UnitMembership;
import br.com.cardapio.domain.account.model.UnitSummary;
import br.com.cardapio.domain.account.model.UserId;

import java.util.List;

/**
 * As unidades da empresa (RF31). Tudo parte do restaurante de quem pede: a empresa é a dele, e
 * nunca vem de fora.
 */
public interface UnitPort {

    /** A empresa do restaurante (todo restaurante tem uma). */
    OrganizationId organizationOf(EstablishmentId establishmentId);

    /** Quantas unidades ativas a empresa tem (o plano limita). */
    long countActive(OrganizationId organizationId);

    /** As unidades ativas da empresa, por nome, com as mesas e a equipe ativas de cada uma. */
    ResultPage<UnitSummary> list(OrganizationId organizationId, PageQuery page);

    /** Grava a unidade nova e o vínculo de dono de quem a criou. */
    void saveUnit(Establishment unit, Membership owner);

    /**
     * Todas as unidades ativas da empresa, por nome, com o fuso de cada uma e se a pessoa é dona dela
     * (a comparação entre unidades só existe para quem é dono de todas).
     */
    List<UnitMembership> unitsFor(OrganizationId organizationId, UserId userId);
}
