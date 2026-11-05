package br.com.cardapio.application.service.plan;

import br.com.cardapio.application.port.out.email.EmailMessage;
import br.com.cardapio.application.port.out.plan.TrialRecipient;
import br.com.cardapio.domain.plan.model.Subscription;
import br.com.cardapio.domain.plan.model.TrialNotice;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Os textos dos avisos do teste grátis, em pt-BR, com as datas no fuso do restaurante. */
final class TrialNoticeEmails {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private TrialNoticeEmails() {
    }

    static EmailMessage of(TrialNotice notice, TrialRecipient to, String subscriptionUrl) {
        Subscription s = to.subscription();
        String name = to.establishmentName();
        String end = date(s.trialEndsAt(), to.timeZone());
        String grace = date(s.graceEndsAt(), to.timeZone());
        String choose = "Para continuar, escolha o plano na tela Assinatura do painel:\n" + subscriptionUrl;
        return switch (notice) {
            case ENDS_IN_3_DAYS -> new EmailMessage(to.email(), "Seu teste grátis acaba em 3 dias",
                    "Olá! O teste grátis do " + name + " vai até " + end + ".\n\n" + choose
                            + "\n\nDepois do fim do teste, a mesa continua funcionando por mais 7 dias.");
            case ENDS_TOMORROW -> new EmailMessage(to.email(), "Seu teste grátis acaba amanhã",
                    "Olá! O teste grátis do " + name + " acaba amanhã, " + end + ".\n\n" + choose
                            + "\n\nDepois do fim do teste, a mesa continua funcionando por mais 7 dias.");
            case ENDED -> new EmailMessage(to.email(), "Seu teste grátis acabou",
                    "Olá! O teste grátis do " + name + " acabou em " + end + ". A mesa continua chamando e pedindo"
                            + " pelo celular até " + grace + ".\n\n" + choose);
        };
    }

    private static String date(Instant instant, String timeZone) {
        return DATE.format(instant.atZone(ZoneId.of(timeZone)));
    }
}
