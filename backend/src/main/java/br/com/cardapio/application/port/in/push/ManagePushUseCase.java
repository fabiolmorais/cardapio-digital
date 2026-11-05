package br.com.cardapio.application.port.in.push;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;

import java.net.URI;

/** As notificações no celular da equipe: ligar e desligar, por navegador. */
public interface ManagePushUseCase {

    PushSettings settings();

    void subscribe(EstablishmentId establishmentId, UserId userId, URI endpoint, String p256dh, String auth);

    void unsubscribe(EstablishmentId establishmentId, UserId userId, URI endpoint);
}
