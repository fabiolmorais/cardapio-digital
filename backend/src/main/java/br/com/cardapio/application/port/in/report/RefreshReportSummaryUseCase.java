package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.SummaryStatus;

import java.util.List;

/**
 * A tarefa do resumo diário (ADR 0014): depois que o dia de um restaurante vira, resume o dia que
 * terminou e refaz os anteriores mais recentes; sem resumo, faz o histórico inteiro.
 */
public interface RefreshReportSummaryUseCase {

    /** Os restaurantes com dia novo para resumir (o dia virou desde a última volta, ou nunca resumiu). */
    List<SummaryStatus> due();

    /** Resume o restaurante até ontem, no fuso dele, numa transação só. */
    void refresh(SummaryStatus status);
}
