package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.account.model.EstablishmentId;

import java.util.List;

/** Os alertas em tempo real (REL-78): a tarefa agendada confere cada restaurante de minuto em minuto. */
public interface SendReportAlertsUseCase {

    /** Os restaurantes que podem ter alerta (ativos, com algum ligado). */
    List<EstablishmentId> candidates();

    /** Confere os atrasos do restaurante e manda os alertas da vez; devolve quantos saíram. */
    int check(EstablishmentId establishmentId);
}
