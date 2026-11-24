package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.DailySummary;
import br.com.cardapio.application.port.out.report.RevenueGoalPort;
import br.com.cardapio.application.port.in.report.SendDailySummariesUseCase;
import br.com.cardapio.application.port.out.email.EmailSenderPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.push.PushSenderPort;
import br.com.cardapio.application.port.out.push.PushSenderPort.Delivery;
import br.com.cardapio.application.port.out.push.PushSubscriptionPort;
import br.com.cardapio.application.port.out.report.DailySummaryPort;
import br.com.cardapio.application.port.out.report.KitchenReportPort;
import br.com.cardapio.application.port.out.report.SalesReportPort;
import br.com.cardapio.application.port.out.report.SummaryTarget;
import br.com.cardapio.domain.push.model.PushMessage;
import br.com.cardapio.domain.push.model.PushSubscription;
import br.com.cardapio.domain.report.model.ReportLevel;
import br.com.cardapio.domain.report.model.SummaryRecipient;
import br.com.cardapio.domain.report.model.SummaryWindow;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Os resumos diários que estão na vez (REL-76). A tarefa agendada roda de 10 em 10 minutos: para
 * cada restaurante com o resumo ligado e relatórios no plano, o último dia que já terminou no
 * horário dele, se ainda não foi enviado. Marca antes de enviar: no máximo uma vez, e nunca duas.
 * Dia sem comanda fechada é marcado e não gera resumo.
 */
public class SendDailySummariesService implements SendDailySummariesUseCase {

    private final SubscriptionPort subscriptions;
    private final DailySummaryPort summaries;
    private final DailySummaryBuilder builder;
    private final EmailSenderPort email;
    private final PushSubscriptionPort pushSubscriptions;
    private final PushSenderPort push;
    private final Clock clock;
    private final String publicUrl;

    public SendDailySummariesService(SubscriptionPort subscriptions, DailySummaryPort summaries,
                                     SalesReportPort sales, KitchenReportPort kitchen, RevenueGoalPort goals,
                                     EmailSenderPort email,
                                     PushSubscriptionPort pushSubscriptions, PushSenderPort push, Clock clock,
                                     String publicUrl) {
        this.subscriptions = subscriptions;
        this.summaries = summaries;
        this.builder = new DailySummaryBuilder(sales, kitchen, goals);
        this.email = email;
        this.pushSubscriptions = pushSubscriptions;
        this.push = push;
        this.clock = clock;
        this.publicUrl = publicUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstablishmentId> due() {
        Instant now = clock.instant();
        return summaries.targets().stream()
                .filter(target -> isDue(target, now))
                .map(SummaryTarget::establishmentId)
                .toList();
    }

    @Override
    @Transactional
    public boolean send(EstablishmentId establishmentId) {
        Optional<SummaryTarget> found = summaries.target(establishmentId);
        if (found.isEmpty() || !isDue(found.get(), clock.instant())) {
            return false; // desligou ou outra instância já mandou
        }
        SummaryTarget target = found.get();
        ReportLevel level = ReportAccesses.of(subscriptions, establishmentId).level();
        if (level == ReportLevel.NONE) {
            return false; // o Essencial não tem relatórios
        }
        SummaryWindow window = window(target, clock.instant());
        if (!summaries.claim(establishmentId, window.day())) {
            return false; // outra instância pegou
        }
        DailySummary summary = builder.build(establishmentId, target.name(), window,
                level.covers(ReportLevel.FULL));
        if (!summary.hasActivity()) {
            return false;
        }
        send(target, summary);
        return true;
    }

    private static SummaryWindow window(SummaryTarget target, Instant now) {
        return SummaryWindow.lastEnded(now, target.settings().sendHour(), target.zone());
    }

    private static boolean isDue(SummaryTarget target, Instant now) {
        LocalDate day = window(target, now).day();
        return target.settings().active() && target.lastSentDay().filter(last -> !last.isBefore(day)).isEmpty();
    }

    private void send(SummaryTarget target, DailySummary summary) {
        List<SummaryRecipient> receivers = summaries.recipients(target.establishmentId()).stream()
                .filter(SummaryRecipient::receives)
                .toList();
        if (target.settings().emailEnabled()) {
            for (SummaryRecipient r : receivers) {
                r.email().ifPresent(to -> email.send(DailySummaryMessages.email(summary, to, r.role(), publicUrl)));
            }
        }
        if (target.settings().pushEnabled() && push.enabled() && !receivers.isEmpty()) {
            PushMessage message = DailySummaryMessages.push(summary);
            List<UserId> users = receivers.stream().map(SummaryRecipient::userId).toList();
            for (PushSubscription device : pushSubscriptions.findForUsers(target.establishmentId(), users)) {
                if (push.send(device, message) == Delivery.GONE) {
                    pushSubscriptions.deleteGone(device.endpoint());
                }
            }
        }
    }
}
