package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.SendReportAlertsUseCase;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.push.PushSenderPort;
import br.com.cardapio.application.port.out.push.PushSenderPort.Delivery;
import br.com.cardapio.application.port.out.push.PushSubscriptionPort;
import br.com.cardapio.application.port.out.report.DailySummaryPort;
import br.com.cardapio.application.port.out.report.ReportAlertPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.push.model.PushMessage;
import br.com.cardapio.domain.push.model.PushSubscription;
import br.com.cardapio.domain.report.model.AlertFinding;
import br.com.cardapio.domain.report.model.AlertSettings;
import br.com.cardapio.domain.report.model.ReportLevel;
import br.com.cardapio.domain.report.model.SummaryRecipient;
import br.com.cardapio.domain.report.service.AlertPolicy;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Os alertas em tempo real (REL-78): para cada restaurante do Business, o que passou do limite de cada
 * alerta ligado. Sai no máximo um de cada tipo a cada 15 min, e só pelo que passou do limite depois do
 * último ({@link AlertPolicy}); vai por push para o dono e os gerentes. Marca antes de enviar.
 */
public class SendReportAlertsService implements SendReportAlertsUseCase {

    private final SubscriptionPort subscriptions;
    private final ReportAlertPort alerts;
    private final DailySummaryPort people;
    private final PushSubscriptionPort pushSubscriptions;
    private final PushSenderPort push;
    private final Clock clock;

    public SendReportAlertsService(SubscriptionPort subscriptions, ReportAlertPort alerts, DailySummaryPort people,
                                   PushSubscriptionPort pushSubscriptions, PushSenderPort push, Clock clock) {
        this.subscriptions = subscriptions;
        this.alerts = alerts;
        this.people = people;
        this.pushSubscriptions = pushSubscriptions;
        this.push = push;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstablishmentId> candidates() {
        return push.enabled() ? alerts.candidates() : List.of();
    }

    @Override
    @Transactional
    public int check(EstablishmentId establishmentId) {
        if (ReportAccesses.of(subscriptions, establishmentId).level() != ReportLevel.FULL) {
            return 0; // os alertas são do Business
        }
        AlertSettings settings = alerts.settings(establishmentId).orElse(AlertSettings.DEFAULT);
        Instant now = clock.instant();
        List<PushMessage> messages = new ArrayList<>();
        if (settings.kitchenEnabled()) {
            Duration limit = Duration.ofMinutes(settings.kitchenMinutes());
            due(establishmentId, alerts.lateKitchen(establishmentId, now.minus(limit)), limit, now)
                    .ifPresent(f -> messages.add(ReportAlertMessages.of(f, settings)));
        }
        if (settings.callEnabled()) {
            Duration limit = Duration.ofMinutes(settings.callMinutes());
            due(establishmentId, alerts.unansweredCalls(establishmentId, now.minus(limit)), limit, now)
                    .ifPresent(f -> messages.add(ReportAlertMessages.of(f, settings)));
        }
        if (settings.tabEnabled()) {
            Duration limit = Duration.ofHours(settings.tabHours());
            due(establishmentId, alerts.longTabs(establishmentId, now.minus(limit)), limit, now)
                    .ifPresent(f -> messages.add(ReportAlertMessages.of(f, settings)));
        }
        if (!messages.isEmpty()) {
            send(establishmentId, messages);
        }
        return messages.size();
    }

    /** O atraso, se ele é a vez de avisar e esta instância conseguiu marcar o alerta. */
    private Optional<AlertFinding> due(EstablishmentId id, Optional<AlertFinding> finding, Duration limit,
                                       Instant now) {
        return finding.filter(f -> AlertPolicy.shouldSend(f.newestStart().plus(limit),
                        alerts.lastSent(id, f.type()), now))
                .filter(f -> alerts.claim(id, f.type(), now, now.minus(AlertPolicy.GAP)));
    }

    private void send(EstablishmentId establishmentId, List<PushMessage> messages) {
        List<UserId> users = people.recipients(establishmentId).stream().map(SummaryRecipient::userId).toList();
        if (users.isEmpty()) {
            return;
        }
        for (PushSubscription device : pushSubscriptions.findForUsers(establishmentId, users)) {
            for (PushMessage message : messages) {
                if (push.send(device, message) == Delivery.GONE) {
                    pushSubscriptions.deleteGone(device.endpoint());
                    break;
                }
            }
        }
    }
}
