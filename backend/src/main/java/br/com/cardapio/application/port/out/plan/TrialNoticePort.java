package br.com.cardapio.application.port.out.plan;

import br.com.cardapio.domain.plan.model.SubscriptionId;
import br.com.cardapio.domain.plan.model.TrialNotice;

import java.time.Instant;
import java.util.List;

/**
 * Os avisos do teste grátis. É uma tarefa do sistema, que olha todos os restaurantes: por isso
 * não recebe establishmentId, e só devolve o necessário para o e-mail.
 */
public interface TrialNoticePort {

    /** As assinaturas em teste cujo fim cai antes de {@code endsBefore} e cuja tolerância ainda não passou de {@code now}. */
    List<TrialRecipient> findTrialsEndingBefore(Instant endsBefore, Instant now);

    /**
     * Marca o aviso como enviado. Devolve false se ele já estava marcado: com duas instâncias
     * rodando a tarefa ao mesmo tempo, só uma ganha, e o e-mail sai uma vez.
     */
    boolean markSent(SubscriptionId subscriptionId, TrialNotice notice, Instant at);
}
