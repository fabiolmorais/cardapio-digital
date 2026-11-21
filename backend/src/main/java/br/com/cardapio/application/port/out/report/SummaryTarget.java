package br.com.cardapio.application.port.out.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.report.model.DailySummarySettings;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Um restaurante com o resumo ligado (por e-mail ou no celular), para a tarefa agendada.
 *
 * @param lastSentDay o último dia já enviado
 */
public record SummaryTarget(EstablishmentId establishmentId, String name, ZoneId zone,
                            DailySummarySettings settings, Optional<LocalDate> lastSentDay) {
}
