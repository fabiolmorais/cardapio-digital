package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.DailySummary;
import br.com.cardapio.application.port.in.report.DailySummarySettingsView;
import br.com.cardapio.application.port.in.report.ManageDailySummaryUseCase;
import br.com.cardapio.application.port.in.report.SummaryTestResult;
import br.com.cardapio.application.port.in.report.UpdateDailySummaryCommand;
import br.com.cardapio.application.port.out.email.EmailSenderPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.push.PushSenderPort;
import br.com.cardapio.application.port.out.push.PushSenderPort.Delivery;
import br.com.cardapio.application.port.out.push.PushSubscriptionPort;
import br.com.cardapio.application.port.out.report.DailySummaryPort;
import br.com.cardapio.application.port.out.report.KitchenReportPort;
import br.com.cardapio.application.port.out.report.SalesReportPort;
import br.com.cardapio.application.port.out.report.SummaryTarget;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.push.model.PushMessage;
import br.com.cardapio.domain.push.model.PushSubscription;
import br.com.cardapio.domain.report.exception.SummaryRecipientNotFoundException;
import br.com.cardapio.domain.report.model.SummaryRecipient;
import br.com.cardapio.domain.report.model.SummaryWindow;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** A configuração do resumo diário (desenho 14.15): Pro em diante, só o dono e os gerentes. */
public class ManageDailySummaryService implements ManageDailySummaryUseCase {

    private final SubscriptionPort subscriptions;
    private final DailySummaryPort summaries;
    private final DailySummaryBuilder builder;
    private final EmailSenderPort email;
    private final PushSubscriptionPort pushSubscriptions;
    private final PushSenderPort push;
    private final Clock clock;
    private final String publicUrl;

    public ManageDailySummaryService(SubscriptionPort subscriptions, DailySummaryPort summaries, SalesReportPort sales,
                                     KitchenReportPort kitchen, EmailSenderPort email,
                                     PushSubscriptionPort pushSubscriptions, PushSenderPort push, Clock clock,
                                     String publicUrl) {
        this.subscriptions = subscriptions;
        this.summaries = summaries;
        this.builder = new DailySummaryBuilder(sales, kitchen);
        this.email = email;
        this.pushSubscriptions = pushSubscriptions;
        this.push = push;
        this.clock = clock;
        this.publicUrl = publicUrl;
    }

    private SummaryTarget target(EstablishmentId id) {
        ReportAccesses.of(subscriptions, id).requireReports();
        // O restaurante vem do usuário autenticado: sempre existe.
        return summaries.target(id).orElseThrow(() -> new IllegalStateException("restaurante sem cadastro"));
    }

    @Override
    @Transactional(readOnly = true)
    public DailySummarySettingsView get(EstablishmentId establishmentId) {
        SummaryTarget target = target(establishmentId);
        return new DailySummarySettingsView(target.settings(), summaries.recipients(establishmentId));
    }

    @Override
    @Transactional
    public DailySummarySettingsView update(UpdateDailySummaryCommand command) {
        EstablishmentId id = command.establishmentId();
        target(id);
        // Só donos e gerentes deste restaurante: de fora (ou da cozinha, do salão), 404.
        Set<UserId> eligible = summaries.recipients(id).stream().map(SummaryRecipient::userId)
                .collect(Collectors.toSet());
        if (!eligible.containsAll(command.receivers())) {
            throw new SummaryRecipientNotFoundException();
        }
        summaries.saveSettings(id, command.settings());
        summaries.setReceivers(id, command.receivers());
        return new DailySummarySettingsView(command.settings(), summaries.recipients(id));
    }

    @Override
    @Transactional(readOnly = true)
    public DailySummary preview(EstablishmentId establishmentId) {
        SummaryTarget target = target(establishmentId);
        SummaryWindow window = SummaryWindow.lastEnded(clock.instant(), target.settings().sendHour(), target.zone());
        return builder.build(establishmentId, target.name(), window);
    }

    @Override
    @Transactional
    public SummaryTestResult sendTest(EstablishmentId establishmentId, UserId userId) {
        DailySummary summary = preview(establishmentId);
        SummaryRecipient me = summaries.recipients(establishmentId).stream()
                .filter(r -> r.userId().equals(userId))
                .findFirst()
                .orElseThrow(SummaryRecipientNotFoundException::new);
        boolean emailed = me.email().isPresent();
        me.email().ifPresent(to -> email.send(DailySummaryMessages.email(summary, to, me.role(), publicUrl)));
        int devices = push.enabled()
                ? deliver(pushSubscriptions.findForUsers(establishmentId, List.of(userId)),
                DailySummaryMessages.push(summary))
                : 0;
        return new SummaryTestResult(emailed, devices);
    }

    private int deliver(List<PushSubscription> devices, PushMessage message) {
        int sent = 0;
        for (PushSubscription device : devices) {
            Delivery delivery = push.send(device, message);
            if (delivery == Delivery.SENT) {
                sent++;
            } else if (delivery == Delivery.GONE) {
                pushSubscriptions.deleteGone(device.endpoint());
            }
        }
        return sent;
    }
}
