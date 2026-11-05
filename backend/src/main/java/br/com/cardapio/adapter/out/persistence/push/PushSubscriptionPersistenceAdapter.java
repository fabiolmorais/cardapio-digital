package br.com.cardapio.adapter.out.persistence.push;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.push.PushSubscriptionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.push.model.PushSubscription;
import br.com.cardapio.domain.push.model.PushSubscriptionId;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

@Component
class PushSubscriptionPersistenceAdapter implements PushSubscriptionPort {

    private final PushSubscriptionJpaRepository subscriptions;
    private final EstablishmentFilter establishmentFilter;

    PushSubscriptionPersistenceAdapter(PushSubscriptionJpaRepository subscriptions,
                                       EstablishmentFilter establishmentFilter) {
        this.subscriptions = subscriptions;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public void replace(PushSubscription s) {
        establishmentFilter.enable(s.establishmentId());
        subscriptions.deleteByEndpointEverywhere(s.endpoint().toString());
        subscriptions.save(new PushSubscriptionJpaEntity(s.id().value(), s.establishmentId().value(),
                s.userId().value(), s.endpoint().toString(), s.p256dh(), s.auth(), s.createdAt()));
    }

    @Override
    public boolean delete(EstablishmentId establishmentId, UserId userId, URI endpoint) {
        establishmentFilter.enable(establishmentId);
        return subscriptions.deleteOwn(establishmentId.value(), userId.value(), endpoint.toString()) > 0;
    }

    @Override
    public List<PushSubscription> findForRoles(EstablishmentId establishmentId, List<String> roles) {
        establishmentFilter.enable(establishmentId);
        return subscriptions.findForRoles(establishmentId.value(), roles).stream()
                .map(e -> new PushSubscription(new PushSubscriptionId(e.getId()), establishmentId,
                        new UserId(e.getUserId()), URI.create(e.getEndpoint()), e.getP256dh(), e.getAuth(),
                        e.getCreatedAt()))
                .toList();
    }

    @Override
    public void deleteGone(URI endpoint) {
        subscriptions.deleteByEndpointEverywhere(endpoint.toString());
    }
}
