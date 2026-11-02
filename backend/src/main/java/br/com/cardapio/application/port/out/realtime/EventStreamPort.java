package br.com.cardapio.application.port.out.realtime;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.event.DomainEvent;

import java.util.function.Consumer;

/** Os eventos publicados, de qualquer instância do back, para quem está com uma tela aberta. */
public interface EventStreamPort {

    /**
     * Passa a receber os eventos do restaurante, e só dele.
     *
     * @param onEvent chamado a cada evento (numa thread do adaptador, não na de quem assinou)
     * @param onEnd   chamado se a assinatura acabar sozinha (o back perdeu a ligação com o banco
     *                ou está desligando): a tela reconecta e busca tudo de novo
     * @return o que cancela a assinatura
     */
    Runnable subscribe(EstablishmentId establishmentId, Consumer<DomainEvent> onEvent, Runnable onEnd);
}
