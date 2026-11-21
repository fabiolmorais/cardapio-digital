package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.SummaryRecipient;
import br.com.cardapio.domain.report.model.DailySummarySettings;

import java.util.List;

/** A tela de configuração do resumo: como ele está e quem pode receber. */
public record DailySummarySettingsView(DailySummarySettings settings, List<SummaryRecipient> recipients) {
}
