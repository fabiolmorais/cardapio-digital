package br.com.cardapio.domain.report.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Quando um alerta sai (REL-78): no máximo um de cada tipo a cada {@link #GAP}, e só por algo que passou
 * do limite depois do último alerta daquele tipo. Assim, uma comanda esquecida avisa uma vez, e não a
 * noite inteira; sem alerta anterior, só conta o que passou do limite nos últimos {@link #GAP}.
 */
public final class AlertPolicy {

    public static final Duration GAP = Duration.ofMinutes(15);

    private AlertPolicy() {
    }

    /**
     * @param newestCrossing quando o mais novo do que está atrasado passou do limite
     * @param lastSent       o último alerta desse tipo; vazio = nunca
     */
    public static boolean shouldSend(Instant newestCrossing, Optional<Instant> lastSent, Instant now) {
        if (lastSent.isPresent() && lastSent.get().isAfter(now.minus(GAP))) {
            return false;
        }
        Instant since = lastSent.orElse(now.minus(GAP));
        return newestCrossing.isAfter(since);
    }
}
