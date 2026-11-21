package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.account.model.EstablishmentId;

import java.util.List;

/**
 * Os resumos diários que estão na vez (a tarefa agendada): primeiro quem está na vez, depois um
 * restaurante por vez, cada um na sua transação (um e-mail lento ou um erro não segura os outros).
 */
public interface SendDailySummariesUseCase {

    /** Os restaurantes com o resumo ligado cujo último dia já terminou e ainda não foi enviado. */
    List<EstablishmentId> due();

    /**
     * Envia o resumo do último dia do restaurante, se ainda está na vez e o plano tem relatórios.
     * Marca antes de enviar (no máximo uma vez, e nunca duas). true: o resumo saiu (dia sem comanda
     * fechada é marcado e não sai).
     */
    boolean send(EstablishmentId establishmentId);
}
