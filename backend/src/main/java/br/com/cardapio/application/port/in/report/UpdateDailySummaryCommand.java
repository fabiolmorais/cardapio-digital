package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.report.model.DailySummarySettings;

import java.util.Objects;
import java.util.Set;

/** A nova configuração do resumo e quem recebe (donos e gerentes do restaurante). */
public record UpdateDailySummaryCommand(EstablishmentId establishmentId, DailySummarySettings settings,
                                        Set<UserId> receivers) {

    public UpdateDailySummaryCommand {
        Objects.requireNonNull(establishmentId, "restaurante é obrigatório");
        Objects.requireNonNull(settings, "configuração é obrigatória");
        receivers = Set.copyOf(receivers);
    }
}
