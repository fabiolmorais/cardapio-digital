package br.com.cardapio.adapter.in.scheduler;

import br.com.cardapio.application.port.in.plan.SendTrialNoticesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** De hora em hora, os avisos do fim do teste grátis que estão na vez. */
@Component
class TrialNoticeJob {

    private static final Logger log = LoggerFactory.getLogger(TrialNoticeJob.class);

    private final SendTrialNoticesUseCase notices;

    TrialNoticeJob(SendTrialNoticesUseCase notices) {
        this.notices = notices;
    }

    @Scheduled(fixedDelayString = "${app.subscription.notice-interval:PT1H}",
            initialDelayString = "${app.subscription.notice-interval:PT1H}")
    void run() {
        int sent = notices.sendDue();
        if (sent > 0) {
            log.info("Avisos do fim do teste grátis enviados: {}", sent);
        }
    }
}
