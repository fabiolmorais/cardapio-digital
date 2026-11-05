package br.com.cardapio.adapter.out.persistence.plan;

import br.com.cardapio.application.port.out.plan.TrialNoticePort;
import br.com.cardapio.application.port.out.plan.TrialRecipient;
import br.com.cardapio.domain.plan.model.Subscription;
import br.com.cardapio.domain.plan.model.SubscriptionId;
import br.com.cardapio.domain.plan.model.TrialNotice;
import br.com.cardapio.domain.shared.model.Email;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
class TrialNoticePersistenceAdapter implements TrialNoticePort {

    private final SubscriptionJpaRepository subscriptions;

    TrialNoticePersistenceAdapter(SubscriptionJpaRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public List<TrialRecipient> findTrialsEndingBefore(Instant endsBefore, Instant now) {
        List<Subscription> trials = subscriptions.findTrialsEndingBetween(now.minus(Subscription.GRACE), endsBefore)
                .stream().map(SubscriptionPersistenceAdapter::toDomain).toList();
        if (trials.isEmpty()) {
            return List.of();
        }
        // Os contatos numa consulta só, para todas as assinaturas (sem N+1).
        Map<UUID, SubscriptionJpaRepository.ContactRow> contacts = subscriptions
                .contacts(trials.stream().map(s -> s.organizationId().value()).toList()).stream()
                .collect(Collectors.toMap(c -> UUID.fromString(c.getOrganizationId()), Function.identity()));
        return trials.stream()
                .filter(s -> contacts.containsKey(s.organizationId().value()))
                .map(s -> {
                    var c = contacts.get(s.organizationId().value());
                    return new TrialRecipient(s, new Email(c.getEmail()), c.getEstablishmentName(), c.getTimeZone());
                })
                .toList();
    }

    @Override
    public boolean markSent(SubscriptionId subscriptionId, TrialNotice notice, Instant at) {
        return subscriptions.markNotice(subscriptionId.value(), notice.name(), at) > 0;
    }
}
