package br.com.cardapio.application.port.in.account;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.OrganizationId;
import br.com.cardapio.domain.account.model.UserId;

public record SignupResult(
        OrganizationId organizationId,
        EstablishmentId establishmentId,
        UserId userId,
        String slug
) {

}
