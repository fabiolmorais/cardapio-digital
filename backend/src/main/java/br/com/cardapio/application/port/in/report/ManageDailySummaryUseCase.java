package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;

/** A configuração do resumo diário na gestão (Pro em diante; só o dono e os gerentes). */
public interface ManageDailySummaryUseCase {

    DailySummarySettingsView get(EstablishmentId establishmentId);

    DailySummarySettingsView update(UpdateDailySummaryCommand command);

    /** O resumo do último dia que já terminou, como ele sairia agora. */
    DailySummary preview(EstablishmentId establishmentId);

    /** Manda o resumo do último dia só para quem pediu, por e-mail e no celular, mesmo sem movimento. */
    SummaryTestResult sendTest(EstablishmentId establishmentId, UserId userId);
}
