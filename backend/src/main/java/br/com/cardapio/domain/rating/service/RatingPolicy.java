package br.com.cardapio.domain.rating.service;

import br.com.cardapio.domain.tablesession.model.TableSession;

import java.time.Duration;
import java.time.Instant;

/**
 * Quando o celular pode avaliar o atendimento (RF40): com a presença confirmada, sem bloqueio, e
 * depois de a mesa pedir a conta (com a sessão ainda valendo) ou de o garçom fechar a comanda (o
 * fechamento encerra a sessão; vale por mais {@link #AFTER_CLOSING}, o tempo de o cliente avaliar
 * ainda na mesa).
 */
public final class RatingPolicy {

    public static final Duration AFTER_CLOSING = Duration.ofMinutes(30);

    private RatingPolicy() {
    }

    /**
     * @param billRequested a mesa pediu a conta nesta visita
     * @param tabClosed     a comanda desta visita foi fechada
     */
    public static boolean canRate(TableSession session, boolean billRequested, boolean tabClosed, Instant now) {
        if (!session.isPresent() || session.isBlocked()) {
            return false;
        }
        if (session.isActive(now)) {
            return billRequested || tabClosed;
        }
        return tabClosed && session.endedAt() != null && !now.isAfter(session.endedAt().plus(AFTER_CLOSING));
    }
}
