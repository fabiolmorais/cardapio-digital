package br.com.cardapio.application.service.plan;

import br.com.cardapio.application.port.in.plan.SendTrialNoticesUseCase;
import br.com.cardapio.application.port.out.email.EmailSenderPort;
import br.com.cardapio.application.port.out.plan.TrialNoticePort;
import br.com.cardapio.application.port.out.plan.TrialRecipient;
import br.com.cardapio.domain.plan.model.TrialNotice;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Os e-mails do fim do teste: 3 dias antes, 1 dia antes e no fim. Roda de hora em hora (a tarefa
 * agendada). Marca antes de enviar: se o envio falhar, aquele aviso não sai de novo (no máximo
 * uma vez, e nunca duas).
 *
 * <p>Sem {@code @Transactional}: cada marcação é a sua própria transação (a do repositório), e um
 * e-mail lento não segura as outras.
 */
public class SendTrialNoticesService implements SendTrialNoticesUseCase {

    /** O aviso mais cedo é 3 dias antes do fim. */
    private static final Duration EARLIEST = Duration.ofDays(3);

    private final TrialNoticePort notices;
    private final EmailSenderPort email;
    private final Clock clock;
    private final String subscriptionUrl;

    public SendTrialNoticesService(TrialNoticePort notices, EmailSenderPort email, Clock clock, String publicUrl) {
        this.notices = notices;
        this.email = email;
        this.clock = clock;
        this.subscriptionUrl = publicUrl + "/admin/assinatura";
    }

    @Override
    public int sendDue() {
        Instant now = clock.instant();
        int sent = 0;
        for (TrialRecipient recipient : notices.findTrialsEndingBefore(now.plus(EARLIEST), now)) {
            var notice = TrialNotice.due(recipient.subscription(), now);
            if (notice.isPresent() && notices.markSent(recipient.subscription().id(), notice.get(), now)) {
                email.send(TrialNoticeEmails.of(notice.get(), recipient, subscriptionUrl));
                sent++;
            }
        }
        return sent;
    }
}
