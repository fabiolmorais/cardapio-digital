package br.com.cardapio.application.service.push;

import br.com.cardapio.application.port.in.push.ManagePushUseCase;
import br.com.cardapio.application.port.in.push.PushSettings;
import br.com.cardapio.application.port.out.push.PushSenderPort;
import br.com.cardapio.application.port.out.push.PushSubscriptionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.push.exception.PushDisabledException;
import br.com.cardapio.domain.push.exception.PushSubscriptionNotFoundException;
import br.com.cardapio.domain.push.model.PushSubscription;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Clock;

public class ManagePushService implements ManagePushUseCase {

    private final PushSubscriptionPort subscriptions;
    private final PushSenderPort sender;
    private final Clock clock;

    public ManagePushService(PushSubscriptionPort subscriptions, PushSenderPort sender, Clock clock) {
        this.subscriptions = subscriptions;
        this.sender = sender;
        this.clock = clock;
    }

    @Override
    public PushSettings settings() {
        return sender.enabled() ? new PushSettings(true, sender.publicKey()) : new PushSettings(false, null);
    }

    @Override
    @Transactional
    public void subscribe(EstablishmentId establishmentId, UserId userId, URI endpoint, String p256dh, String auth) {
        if (!sender.enabled()) {
            throw new PushDisabledException();
        }
        PushSubscription subscription = PushSubscription.create(establishmentId, userId, endpoint, p256dh, auth,
                clock.instant());
        if (!sender.accepts(subscription.endpoint())) {
            throw new InvalidValueException(
                    "Este endereço de notificação não é de um serviço de push conhecido.");
        }
        subscriptions.replace(subscription);
    }

    @Override
    @Transactional
    public void unsubscribe(EstablishmentId establishmentId, UserId userId, URI endpoint) {
        if (!subscriptions.delete(establishmentId, userId, endpoint)) {
            throw new PushSubscriptionNotFoundException();
        }
    }
}
