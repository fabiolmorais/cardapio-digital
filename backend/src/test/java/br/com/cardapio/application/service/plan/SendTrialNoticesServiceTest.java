package br.com.cardapio.application.service.plan;

import br.com.cardapio.application.port.out.email.EmailMessage;
import br.com.cardapio.application.port.out.plan.TrialNoticePort;
import br.com.cardapio.application.port.out.plan.TrialRecipient;
import br.com.cardapio.domain.account.model.OrganizationId;
import br.com.cardapio.domain.plan.model.PlanId;
import br.com.cardapio.domain.plan.model.Subscription;
import br.com.cardapio.domain.plan.model.SubscriptionId;
import br.com.cardapio.domain.plan.model.TrialNotice;
import br.com.cardapio.domain.shared.model.Email;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SendTrialNoticesServiceTest {

    // 19/11 às 02h UTC é 18/11 às 23h em São Paulo: o e-mail mostra a data do restaurante.
    private static final Instant END = Instant.parse("2026-11-19T02:00:00Z");

    /** O banco dos testes: as assinaturas em teste e os avisos já marcados. */
    private final List<TrialRecipient> trials = new ArrayList<>();
    private final Set<String> marked = new HashSet<>();
    private final List<EmailMessage> sent = new ArrayList<>();
    private Instant askedEndsBefore;

    private final TrialNoticePort port = new TrialNoticePort() {
        @Override
        public List<TrialRecipient> findTrialsEndingBefore(Instant endsBefore, Instant now) {
            askedEndsBefore = endsBefore;
            return trials;
        }

        @Override
        public boolean markSent(SubscriptionId subscriptionId, TrialNotice notice, Instant at) {
            return marked.add(subscriptionId.value() + ":" + notice);
        }
    };

    private int runAt(Instant now) {
        return new SendTrialNoticesService(port, sent::add, Clock.fixed(now, ZoneOffset.UTC), "https://app.test")
                .sendDue();
    }

    private void givenTrial(String email) {
        Subscription trial = Subscription.startTrial(OrganizationId.newId(), new PlanId(UUID.randomUUID()),
                END.minus(Subscription.TRIAL));
        trials.add(new TrialRecipient(trial, new Email(email), "Sakura Sushi", "America/Sao_Paulo"));
    }

    @Test
    void eachNoticeGoesOutOnceWithTheDatesOfTheRestaurant() {
        givenTrial("dono@sakura.com");

        assertThat(runAt(END.minus(Duration.ofDays(3)))).isEqualTo(1);
        assertThat(askedEndsBefore).isEqualTo(END); // procura só os que acabam nos próximos 3 dias
        assertThat(runAt(END.minus(Duration.ofDays(3)).plus(Duration.ofHours(1)))).isZero(); // a próxima hora não repete
        assertThat(runAt(END.minus(Duration.ofHours(20)))).isEqualTo(1);
        assertThat(runAt(END.plus(Duration.ofHours(1)))).isEqualTo(1);

        assertThat(sent).extracting(EmailMessage::subject).containsExactly("Seu teste grátis acaba em 3 dias",
                "Seu teste grátis acaba amanhã", "Seu teste grátis acabou");
        assertThat(sent).allSatisfy(m -> assertThat(m.to().value()).isEqualTo("dono@sakura.com"));
        // As datas no fuso do restaurante: o fim (02h UTC) é 18/11 em São Paulo, e a tolerância vai até 25/11.
        assertThat(sent.getFirst().text()).contains("O teste grátis do Sakura Sushi vai até 18/11/2026.")
                .contains("https://app.test/admin/assinatura");
        assertThat(sent.getLast().text()).contains("acabou em 18/11/2026").contains("até 25/11/2026");
    }

    @Test
    void anotherInstanceThatAlreadyMarkedTheNoticeWins() {
        givenTrial("dono@sakura.com");
        marked.add(trials.getFirst().subscription().id().value() + ":" + TrialNotice.ENDS_TOMORROW);

        assertThat(runAt(END.minus(Duration.ofHours(20)))).isZero();
        assertThat(sent).isEmpty();
    }
}
